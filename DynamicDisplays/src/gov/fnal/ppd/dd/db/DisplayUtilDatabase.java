package gov.fnal.ppd.dd.db;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.getContentOnDisplays;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.util.Util.convertContentToDBReadyString;
import static gov.fnal.ppd.dd.util.Util.getChannelFromNumber;
import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
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
import gov.fnal.ppd.dd.display.DisplayFacade;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

/**
 * This class gets the channels that each display is supposed to show for a particular save set.
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
	 * @param setName
	 * @return the content at each display
	 */
	public static Map<Display, SignageContent> getDisplayContent(final String setName) throws NoSuchDisplayException {
		Map<Display, SignageContent> restoreMap = new HashMap<Display, SignageContent>();
		try {
			Connection connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT DefaultChannels.DisplayID as DisplayID,Channel,SignageContent FROM "
						+ "DefaultChannels,DisplaySort WHERE DefaultChannels.DisplayID=DisplaySort.DisplayID AND LocationCode="
						+ getLocationCode() + " AND NameOfThisDefaultSet='" + setName + "'");
				rs.first(); // Move to first returned row
				while (!rs.isAfterLast())
					try {
						int displayID = rs.getInt("DisplayID");
						int chanNum = rs.getInt("Channel");
						SignageContent newContent = null;
						if (chanNum == 0) {
							// Set the display by the SignageContent object
							Blob sc = rs.getBlob("SignageContent");

							if (sc != null && sc.length() > 0)
								try {
									int len = (int) sc.length();
									byte[] bytes = sc.getBytes(1, len);

									ByteArrayInputStream fin = new ByteArrayInputStream(bytes);
									ObjectInputStream ois = new ObjectInputStream(fin);
									newContent = (SignageContent) ois.readObject();

								} catch (Exception e) {
									e.printStackTrace();
								}
						} else {
							newContent = getChannelFromNumber(chanNum);
						}

						Display D;
						if ((D = getContentOnDisplays().get(displayID)) != null) {
							restoreMap.put(D, newContent);
						} else {
							// TODO -- Fix this silent error!
							// This is an error condition from within the Channel Selector.  This error message is not good enough
							printlnErr(DisplayUtilDatabase.class, "Looking for displayID=" + displayID + ", but could not find it!!");
							throw new NoSuchDisplayException("displayID=" + displayID + " no longer is a valid display");
						}

						rs.next();
					} catch (Exception e) {
						e.printStackTrace();
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
	 * Save a set of default channels in ALL of the displays in this location to the database. it reads the present content from
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
				// select NameOfThisDefaultSet,LocationCode from DefaultChannels; -- or something like that. Should be "distinct"

				String statementStringStart = "INSERT INTO DefaultChannels VALUES (NULL,";

				ListOfExistingContent h = getContentOnDisplays();
				for (Display D : h.keySet()) {
					String blob = convertContentToDBReadyString(D.getContent());
					String fullStatement = statementStringStart + D.getDBDisplayNumber() + ", \"" + s + "\", 0, x'" + blob + "')";

					int nrows;
					if ((nrows = stmt.executeUpdate(fullStatement)) != 1) {
						println(DisplayUtilDatabase.class, "-- [" + fullStatement + "]\n\t -- expected one row modified; got "
								+ nrows);
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
	 * @param locationCode
	 * @return The displays at this location
	 */
	public static List<Display> getDisplays(final int locationCode) {
		List<Display> theList = new ArrayList<Display>();
		int count = 0;
		List<Integer> dID = new ArrayList<Integer>();
		String query = "SELECT DisplaySort.LocationCode as LocationCode,Display.LocationCode as TickerCode,"
				+ "Display.DisplayID as DisplayID,VirtualDisplayNumber,ScreenNumber,Location,IPName,ColorCode,Type "
				+ "FROM Display LEFT JOIN DisplaySort ON (Display.DisplayID=DisplaySort.DisplayID) ORDER BY VirtualDisplayNumber;";
		
		try {
			Connection connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement();
						ResultSet rs = stmt
								.executeQuery(query);) {
					rs.first(); // Move to first returned row
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
							rs.next();
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

		println(DisplayUtilDatabase.class, ": Found " + count + " displays at locationCode=" + locationCode + ".\n"+ query);
		return theList;
	}

	/**
	 * @param displayDBNumber
	 * @param locationCode
	 * @return the list
	 */
	public static List<Display> getADisplay(final int displayDBNumber, final int locationCode) {
		int count = 0;
		List<Display> theList = new ArrayList<Display>();
		try {
			Connection connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement();
						ResultSet rs = stmt
								.executeQuery("SELECT Location,IPName,Display.DisplayID as DisplayID,VirtualDisplayNumber,DisplaySort.LocationCode as LocationCode,"
										+ "ScreenNumber,ColorCode,Type FROM Display LEFT JOIN DisplaySort ON (Display.DisplayID=DisplaySort.DisplayID) "
										+ "ORDER BY VirtualDisplayNumber");) {
					rs.first(); // Move to first returned row
					while (!rs.isAfterLast())
						try {
							String location = rs.getString("Location");
							String ipName = rs.getString("IPName");
							int locCode = rs.getInt("LocationCode");
							int displayID = rs.getInt("DisplayID");
							int vDisplayNum = rs.getInt("VirtualDisplayNumber");
							int screenNumber = rs.getInt("ScreenNumber");
							int colorCode = Integer.parseInt(rs.getString("ColorCode"), 16);
							if ((locationCode < 0 || locCode == locationCode) && displayDBNumber == displayID) {
								Display p = new DisplayFacade(locCode, ipName, vDisplayNum, displayID, screenNumber, location,
										new Color(colorCode));
								p.setVirtualDisplayNumber(vDisplayNum);

								theList.add(p);
								count++;
							}
							rs.next();
						} catch (Exception e) {
							e.printStackTrace();
						}

					stmt.close();
					rs.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		println(DisplayUtilDatabase.class, ": Found " + count + " displays at locationCode=" + locationCode
				+ " and displayDBNumber=" + displayDBNumber);
		return theList;
	}
}
