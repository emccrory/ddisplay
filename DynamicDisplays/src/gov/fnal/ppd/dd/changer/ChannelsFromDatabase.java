/*
 * ChannelsFromDatabase
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Return all the channels from the database
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelsFromDatabase extends HashMap<String, SignageContent> implements ChannelCatalog {

	private static final long							serialVersionUID	= 8020508216810250903L;

	private final Connection							connection;

	private SignageContent								defaultChannel;

	private static Comparator<? super SignageContent>	comparator			= new Comparator<SignageContent>() {

																				/**
																				 * Compare by the name of the channel
																				 * 
																				 * @param o1
																				 * @param o2
																				 * @return the comparison result
																				 */
																				public int compare(SignageContent o1,
																						SignageContent o2) {
																					return o1.getName().compareTo(o2.getName());
																				}

																			};

	/**
	 * @return The existing comparator being used by the channel creation method
	 */
	public static Comparator<? super SignageContent> getComparator() {
		return comparator;
	}

	/**
	 * @param comparator
	 *            The replacement comparator for creating the channel list.
	 */
	public static void setComparator(final Comparator<? super SignageContent> comparator) {
		ChannelsFromDatabase.comparator = comparator;
	}

	ChannelsFromDatabase() throws DatabaseNotVisibleException {
		connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		synchronized (connection) {
			getChannels();
			getImages();
		}

		defaultChannel = get(keySet().iterator().next()); // The first channel (whatever!)
	}

	private void getChannels() {
		Statement stmt = null;
		ResultSet rs = null;

		try {
			synchronized (connection) {
				stmt = connection.createStatement();
				rs = stmt.executeQuery("USE " + DATABASE_NAME);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		}

		int count = 0;
		try {
			rs = stmt.executeQuery("SELECT Channel.Number as Number,Name,Description,URL,Category,Type,DwellTime "
					+ "FROM Channel LEFT JOIN ChannelTabSort ON (Channel.Number=ChannelTabSort.Number) WHERE Approval=1");
			rs.first(); // Move to first returned row
			while (!rs.isAfterLast())
				try {
					String name = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Name"));
					ChannelCategory category = ChannelCategory.MISCELLANEOUS;
					if (rs.getAsciiStream("Type") != null) {
						String cat = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Type")).toUpperCase();
						category = new ChannelCategory(cat);
					}
					String description = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Description"));
					String url = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("URL"));
					int number = rs.getInt("Number");
					int dwellTime = rs.getInt("DwellTime");
					SignageContent c = new ChannelImpl(name, category, description, new URI(url), number, dwellTime);
					put(name, c);
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
		System.out.println(getClass().getSimpleName() + ": Found " + count + " channels.");
	}

	private void getImages() {
		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = connection.createStatement();
			rs = stmt.executeQuery("USE " + DATABASE_NAME);
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		}

		int count = 0;
		try {
			rs = stmt
					.executeQuery("select Filename,Experiment,Description from Portfolio where Type='Image' and Approval='Approved'");
			rs.first(); // Move to first returned row
			while (!rs.isAfterLast())
				try {
					String name = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("FileName"));
					String descr = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Description"));
					String exp = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Experiment"));

					String url = getFullURLPrefix() + "/portfolioOneSlide.php?photo=" + URLEncoder.encode(name, "UTF-8")
							+ "&caption=" + URLEncoder.encode(descr, "UTF-8");
					SignageContent c = new ChannelImage(name, ChannelCategory.IMAGE, descr, new URI(url), 0, exp);

					put(name, c);
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
		System.out.println(getClass().getSimpleName() + ": Found " + count + " images.");
	}

	public Map<String, SignageContent> getPublicChannels() {
		HashMap<String, SignageContent> retval = new HashMap<String, SignageContent>();

		for (String key : this.keySet()) {
			if (this.get(key).getCategory().equals(ChannelCategory.PUBLIC))
				retval.put(key, this.get(key));
		}
		return retval;
	}

	public Set<SignageContent> getChannelCatalog(ChannelCategory cat) {
		TreeSet<SignageContent> retval = new TreeSet<SignageContent>(comparator);

		for (String key : this.keySet())
			if (this.get(key).getCategory().equals(cat)) {
				SignageContent C = this.get(key);
				if (C instanceof ChannelImage)
					retval.add(new ChannelImage(C));
				else if (C instanceof Channel)
					retval.add(new ChannelImpl(C));
			}
		return retval;
	}

	public SignageContent getDefaultChannel() {
		return defaultChannel;
	}

}
