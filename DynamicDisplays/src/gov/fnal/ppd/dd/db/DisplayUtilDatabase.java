package gov.fnal.ppd.dd.db;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getContentOnDisplays;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;
import static gov.fnal.ppd.dd.util.specific.PackageUtilities.convertContentToDBReadyString;
import static gov.fnal.ppd.dd.util.specific.PackageUtilities.getChannelFromNumber;

import java.awt.Color;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.fnal.ppd.dd.changer.ListOfExistingContent;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelInListImpl;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.channel.ConcreteChannelListHolder;
import gov.fnal.ppd.dd.chat.ErrorProcessingMessage;
import gov.fnal.ppd.dd.display.DisplayFacade;
import gov.fnal.ppd.dd.interfaces.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.nonguiUtils.CredentialsNotFoundException;
import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;
import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;
import gov.fnal.ppd.dd.xml.messages.ChangeChannelList;

/**
 * This class does a couple of tasks for displays with respect to the database.
 * <ol>
 * <li>Gets the channels that each display is supposed to show for a particular save set.</li>
 * <li>Gets the FLAVOR (gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR) that this display is supposed to be looking for
 * during an update. This is obtained from the database.</li>
 * </ol>
 * 
 * Note that if a Display is removed and it was saved in a save set, this will throw a silent error.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2018
 * 
 */
public class DisplayUtilDatabase {
	private DisplayUtilDatabase() {
		// Do not instantiate
	}

