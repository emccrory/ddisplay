/*
 * CheckDisplayStatus
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.specific;

import static gov.fnal.ppd.dd.GlobalVariables.PING_INTERVAL;
import static gov.fnal.ppd.dd.GlobalVariables.SELF_IDENTIFY;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_VIRTUAL_DISPLAY_NUMS;
import static gov.fnal.ppd.dd.GlobalVariables.getContentOnDisplays;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;

import java.io.StreamCorruptedException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import gov.fnal.ppd.dd.changer.DisplayButtons;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelInListImpl;
import gov.fnal.ppd.dd.channel.ChannelListHolder;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.channel.ConcreteChannelListHolder;
import gov.fnal.ppd.dd.chat.ErrorProcessingMessage;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.display.DisplayFacade;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.xml.ChangeChannelList;
import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;

/**
 * Utility class for the main Channel Selector class.
 * 
 * TODO: This class mixes database access (to see what the status of a display is) and GUI (to show this status in a meaningful way
 * on the GUI). So this should be refactored into the listening part and the GUI part.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class CheckDisplayStatus extends Thread {

	private Display			display;
	private Object			footer;
	private static boolean	doDisplayButtons	= true;
	private final long		initialSleep;
	private String			lastStreamCorruptedMesage;

	// private List<List<ChannelButtonGrid>> grids;

	/**
	 * @param display
	 * @param index
	 *            - A value related to the initial sleep for this Thread
	 * @param footer
	 */
	public CheckDisplayStatus(final Display display, final int index, final Object footer) {
		super("Display." + display.getDBDisplayNumber() + "." + display.getScreenNumber() + ".StatusUpdateGrabber");
		this.display = display;
		this.footer = footer;
		initialSleep = 5 * index + 20000L;
	}

	/**
	 * @param display
	 * @param footer
	 */
	public CheckDisplayStatus(final Display display, final Object footer) {
		super("Display." + display.getDBDisplayNumber() + "." + display.getScreenNumber() + ".StatusUpdateGrabber");
		this.display = display;
		this.footer = footer;
		initialSleep = 10000L + (long) (Math.random() * 3000L);
	}

	/**
	 * @param doDisplayButtons
	 *            the doDisplayButtons to set
	 */
	public static void setDoDisplayButtons(boolean doDisplayButtons) {
		CheckDisplayStatus.doDisplayButtons = doDisplayButtons;
	}

	public void run() {
		catchSleep(Math.max(1000L, initialSleep - PING_INTERVAL)); // Put in a delay to get the Pings to each Display offset from
																	// each other a little bit.
		try {
			Connection connection = ConnectionToDatabase.getDbConnection();
			// read from the database to see what's up with this Display
			String query = "SELECT Content,ContentName,SignageContent FROM DisplayStatus WHERE DisplayID="
					+ display.getDBDisplayNumber();
			final String startText = "Disp "
					+ (SHOW_VIRTUAL_DISPLAY_NUMS ? display.getVirtualDisplayNumber() : display.getDBDisplayNumber()) + ": ";
			final String isBeingDisplayed = " is being displayed";

			int counter = 0;
			int failures = 0;
			while (true) {
				try {
					sleep(PING_INTERVAL);
					counter++;
					// Use ARM (Automatic Resource Management) to assure that things get closed properly (a new Java 7
					// feature)
					String rawMessage = null;
					try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query);) {
						if (rs.first()) { // Move to first returned row
							while (!rs.isAfterLast()) {
								try {
									String contentName = rs.getString("Content");
									Blob sc = rs.getBlob("SignageContent");
									rawMessage = new String(sc.getBytes(1, (int) sc.length()));
									SignageContent currentContent = null;
									if (rawMessage.contains("changeChannelList")) {
										ChangeChannelList ccl = (ChangeChannelList) MyXMLMarshaller
												.unmarshall(ChangeChannelList.class, rawMessage);
										List<ChannelInList> lsc = new ArrayList<ChannelInList>();
										for (ChannelSpec C : ccl.getChannelSpec()) {
											lsc.add(new ChannelInListImpl(C));
										}
										ChannelListHolder holder = new ConcreteChannelListHolder(contentName, lsc);
										currentContent = new ChannelPlayList(holder, 1000000L);
										//currentContent = new ChannelPlayList(contentName, lsc, 1000000L);
									} else if (rawMessage.contains("channelSpec")) {
										currentContent = (ChannelSpec) MyXMLMarshaller.unmarshall(ChannelSpec.class, rawMessage);
									} else {
										throw new ErrorProcessingMessage(
												"Unknown XML data type within this XML document: [Unrecognized XML message received] -- "
														+ rawMessage);
									}
									if (display instanceof DisplayFacade && currentContent != null) {
										if (!contentName.equals(SELF_IDENTIFY)) {
											// It should fall here; all displays on this side are DisplayFacade's
											DisplayFacade myDisplay = (DisplayFacade) display;
											myDisplay.setContentValueBackdoor(currentContent);
											// println(getClass(), "Status of Display " + display.getDBDisplayNumber() + " is being
											// set to " + currentContent + "\t(" + currentContent.getClass().getSimpleName()
											// + ")");
											Display o = getContentOnDisplays().get(myDisplay.getDBDisplayNumber());
											if (o == null)
												getContentOnDisplays().put(myDisplay, currentContent);
											else
												getContentOnDisplays().replace(o, currentContent);
										}
									}

									if (contentName != null)
										try {
											if (contentName.startsWith("0: "))
												contentName = contentName.substring(3);
											contentName = contentName.replace(isBeingDisplayed, "");
											// Create a new footer
											if (getContentOnDisplays().get(display) != null)
												setText(startText + getContentOnDisplays().get(display));
											else
												setText(startText + contentName);

											if (doDisplayButtons)
												setToolTip(display);

											// Enable the Channel buttons, too
											// synchronized (grids) {
											// if (grids == null || grids.size() == 0)
											// continue;
											// for (List<ChannelButtonGrid> allGrids : grids)
											// if (allGrids == null || allGrids.size() == 0)
											// continue;
											// else if (allGrids.get(0).getDisplay().getDBDisplayNumber() == display
											// .getDBDisplayNumber())
											// for (ChannelButtonGrid cbg : allGrids)
											// for (int i = 0; i < cbg.getBg().getNumButtons(); i++) {
											// DDButton myButton = cbg.getBg().getAButton(i);
											// boolean selected = myButton.getChannel().toString().equals(contentName);
											// myButton.setSelected(selected);
											// }
											// }
											// FIXME -- The code, above, works but it is really a lot of work to enable a button. I
											// don't even think it works anymore.
										} catch (Exception e) {
											e.printStackTrace();
										}
								} catch (Exception e) {
									// Special case for the interim period when some displays have the old code;
									if (e instanceof javax.xml.bind.UnmarshalException) {
										printlnErr(getClass(), "Display number " + display.getDBDisplayNumber()
												+ " not ready for this version of the code (" + e.getMessage() + ")");
									} else if (e instanceof ClassNotFoundException
											&& e.getLocalizedMessage().equals("gov.fnal.ppd.dd.changer.ChannelCategory")) {
										printlnErr(getClass(), "Ignoring " + e.getMessage());
										; // Ignore this error completely while we transition to XML documents in the status
											// database.
									} else if (e instanceof StreamCorruptedException) {
										if (!e.getLocalizedMessage().equals(lastStreamCorruptedMesage)) {
											println(getClass(),
													counter + " Trying to read streamed content object in database from display "
															+ display.getDBDisplayNumber() + ", but it was corrputed: ["
															+ e.getClass().getSimpleName() + " - " + e.getLocalizedMessage()
															+ "]\n\tWill supress this specific error message for now.\n"
															+ rawMessage);
											lastStreamCorruptedMesage = e.getLocalizedMessage();
										}
										e.printStackTrace();
									} else {
										println(getClass(),
												counter + " Trying to read streamed content object in database from display "
														+ display.getDBDisplayNumber() + ", but it failed: ["
														+ e.getClass().getSimpleName() + " - " + e.getLocalizedMessage() + "]"
														+ rawMessage);
										e.printStackTrace();
									}
								}
								if (!rs.next())
									break;
							}
						} else {
							if (failures++ < 10 || (failures % 50) == 0)
								printlnErr(getClass(),
										"Failure " + failures + " - No status for display " + display.getVirtualDisplayNumber()
												+ "|" + display.getDBDisplayNumber() + ".  Query: " + query);
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setToolTip(Display disp) {
		// This needs to be refactored so there is no direct dependence on this GUI class.
		DisplayButtons.setToolTip(disp);
	}

	private void setText(String text) {
		// Refactored so this class does not need to depend on JLabel (which was the type of the attribute "footer" at one time).
		try {
			Method method = footer.getClass().getMethod("setText", String.class);
			method.invoke(footer, text);
		} catch (NoSuchMethodException e) {
			// This one is probably OK
		} catch (Exception e) {
			// Everything else, tell me about it.
			e.printStackTrace();
		}

	}
}
