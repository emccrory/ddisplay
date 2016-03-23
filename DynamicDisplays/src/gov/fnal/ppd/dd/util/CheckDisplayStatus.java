/*
 * CheckDisplayStatus
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.PING_INTERVAL;
import static gov.fnal.ppd.dd.GlobalVariables.SELF_IDENTIFY;
import static gov.fnal.ppd.dd.GlobalVariables.getContentOnDisplays;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import gov.fnal.ppd.dd.changer.ChannelButtonGrid;
import gov.fnal.ppd.dd.changer.DisplayButtons;
import gov.fnal.ppd.dd.display.DisplayFacade;
import gov.fnal.ppd.dd.display.DisplayListDatabaseRemote;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.swing.JLabel;

/**
 * Utility class for the main Channel Selector class.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class CheckDisplayStatus extends Thread {

	private Display			display;
	private int				index;
	private JLabel			footer;
	private static boolean	doDisplayButtons	= true;
	private final long		initialSleep;

	// private List<List<ChannelButtonGrid>> grids;

	/**
	 * @param display
	 * @param index
	 * @param footer
	 * @param grids
	 */
	public CheckDisplayStatus(final Display display, final int index, final JLabel footer, final List<List<ChannelButtonGrid>> grids) {
		super("Display." + display.getDBDisplayNumber() + "." + display.getScreenNumber() + ".StatusUpdateGrabber");
		this.display = display;
		this.index = index;
		this.footer = footer;
		// this.grids = grids;
		initialSleep = 5 * index + 20000L;
	}

	/**
	 * @param display
	 * @param footer
	 */
	public CheckDisplayStatus(final Display display, final JLabel footer) {
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
																	// each other a little
		// bit.
		Connection connection = DisplayListDatabaseRemote.getConnection();
		// read from the database to see what's up with this Display
		String query = "SELECT Content,ContentName,SignageContent FROM DisplayStatus WHERE DisplayID="
				+ display.getDBDisplayNumber();
		final String startText = "Disp " + display.getVirtualDisplayNumber() + ": ";
		final String isBeingDisplayed = " is being displayed";

		while (true) {
			try {
				sleep(PING_INTERVAL);

				// Use ARM (Automatic Resource Management) to assure that things get closed properly (a new Java 7
				// feature)
				try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query);) {
					if (rs.first()) { // Move to first returned row
						while (!rs.isAfterLast()) {
							Blob sc = rs.getBlob("SignageContent");
							String contentName = rs.getString("Content");
							SignageContent currentContent = null;

							if (sc != null && sc.length() > 0)
								try {
									int len = (int) sc.length();
									byte[] bytes = sc.getBytes(1, len);

									ByteArrayInputStream fin = new ByteArrayInputStream(bytes);
									ObjectInputStream ois = new ObjectInputStream(fin);
									currentContent = (SignageContent) ois.readObject();
									getContentOnDisplays().put(display, currentContent);
								} catch (Exception e) {
									e.printStackTrace();
								}

							if (display instanceof DisplayFacade && currentContent != null) {
								if (!contentName.equals(SELF_IDENTIFY)) {
									// It should fall here; all displays on this side are DisplayFacade's
									DisplayFacade myDisplay = (DisplayFacade) display;
									myDisplay.setContentValueBackdoor(currentContent);
									// System.out.println("Status of Display " + display.getDBDisplayNumber() + " is being set to "
									// + currentContent + "\t(" + currentContent.getClass().getSimpleName() + ")");
								}
							}

							if (contentName != null)
								try {
									if (contentName.startsWith("0: "))
										contentName = contentName.substring(3);
									contentName = contentName.replace(isBeingDisplayed, "");
									// Create a new footer
									if (getContentOnDisplays().get(display) != null)
										footer.setText(startText + getContentOnDisplays().get(display));
									else
										footer.setText(startText + contentName);

									if (doDisplayButtons)
										DisplayButtons.setToolTip(display);

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
									// FIXME -- The code, above, works but it is really a lot of work to enable a button. I don't
									// even think it works anymore.
								} catch (Exception e) {
									e.printStackTrace();
								}
							rs.next();
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
