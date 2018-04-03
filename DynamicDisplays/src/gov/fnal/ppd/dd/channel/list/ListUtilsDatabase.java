package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_BILLION;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.net.URI;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A collection of the methods needed to deal with channel lists in the database.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2018
 * 
 */
public class ListUtilsDatabase {
	private static String	lastListName	= null;

	private ListUtilsDatabase() {
	}

	/**
	 * Get the list of channels that are currently defined.
	 * 
	 * Database access in this method
	 * 
	 * @return A list of strings of these list names
	 * @throws Exception
	 */
	public static List<String> getExistingList() throws Exception {
		List<String> retval = new ArrayList<String>();
		Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		// TODO - Add a new column to this table that says if the list is valid or not (which would be used to delete the list but
		// retain it if it is needed later)
		String query = "Select ListName,ListAuthor,ListNumber from ChannelListName";

		synchronized (connection) {
			try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
				try (ResultSet rs2 = stmt.executeQuery(query)) {
					if (rs2.first())
						do {
							String listName = rs2.getString("ListName");
							String listAuthor = rs2.getString("ListAuthor");
							// Special Case: The code author - make his name shorter
							if (listAuthor.toLowerCase().contains("mccrory"))
								listAuthor = "EM";
							int listNumber = rs2.getInt("ListNumber");
							retval.add((listNumber < 10 ? "0" : "") + listNumber + ": " + listName + " (" + listAuthor + ")");
						} while (rs2.next());
					else {
						// Oops. no first element!?
						throw new Exception("No channel lists in the save/restore database!");
					}
				} catch (SQLException e) {
					System.err.println(query);
					e.printStackTrace();
				}
			} catch (SQLException e) {
				System.err.println(query);
				e.printStackTrace();
			}
		}
		return retval;
	}

	/**
	 * Retrieve all the channels in list number "listNumber"
	 * 
	 * @param listNumber
	 * @return The channels in the list number
	 */
	public static List<ChannelInList> getThisList(int listNumber) {
		Connection connection;
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
			return null;
		}
		List<ChannelInList> retval = new ArrayList<ChannelInList>();

		String query1 = "SELECT Number,Dwell,SequenceNumber FROM ChannelList where ListNumber=" + listNumber
				+ " ORDER BY SequenceNumber ASC";

		synchronized (connection) {
			try (Statement stmt1 = connection.createStatement(); ResultSet rs1 = stmt1.executeQuery("USE " + DATABASE_NAME)) {
				// try (ResultSet rs2 = stmt.executeQuery(query)) {
				try (ResultSet rs2 = stmt1.executeQuery(query1)) {
					if (rs2.first())
						do {
							int chanNumber = rs2.getInt("Number");
							int seq = rs2.getInt("SequenceNumber");
							long dwell = rs2.getLong("Dwell");
							ChannelInList cih = null;
							String name = null;

							// Sub-query based on the Number value here
							if (chanNumber > ONE_BILLION) {
								int portfolioID = chanNumber - ONE_BILLION;
								try (Statement stmt2 = connection.createStatement()) {
									String query2 = "SELECT Filename,Description,Experiment FROM Portfolio WHERE Approval='Approved' AND PortfolioID="
											+ portfolioID;
									ResultSet rs3 = stmt2.executeQuery(query2);

									if (rs3.first()) {
										name = rs3.getString("Filename");
										String desc = rs3.getString("Description");
										String exp = rs3.getString("Experiment");

										String url = getFullURLPrefix() + "/portfolioOneSlide.php?photo="
												+ URLEncoder.encode(name, "UTF-8") + "&caption=" + URLEncoder.encode(desc, "UTF-8");
										URI uri = new URI(url);
										Channel channel = new ChannelImage(name, ChannelCategory.IMAGE, desc, uri, portfolioID, exp);
										channel.setTime(dwell);
										cih = new ChannelInList(channel, seq, dwell);
									} else {
										println(ListUtilsDatabase.class,
												"getThisList(): Unexpected condition - rs3.first() returned false");
									}
								} catch (Exception e) {
									e.printStackTrace();
									continue;
								}
							} else {
								try (Statement stmt2 = connection.createStatement()) {
									String query2 = "SELECT Name,URL,Description,Category FROM Channel WHERE Approval=1 AND Number="
											+ chanNumber;

									ResultSet rs3 = stmt2.executeQuery(query2);
									if (rs3.first()) {
										name = rs3.getString("Name");
										String url = rs3.getString("URL");
										String desc = rs3.getString("Description");
										URI uri = new URI(url);
										Channel channel = new ChannelImpl(name, ChannelCategory.MISCELLANEOUS, desc, uri,
												chanNumber, dwell);
										cih = new ChannelInList(channel, seq, dwell);
									} // Guaranteed to be exactly one
								} catch (Exception e) {
									e.printStackTrace();
									continue;
								}
							}
							if (cih != null) {
								retval.add(cih);
							} else {
								println(ListUtilsDatabase.class, "getThisList(): NULL Channel.  It is likely that channel number "
										+ chanNumber + " is not approved.");
							}

						} while (rs2.next());
					else {
						// Oops. no first element!?
						System.err.println("No elements in the save/restore database for list number " + listNumber + "!");
						return null;
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				String query = "SELECT ListName from ChannelListName WHERE ListNumber=" + listNumber;

				try (ResultSet rs3 = stmt1.executeQuery(query)) {
					if (rs3.first()) {
						do {
							lastListName = rs3.getString("ListName");
						} while (rs3.next());
					} else {
						// Oops. no first element!?
						System.err.println("No elements in the save/restore database for list number " + listNumber + "!");
						return null;
					}
				} catch (SQLException e) {
					System.err.println(query);
					e.printStackTrace();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return retval;
	}

	/**
	 * @return the lastListName
	 */
	public static String getLastListName() {
		return lastListName;
	}

	/**
	 * Delete just the channels from a database list
	 * 
	 * @param listNumber
	 */
	public static void deleteChannelsFromDatabaseList(int listNumber) {

		String query1 = "DELETE FROM ChannelList     WHERE ListNumber=" + listNumber;

		try {
			Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			synchronized (connection) {
				Statement stmt = connection.createStatement();
				stmt.executeQuery("USE " + DATABASE_NAME);
				stmt.executeUpdate(query1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return; // return "An exception caused this to fail.";
		}
		println(ListUtilsDatabase.class, "All channels removed from list number " + listNumber + ".");
	}

	/**
	 * Entirely delete a list from the database.
	 * 
	 * @param listNumber
	 */
	public static void deleteListFromDatabase(int listNumber) {

		String query1 = "DELETE FROM ChannelList     WHERE ListNumber=" + listNumber;
		String query2 = "DELETE FROM ChannelListName WHERE ListNumber=" + listNumber;

		try {
			Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			synchronized (connection) {
				Statement stmt = connection.createStatement();
				stmt.executeQuery("USE " + DATABASE_NAME);
				stmt.executeUpdate(query1);
				stmt.executeUpdate(query2);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return; // return "An exception caused this to fail.";
		}
		println(ListUtilsDatabase.class, "List number " + listNumber + " deleted");
	}

	/**
	 * @param list
	 * @return boolean if the list exists in the DB already
	 */
	public static boolean doesListExist(final String list) {
		try {
			Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			synchronized (connection) {
				String isItThereAlready = "SELECT ListNumber from ChannelListName WHERE ListName='" + list + "'";

				try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
					// Does this list already exist in the database?
					try (ResultSet rs2 = stmt.executeQuery(isItThereAlready)) {
						if (rs2.first()) {
							int listNumber = rs2.getInt("ListNumber"); // Got the list number.
							return (listNumber > 0);
						}
					} catch (Exception e) {
						// Not a problem -- this list does not exist yet.
					}
				} catch (Exception e) {
					// An exception here might be a problem
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// An exception here might be a problem
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Writes a sequence of channels to an existing list number
	 * 
	 * @param listName
	 * @param theList
	 *            The channels in this list
	 */
	public static void saveListToDatabase(String listName, List<ChannelInList> theList) {
		try {
			Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			synchronized (connection) {
				int listNumber = 0;

				String list = lastListName = listName;
				if (list == null || list.length() == 0)
					list = "" + new Date();

				String retrieve = "SELECT ListNumber from ChannelListName WHERE ListName='" + list + "'";

				try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {

					try {
						println(ListUtilsDatabase.class, ": Re-saving the list called '" + list + "'");

						ResultSet rs2 = stmt.executeQuery(retrieve);
						if (rs2.first())
							listNumber = rs2.getInt(1);

					} catch (Exception e) {
						e.printStackTrace();
						return; // return "Reading the list number, which we just created, failed";
					}

					// Use the exiting channel list from the GUI
					int sequence = 0;
					for (ChannelInList C : theList) {
						String insert = "INSERT INTO ChannelList VALUES (NULL, " + listNumber + ", " + C.getNumber() + ", "
								+ C.getTime() + ", NULL, " + C.getSequenceNumber() + ")";
						if (stmt.executeUpdate(insert) == 1) {
							// OK, the insert worked.
							println(ListUtilsDatabase.class, ": Successful insert " + insert);
						} else {
							new Exception("Insert of list element number " + sequence + " failed").printStackTrace();
							return; // return "Insert of list element number " + sequence + " failed";
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return; // return "An exception caused this to fail.";
		}
	}

	/**
	 * Writes a sequence of channels to an existing list number
	 * 
	 * @param listName
	 * @param authorName
	 * @param theList
	 *            The channels in this list
	 */
	public static void saveListToDatabase(String listName, String authorName, List<ChannelInList> theList) {
		try {
			Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			synchronized (connection) {
				int listNumber = 0;

				String list = lastListName = listName;
				if (list == null || list.length() == 0)
					list = "" + new Date();

				String insertNewList = "INSERT INTO ChannelListName VALUES (NULL, '" + listName + ",, '" + authorName + "')";
				// TODO Need to force the name of the list to be unique (or maybe name+author?)

				try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {

					try {
						println(ListUtilsDatabase.class, ": Creating a list called '" + list + "'");
						if (stmt.executeUpdate(insertNewList) == 1)
							println(ListUtilsDatabase.class, ": Successful insert " + insertNewList);

					} catch (Exception e) {
						e.printStackTrace();
						return; // return "Reading the list number, which we just created, failed";
					}

					String retrieve = "SELECT ListNumber from ChannelListName WHERE ListName='" + list + "'";

					ResultSet rs2 = stmt.executeQuery(retrieve);
					if (rs2.first())
						listNumber = rs2.getInt(1);

					if (listNumber != 0) {
						// Use the exiting channel list from the GUI
						int sequence = 0;
						for (ChannelInList C : theList) {
							String insert = "INSERT INTO ChannelList VALUES (NULL, " + listNumber + ", " + C.getNumber() + ", "
									+ C.getTime() + ", NULL, " + C.getSequenceNumber() + ")";
							if (stmt.executeUpdate(insert) == 1) {
								// OK, the insert worked.
								println(ListUtilsDatabase.class, ": Successful insert " + insert);
							} else {
								new Exception("Insert of list element number " + sequence + " failed").printStackTrace();
								return; // return "Insert of list element number " + sequence + " failed";
							}
						}
					} else {
						println(ListUtilsDatabase.class, "Something funny happened...");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return; // return "An exception caused this to fail.";
		}
	}

}
