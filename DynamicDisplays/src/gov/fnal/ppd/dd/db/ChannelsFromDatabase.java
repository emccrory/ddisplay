package gov.fnal.ppd.dd.db;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_BILLION;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getNumberOfLocations;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;
import static gov.fnal.ppd.dd.util.specific.PackageUtilities.getChannelFromNumber;

import java.net.URI;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelInListImpl;
import gov.fnal.ppd.dd.channel.ChannelListHolder;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.channel.ConcreteChannelListHolder;
import gov.fnal.ppd.dd.interfaces.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

/**
 * A collection of methods that deal with learning the channels in the database, in various contexts.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2018
 * 
 */
public class ChannelsFromDatabase {

	private static boolean verbose = false;

	public static void setVerbose(boolean v) {
		verbose = v;
	}

	private ChannelsFromDatabase() {
		// Do not instantiate
	}

	/**
	 * @param theMap
	 *            Where to save the channels that have been retrieved
	 * @throws DatabaseNotVisibleException
	 */
	public static void getChannels(final Map<String, SignageContent> theMap) throws DatabaseNotVisibleException {
		Statement stmt = null;
		ResultSet rs = null;
		Connection connection = ConnectionToDatabase.getDbConnection();

		try {
			synchronized (connection) {
				stmt = connection.createStatement();
				rs = stmt.executeQuery("USE " + DATABASE_NAME);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
			return;
		}

		int count = 0;
		Map<ChannelClassification, Integer> ccc = new HashMap<ChannelClassification, Integer>();
		try {
			String q = "SELECT Channel.Number as Number,Name,Description,URL,Type,DwellTime,Sound "
					+ "FROM Channel LEFT JOIN ChannelTabSort ON (Channel.Number=ChannelTabSort.Number) WHERE Approval=1";
			rs = stmt.executeQuery(q);
			if (!rs.first()) { // Move to first returned row
				printlnErr(ChannelsFromDatabase.class, "Empty result set for this query: " + q);
				return;
			}
			while (!rs.isAfterLast()) {
				try {
					String name = rs.getString("Name");
					String description = rs.getString("Description");
					String url = rs.getString("URL");
					int number = rs.getInt("Number");
					int dwellTime = rs.getInt("DwellTime");
					int codevalue = rs.getInt("Sound");
					String category = rs.getString("Type"); // Note that "Type" is actually the name of the tab this channel gets
															// put on.
					ChannelClassification classification = new ChannelClassification("MISCELLANEOUS");
					if (category != null)
						classification = new ChannelClassification(category);
					if (!ccc.containsKey(classification))
						ccc.put(classification, 0);
					ccc.put(classification, ccc.get(classification) + 1);
					SignageContent c = new ChannelImpl(name, classification, description, new URI(url), number, dwellTime);
					c.setCode(codevalue);
					theMap.put(name, c);
					count++;
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (!rs.next())
					break;
			}
			stmt.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (verbose) {
			String info = "Found " + count + " channels in these categories:\n";
			for (ChannelClassification C : ccc.keySet()) {
				info += "\t" + C + ": " + ccc.get(C) + "\n";
			}
			println(ChannelsFromDatabase.class, info);
		} else {
			println(ChannelsFromDatabase.class, "Got " + count + " channels from the database.");
		}
	}

	/**
	 * @param theMap
	 *            Where to save the channels that have been retrieved
	 * @throws DatabaseNotVisibleException
	 */
	public static void getImages(final Map<String, SignageContent> theMap) throws DatabaseNotVisibleException {
		Statement stmt = null;
		ResultSet rs = null;
		Connection connection = ConnectionToDatabase.getDbConnection();

		try {
			stmt = connection.createStatement();
			rs = stmt.executeQuery("USE " + DATABASE_NAME);
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
			return;
		}

		int count = 0;
		try {
			String q = "SELECT Filename,Experiment,Description,PortfolioID FROM Portfolio WHERE Type='Image' AND Approval='Approved' ORDER BY Sequence";
			rs = stmt.executeQuery(q);
			if (!rs.first()) {// Move to first returned row
				printlnErr(ChannelsFromDatabase.class, "Tried to execute a query that had no results " + q);
				return; // Unless there is not a first row
			}
			while (!rs.isAfterLast())
				try {
					String name = rs.getString("FileName");
					String descr = rs.getString("Description");
					String exp = rs.getString("Experiment");
					int imageNumber = rs.getInt("PortfolioID");

					String url = getFullURLPrefix() + "/portfolioOneSlide.php?photo=" + URLEncoder.encode(name, "UTF-8")
							+ "&caption=" + URLEncoder.encode(descr, "UTF-8");
					ChannelImage c = new ChannelImage(name, descr, new URI(url), imageNumber + ONE_BILLION, exp);
					c.setNumber(imageNumber);

					theMap.put(name, c);
					if (!rs.next())
						break;
					count++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			stmt.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		println(ChannelsFromDatabase.class, ": Found " + count + " images.");
	}

	/**
	 * @return An array of channel categories for all the approved channels in the DB.
	 */
	public static ChannelClassification[] getCategoriesDatabaseForAllChannels() {
		List<ChannelClassification> cats = new ArrayList<ChannelClassification>();

		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			connection = ConnectionToDatabase.getDbConnection();

			stmt = connection.createStatement();
			rs = stmt.executeQuery("USE " + DATABASE_NAME);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
			return null;
		}

		String query1 = "select distinct ChannelTabSort.Type,Abbreviation from ChannelTabSort,LocationTab where ChannelTabSort.Type=TabName";

		try {
			synchronized (connection) {
				// String query2 = "select DISTINCT Type from Channel,ChannelTabSort where Channel.Number=ChannelTabSort.Number and
				// Approval=1";
				rs = stmt.executeQuery(query1);
				if (rs.first()) // Move to first returned row
					while (!rs.isAfterLast())
						try {
							String cat = decode(rs.getString("ChannelTabSort.Type"));
							String abb = decode(rs.getString("Abbreviation"));
							cats.add(new ChannelClassification(cat, abb));

							if (!rs.next())
								break;
						} catch (Exception e) {
							e.printStackTrace();
						}
				else {
					System.err.println("No definition of what tabs to show for locationCode=" + getLocationCode());
					System.exit(-1);
				}
				stmt.close();
				rs.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Query was '" + query1 + "'");
		}
		ChannelClassification[] retval = cats.toArray(new ChannelClassification[0]);
		return retval;
	}

	/**
	 * @return An array of channel categories for the location specified by the current node's configuration.
	 */
	public static ChannelClassification[] getCategoriesDatabaseForLocation() {
		List<ChannelClassification> cats = new ArrayList<ChannelClassification>();

		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			connection = ConnectionToDatabase.getDbConnection();

			stmt = connection.createStatement();
			rs = stmt.executeQuery("USE " + DATABASE_NAME);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
			return null;
		}

		synchronized (connection) {
			String q = "SELECT DISTINCT TabName,Abbreviation FROM LocationTab";
			try {
				if (getNumberOfLocations() == 1) {
					if (getLocationCode() >= 0)
						q += " WHERE LocationCode=" + getLocationCode();
					// else get them all
				} else {
					// Controlling more than one location
					String extra = " WHERE (";
					for (int i = 0; i < getNumberOfLocations(); i++) {
						int lc = getLocationCode(i);
						if (i > 0)
							extra += " OR ";
						extra += " LocationCode=" + lc;
					}
					extra += ")";
					if (!extra.equals(" WHERE ()"))
						q += extra;
				}

				q += " ORDER BY Abbreviation";
				println(ChannelsFromDatabase.class, "Tab retrieval query: " + q);
				rs = stmt.executeQuery(q);
				if (rs.first()) // Move to first returned row
					while (!rs.isAfterLast())
						try {

							// | TabName .... | char(64)
							// | LocationCode | int(11)
							// | LocalID .... | int(11)
							// | Abbreviation | char(15)

							String cat = decode(rs.getString("TabName"));
							String abb = decode(rs.getString("Abbreviation"));
							cats.add(new ChannelClassification(cat, abb));

							if (!rs.next())
								break;
						} catch (Exception e) {
							e.printStackTrace();
						}
				else {
					System.err.println("No definition of what tabs to show for locationCode=" + getLocationCode());
					System.exit(-1);
				}
				stmt.close();
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
				System.err.println("Query was '" + q + "'");
			}
		}
		ChannelClassification[] retval = cats.toArray(new ChannelClassification[0]);
		return retval;
	}

	private static final String decode(final String in) {
		String working = in;
		int index;
		index = working.indexOf("\\u");
		while (index > -1) {
			int length = working.length();
			if (index > (length - 6))
				break;
			int numStart = index + 2;
			int numFinish = numStart + 4;
			String substring = working.substring(numStart, numFinish);
			int number = Integer.parseInt(substring, 16);
			String stringStart = working.substring(0, index);
			String stringEnd = working.substring(numFinish);
			working = stringStart + ((char) number) + stringEnd;
			index = working.indexOf("\\u");
		}
		return working;
	}

	/**
	 * @param docentName
	 *            The name of this docent category to retrieve
	 * @return A list of the content associated with this docent category
	 */
	public static List<SignageContent> getDocentContent(final String docentName) {
		List<SignageContent> retval = new ArrayList<SignageContent>();

		final long revertTime = 5 * ONE_MINUTE;

		// Get the channels associated with this docent.
		try {
			/**
			 * FIXME This is the a place that requires the Channel and Portfolio tables to be in the same database. To break this
			 * will require two Docent tables, one that is in the same DB as Channel and the other in the same DB as Portfolio.
			 */
			Connection connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				Statement stmt = connection.createStatement();
				Statement stmt1 = connection.createStatement();
				Statement stmt2 = connection.createStatement();
				// @SuppressWarnings("unused")
				stmt.executeQuery("USE " + DATABASE_NAME);

				String query1 = "select ChannelNumber,Name,Description,URL,DwellTime FROM Docent,Channel "
						+ "where ChannelNumber=Number AND DocentName='" + docentName + "' AND Approval=1";
				String query2 = "select PortfolioNumber,Filename,Description,Experiment FROM Docent,Portfolio "
						+ "where PortfolioNumber=PortfolioID AND DocentName='" + docentName + "' AND Approval='Approved'";

				ResultSet rsChan = stmt1.executeQuery(query1);
				ResultSet rsPort = stmt2.executeQuery(query2);

				rsChan.first(); // Move to first returned row
				while (!rsChan.isAfterLast()) {
					String name = rsChan.getString("Name");
					String descr = rsChan.getString("Description");
					String url = rsChan.getString("URL");
					long dwell = rsChan.getLong("DwellTime");
					int chanNum = rsChan.getInt("ChannelNumber");

					SignageContent c = new ChannelImpl(name, ChannelClassification.MISCELLANEOUS, descr, new URI(url), chanNum,
							dwell);
					c.setExpiration(revertTime);

					retval.add(c);
					rsChan.next();
				}

				rsPort.first(); // Move to first returned row
				while (!rsPort.isAfterLast()) {
					String name = rsPort.getString("FileName");
					String descr = rsPort.getString("Description");
					String exp = rsPort.getString("Experiment");

					String url = getFullURLPrefix() + "/portfolioOneSlide.php?photo=" + URLEncoder.encode(name, "UTF-8")
							+ "&caption=" + URLEncoder.encode(descr, "UTF-8");
					SignageContent c = new ChannelImage(name, descr, new URI(url), 0, exp);
					c.setExpiration(revertTime);

					retval.add(c);
					rsPort.next();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.out.println("*** Number of buttons for docent " + docentName + ": " + myButtonList.size() + ", display number "
		// + display.getNumber());
		return retval;
	}

	/**
	 * @param theMap
	 *            The map of the channels to return to the user. This will add to that map (user should probably clear it first)
	 */
	public static void readValidChannels(final Map<Integer, Channel> theMap) {
		Statement stmt = null;
		ResultSet rs = null;
		Connection connection;

		try {
			connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				stmt = connection.createStatement();
				rs = stmt.executeQuery("USE " + DATABASE_NAME);
				rs = stmt.executeQuery("SELECT URL,Name,Description,Number,DwellTime FROM Channel where Approval=1");
				rs.first(); // Move to first returned row
				while (!rs.isAfterLast()) {
					String theURL = rs.getString("URL");
					String name = rs.getString("Name");
					String desc = rs.getString("Description");
					int number = rs.getInt("Number");
					long dwell = rs.getLong("DwellTime");
					try {
						ChannelImpl c = new ChannelImpl(name, ChannelClassification.MISCELLANEOUS, desc, new URI(theURL), number,
								dwell);
						theMap.put(number, c);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (!rs.next())
						break;
				}
				stmt.close();
				rs.close();
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
		}

		println(ChannelsFromDatabase.class, ": Found " + theMap.size() + " valid channels.");
	}

	public static Set<SignageContent> getSpecialChannelsForDisplay(int displayID) throws DatabaseNotVisibleException {
		Set<SignageContent> retval = new HashSet<SignageContent>();
		Statement stmt = null;
		ResultSet rs = null;
		Connection connection = ConnectionToDatabase.getDbConnection();

		try {
			synchronized (connection) {
				stmt = connection.createStatement();
				rs = stmt.executeQuery("USE " + DATABASE_NAME);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
			return null;
		}

		int count = 0;
		try {

			// Get the "preferred" content for this display (stored in the table SimplifiedChannelChoice)
			List<Integer> channels = new ArrayList<Integer>();
			List<Integer> lists = new ArrayList<Integer>();
			List<Integer> pictures = new ArrayList<Integer>();

			String q1 = "SELECT Content,PortfolioID FROM SimplifiedChannelChoice WHERE DisplayID=" + displayID;
			rs = stmt.executeQuery(q1);
			if (!rs.first()) {// Move to first returned row
				printlnErr(ChannelsFromDatabase.class, "Tried to execute a query that had no results " + q1);
				// Unless there is not a first row
			} else
				while (!rs.isAfterLast()) {
					try {
						int chanNum = rs.getInt("Content");
						int portf = rs.getInt("PortfolioID");
						if (chanNum < 0)
							lists.add(-chanNum);
						else if (chanNum > 0)
							channels.add(chanNum);
						// Skip chanNum == 0
						if (portf > 0)
							pictures.add(portf);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (!rs.next())
						break;
				}
			rs.close();

			// Parse out the simple channels
			for (int CN : channels) {
				String q = "SELECT Channel.Number,Name,Description,URL,DwellTime,Sound "
						+ "FROM Channel WHERE Approval=1 AND Number=" + CN;
				rs = stmt.executeQuery(q);
				if (!rs.first()) {// Move to first returned row
					printlnErr(ChannelsFromDatabase.class, "Tried to execute a query that had no results " + q);
					continue; // Unless there is not a first row
				}
				while (!rs.isAfterLast()) {
					try {
						String name = rs.getString("Name");
						ChannelClassification category = ChannelClassification.MISCELLANEOUS;
						String description = rs.getString("Description");
						String url = rs.getString("URL");
						int number = rs.getInt("Number");
						int dwellTime = rs.getInt("DwellTime");
						int codevalue = rs.getInt("Sound");
						SignageContent c = new ChannelImpl(name, category, description, new URI(url), number, dwellTime);
						c.setCode(codevalue);
						retval.add(c);
						count++;
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (!rs.next())
						break;
				}
				rs.close();
			}

			for (int CL : lists) {
				List<ChannelInList> theChannelList = new ArrayList<ChannelInList>();

				String query = "SELECT ChannelList.ListNumber AS ListNumber,Number,Dwell,ChangeTime,SequenceNumber,ListName,ListAuthor"
						+ " FROM ChannelList,ChannelListName WHERE ChannelList.ListNumber=ChannelListName.ListNumber "
						+ " AND ChannelList.ListNumber=" + CL + " ORDER BY SequenceNumber ASC";

				String fullName = "void";
				try (ResultSet rs2 = stmt.executeQuery(query)) {
					if (rs2.first())
						do {
							String listName = rs2.getString("ListName");
							String listAuthor = rs2.getString("ListAuthor");
							// Special case -- abbreviate the code author's name here
							if (listAuthor.toLowerCase().contains("mccrory"))
								listAuthor = "EM";
							// int listNumber = rs2.getInt("ListNumber");
							long dwell = rs2.getLong("Dwell");
							int sequence = rs2.getInt("SequenceNumber");
							int chanNumber = rs2.getInt("Number");

							fullName = listName + " (" + listAuthor + ")";

							Channel chan = (Channel) getChannelFromNumber(chanNumber);
							ChannelInList cih = new ChannelInListImpl(chan, sequence, dwell);

							theChannelList.add(cih);

						} while (rs2.next());
				} catch (SQLException e) {
					System.err.println(query);
					e.printStackTrace();
				}
				// ChannelPlayList theList = new ChannelPlayList(fullName, theChannelList, 60000000);
				ChannelListHolder holder = new ConcreteChannelListHolder(fullName, theChannelList);
				ChannelPlayList theList = new ChannelPlayList(holder, 60000000);
				retval.add(theList);
			}

			for (int PF : pictures) {

				String q = "SELECT Filename,Experiment,Description,PortfolioID FROM Portfolio "
						+ "WHERE Type='Image' AND Approval='Approved' AND PortfolioID=" + PF;
				rs = stmt.executeQuery(q);
				if (!rs.first()) {
					printlnErr(ChannelsFromDatabase.class, "Executed a query that returned no results: " + q);
				} else
					while (!rs.isAfterLast())
						try {
							String name = rs.getString("FileName");
							String descr = rs.getString("Description");
							String exp = rs.getString("Experiment");
							int imageNumber = rs.getInt("PortfolioID");

							String url = getFullURLPrefix() + "/portfolioOneSlide.php?photo=" + URLEncoder.encode(name, "UTF-8")
									+ "&caption=" + URLEncoder.encode(descr, "UTF-8");
							ChannelImage c = new ChannelImage(name, descr, new URI(url), imageNumber + ONE_BILLION, exp);
							c.setNumber(imageNumber);

							retval.add(c);
							if (!rs.next())
								break;
							count++;
						} catch (Exception e) {
							e.printStackTrace();
						}
			}
			rs.close();

			stmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		println(ChannelsFromDatabase.class, ": Found " + count + " 'simplified' channels.");
		return retval;
	}
}
