package gov.fnal.ppd.dd.db;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_BILLION;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getNumberOfLocations;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.net.URI;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A collection of methods that deal with learning the channels in the database, in various contexts.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2018
 * 
 */
public class ChannelsFromDatabase {

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
		try {
			rs = stmt.executeQuery("SELECT Channel.Number as Number,Name,Description,URL,Category,Type,DwellTime,Sound "
					+ "FROM Channel LEFT JOIN ChannelTabSort ON (Channel.Number=ChannelTabSort.Number) WHERE Approval=1");
			rs.first(); // Move to first returned row
			while (!rs.isAfterLast()) {
				try {
					String name = rs.getString("Name");
					ChannelCategory category = ChannelCategory.MISCELLANEOUS;
					if (rs.getAsciiStream("Type") != null) {
						String cat = rs.getString("Type").toUpperCase();
						category = new ChannelCategory(cat);
					}
					String description = rs.getString("Description");
					String url = rs.getString("URL");
					int number = rs.getInt("Number");
					int dwellTime = rs.getInt("DwellTime");
					int codevalue = rs.getInt("Sound");
					SignageContent c = new ChannelImpl(name, category, description, new URI(url), number, dwellTime);
					c.setCode(codevalue);
					theMap.put(name, c);
					count++;
				} catch (Exception e) {
					e.printStackTrace();
				}
				rs.next();
			}
			stmt.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		println(ChannelsFromDatabase.class, ": Found " + count + " channels.");
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
			rs = stmt
					.executeQuery("SELECT Filename,Experiment,Description,PortfolioID FROM Portfolio WHERE Type='Image' AND Approval='Approved' ORDER BY Sequence");
			rs.first(); // Move to first returned row
			while (!rs.isAfterLast())
				try {
					String name = rs.getString("FileName");
					String descr = rs.getString("Description");
					String exp = rs.getString("Experiment");
					int imageNumber = rs.getInt("PortfolioID");

					String url = getFullURLPrefix() + "/portfolioOneSlide.php?photo=" + URLEncoder.encode(name, "UTF-8")
							+ "&caption=" + URLEncoder.encode(descr, "UTF-8");
					ChannelImage c = new ChannelImage(name, ChannelCategory.IMAGE, descr, new URI(url), imageNumber + ONE_BILLION,
							exp);
					c.setNumber(imageNumber);

					theMap.put(name, c);
					rs.next();
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
	 * @return An array of channel categories
	 */
	public static ChannelCategory[] getCategoriesDatabase() {
		List<ChannelCategory> cats = new ArrayList<ChannelCategory>();

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
					if (IS_PUBLIC_CONTROLLER) {
						if (getLocationCode() < 0)
							q += " WHERE Type='Public'";
						else
							q += " WHERE LocationCode=" + getLocationCode() + " AND Type='Public'";
					} else {
						if (getLocationCode() >= 0)
							q += " WHERE LocationCode=" + getLocationCode();
					}
				} else {
					// Controlling more than one location
					String extra = " WHERE (";
					for (int i = 0; i < getNumberOfLocations(); i++) {
						int lc = getLocationCode(i);
						if (i > 0)
							extra += " OR ";
						extra += " LocationCode=" + lc;
					}
					if (IS_PUBLIC_CONTROLLER)
						extra += ") AND Type='Public'";
					else
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
							// | Type ....... | enum('Public','Experiment','XOC')
							// | Abbreviation | char(15)

							String cat = decode(rs.getString("TabName"));
							String abb = decode(rs.getString("Abbreviation"));
							cats.add(new ChannelCategory(cat, abb));

							rs.next();
						} catch (Exception e) {
							e.printStackTrace();
						}
				else {
					System.err.println("No definition of what tabs to show for locationCode=" + getLocationCode()
							+ " and Controller Type=" + (IS_PUBLIC_CONTROLLER ? "Public" : "XOC"));
					System.exit(-1);
				}
				stmt.close();
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
				System.err.println("Query was '" + q + "'");
			}
		}
		ChannelCategory[] retval = cats.toArray(new ChannelCategory[0]);
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

				String query1 = "select ChannelNumber,Name,Description,URL,DwellTime,Category from Docent,Channel "
						+ "where ChannelNumber=Number AND DocentName='" + docentName + "' AND Approval=1";
				String query2 = "select PortfolioNumber,Filename,Description,Experiment from Docent,Portfolio "
						+ "where PortfolioNumber=PortfolioID AND DocentName='" + docentName + "' AND Approval='Approved'";

				ResultSet rsChan = stmt1.executeQuery(query1);
				ResultSet rsPort = stmt2.executeQuery(query2);

				rsChan.first(); // Move to first returned row
				while (!rsChan.isAfterLast()) {
					String name = rsChan.getString("Name");
					String descr = rsChan.getString("Description");
					String url = rsChan.getString("URL");
					String category = rsChan.getString("Category");
					long dwell = rsChan.getLong("DwellTime");
					int chanNum = rsChan.getInt("ChannelNumber");

					SignageContent c = new ChannelImpl(name, new ChannelCategory(category, category), descr, new URI(url), chanNum,
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
					SignageContent c = new ChannelImage(name, ChannelCategory.IMAGE, descr, new URI(url), 0, exp);
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
				rs = stmt.executeQuery("SELECT * FROM Channel where Approval=1");
				rs.first(); // Move to first returned row
				while (!rs.isAfterLast()) {
					String theURL = rs.getString("URL");
					String name = rs.getString("Name");
					String cat = rs.getString("Category");
					String desc = rs.getString("Description");
					int number = rs.getInt("Number");
					long dwell = rs.getLong("DwellTime");
					try {
						ChannelImpl c = new ChannelImpl(name, new ChannelCategory(cat), desc, new URI(theURL), number, dwell);
						theMap.put(number, c);
					} catch (Exception e) {
						e.printStackTrace();
					}
					rs.next();
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

}