	/**
	 * Get the save set associated with setName
	 * 
	 * @param setName
	 *            - the name of the save set in the database
	 * @return the content at each display for this save set.
	 */
	public static Map<Display, SignageContent> getDisplayContent(final String setName) throws NoSuchDisplayException {
		Map<Display, SignageContent> restoreMap = new HashMap<Display, SignageContent>();
		String rawMessage = null;
		try {
			Connection connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				Statement stmt = connection.createStatement();
				String q = "SELECT DefaultChannels.DisplayID as DisplayID,Channel,SignageContent FROM "
						+ "DefaultChannels,DisplaySort WHERE DefaultChannels.DisplayID=DisplaySort.DisplayID AND LocationCode="
						+ getLocationCode() + " AND NameOfThisDefaultSet='" + setName + "'";
				ResultSet rs = stmt.executeQuery(q);
				if (!rs.first()) { // Move to first returned row
					printlnErr(DisplayUtilDatabase.class, "Executed a query that returned no results: " + q);
				} else
					while (!rs.isAfterLast())
						try {
							int displayID = rs.getInt("DisplayID");
							int chanNum = rs.getInt("Channel");
							SignageContent newContent = null;
							if (chanNum == 0) {
								// Set the display by the SignageContent object
								Blob sc = rs.getBlob("SignageContent");

								rawMessage = new String(sc.getBytes(1, (int) sc.length()));
								newContent = null;
								rawMessage = rawMessage.replace("version=1.0 encoding=UTF-8 standalone=yes",
										"version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"");
								rawMessage = rawMessage.replace(
										"xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance xsi:schemaLocation=http://null signage.xsd",
										"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://null\"");

								if (rawMessage.contains("changeChannelList")) {
									ChangeChannelList ccl = (ChangeChannelList) MyXMLMarshaller.unmarshall(ChangeChannelList.class,
											rawMessage);
									List<ChannelInList> lsc = new ArrayList<ChannelInList>();
									for (ChannelSpec C : ccl.getChannelSpec()) {
										lsc.add(new ChannelInListImpl(C));
									}
									newContent = new ChannelPlayList(new ConcreteChannelListHolder("Channel list " + chanNum, lsc));
								} else if (rawMessage.contains("channelSpec")) {
									newContent = (ChannelSpec) MyXMLMarshaller.unmarshall(ChannelSpec.class, rawMessage);
								} else {
									throw new ErrorProcessingMessage(
											"Unknown XML data type within this XML document: [Unrecognized XML message received] -- "
													+ rawMessage);
								}
							} else {
								newContent = getChannelFromNumber(chanNum);
							}

							Display D;
							if ((D = getContentOnDisplays().get(displayID)) != null) {
								restoreMap.put(D, newContent);
							} else {
								// TODO -- Fix this silent error!
								// This is an error condition from within the Channel Selector. This error message is not good
								// enough
								printlnErr(DisplayUtilDatabase.class,
										"Looking for displayID=" + displayID + ", but could not find it!!");
								// throw new NoSuchDisplayException("displayID=" + displayID + " no longer is a valid display");
							}

							if (!rs.next())
								break;
						} catch (Exception e) {
							if (rawMessage != null) {
								printlnErr(DisplayUtilDatabase.class, " XML is [[" + rawMessage + "]]");
							}
							e.printStackTrace();
							System.exit(-1);
						}
				stmt.close();
				rs.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		return restoreMap;
	}

	/**
	 * Save a set of default channels in ALL of the displays in this location to the database. It reads the present content from
	 * these displays and saves that as this set.
	 * 
	 * @param s
	 *            The name of this save set (The name "Default" is special in the Dynamic Display system)
	 */
	public static void saveDefaultChannels(String s) {
		try {
			Connection connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("USE " + DATABASE_NAME);

				// TODO -- If the user selects the exact same name for a save set, then we have to delete all these entries first
				// before inserting them.
				// select NameOfThisDefaultSet,LocationCode FROM DefaultChannels; -- or something like that. Should be "distinct"

				String statementStringStart = "INSERT INTO DefaultChannels VALUES (NULL,";

				ListOfExistingContent h = getContentOnDisplays();
				for (Display D : h.keySet()) {
					String blob = convertContentToDBReadyString(D.getContent());
					String fullStatement = statementStringStart + D.getDBDisplayNumber() + ", \"" + s + "\", 0, '" + blob
							+ "', NULL)";

					int nrows;
					if ((nrows = stmt.executeUpdate(fullStatement)) != 1) {
						println(DisplayUtilDatabase.class,
								"-- [" + fullStatement + "]\n\t -- expected one row modified; got " + nrows);
					}
				}
				stmt.close();
				rs.close();

			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Return all the displays that exist at the specified location
	 * 
	 * @param locationCode
	 *            - the location of interest
	 * @return The displays at this location
	 */
	public static List<Display> getDisplays(final int locationCode) {
		List<Display> theList = new ArrayList<Display>();
		int count = 0;
		List<Integer> dID = new ArrayList<Integer>();
		String query = "SELECT DisplaySort.LocationCode as LocationCode,"
				+ "Display.DisplayID as DisplayID,VirtualDisplayNumber,ScreenNumber,Location,IPName,ColorCode "
				+ "FROM Display LEFT JOIN DisplaySort ON (Display.DisplayID=DisplaySort.DisplayID) ORDER BY VirtualDisplayNumber;";

		try {
			Connection connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query);) {
					if (!rs.first()) { // Move to first returned row
						printlnErr(DisplayUtilDatabase.class, "Executed a query that returned no results: " + query);
					} else
						while (!rs.isAfterLast())
							try {
								String location = rs.getString("Location");
								String ipName = rs.getString("IPname");
								int locCode = rs.getInt("LocationCode");
								int displayID = rs.getInt("DisplayID");
								int vDisplayNum = rs.getInt("VirtualDisplayNumber");
								int screenNumber = rs.getInt("ScreenNumber");
								int colorCode = Integer.parseInt(rs.getString("ColorCode"), 16);
								if ((locationCode < 0 || locCode == locationCode) && !dID.contains(displayID)) {
									// Negative locationCode will select ALL displays everywhere
									dID.add(displayID);
									Display p = new DisplayFacade(locCode, ipName, vDisplayNum, displayID, screenNumber, location,
											new Color(colorCode));
									p.setVirtualDisplayNumber(vDisplayNum);

									theList.add(p);
									count++;
								}
								if (!rs.next())
									break;
							} catch (Exception e) {
								e.printStackTrace();
							}
					stmt.close();
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} catch (DatabaseNotVisibleException e1) {
			e1.printStackTrace();
		}

		println(DisplayUtilDatabase.class, ": Found " + count + " displays at locationCode=" + locationCode + ".\n" + query);
		return theList;
	}

	/**
	 * Find out from the database what the FLAVOR (that is, DEVELOPMENT, TEST, or PRODUCTION) that this client is supposed to look
	 * for.
	 * 
	 * @param ipName
	 *            - The name of this node, like 'display-01.fnal.gov'
	 * @return The FLAVOR of the version this node is supposed to get
	 */

	public static FLAVOR getFlavorFromDatabase(String ipName) {
		FLAVOR retval = FLAVOR.PRODUCTION;
		String query = "SELECT Flavor from Display WHERE IPName = '" + ipName + "'";
		try {
			Connection connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {

				try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query);) {
					if (!rs.first()) { // Move to first returned row
						printlnErr(DisplayUtilDatabase.class, "Executed a query that returned no results: " + query);
						return null;
					} else
						while (!rs.isAfterLast()) {
							retval = FLAVOR.valueOf(rs.getString("Flavor"));
							if (!rs.next())
								break;
						}
				}
			}
		} catch (Exception e) {
			printlnErr(DisplayUtilDatabase.class, "Exception caught and ignored: " + e.getMessage());
		}

		return retval;
	}

	/**
	 * A wrapper for the method getFlavorFromDatabase. This allows this class to be used to retrieve that flavor for a node.
	 * 
	 * @param args
	 *            (No arguments are used)
	 */
	public static void main(String[] args) {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		String myNode = "localhost";
		try {
			myNode = InetAddress.getLocalHost().getCanonicalHostName().replace(".dhcp.", ".");
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
			System.exit(-1);
		}

		FLAVOR flavor = getFlavorFromDatabase(myNode);
		if (flavor != null)
			System.out.println("FLAVOR = " + flavor);
		System.exit(0);
	}
}
