package gov.fnal.ppd.signage.changer;

import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.SignageDatabaseNotVisibleException;
import gov.fnal.ppd.signage.channel.ChannelImpl;

import java.net.URI;
import java.net.URISyntaxException;
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
 * @copyright 2014
 * 
 */
public class ChannelsFromDatabase extends HashMap<String, SignageContent> implements ChannelCatalog {

	private static final long								serialVersionUID	= 8020508216810250903L;

	private Connection										connection;

	private SignageContent									defaultChannel;

	private static final Comparator<? super SignageContent>	comparator			= new Comparator<SignageContent>() {

																					public int compare(SignageContent o1,
																							SignageContent o2) {
																						return o1.getName().compareTo(o2.getName());
																					}

																				};

	ChannelsFromDatabase() {
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		} catch (SignageDatabaseNotVisibleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		getChannels();
		defaultChannel = get(keySet().iterator().next()); // The first channel (whatever!)
	}

	private void getChannels() {
		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = connection.createStatement();
			rs = stmt.executeQuery("USE xoc");
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		}

		int count = 0;
		try {
			rs = stmt.executeQuery("SELECT Channel.Number as Number,Name,Description,URL,Category,Type "
					+ "FROM Channel LEFT JOIN ChannelTabSort ON (Channel.Number=ChannelTabSort.Number)");
			rs.first(); // Move to first returned row
			while (!rs.isAfterLast()) {
				String name = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Name"));
				ChannelCategory category = ChannelCategory.MISCELLANEOUS;
				try {
					String cat = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Type")).toUpperCase();
					category = ChannelCategory.valueOf(cat);
				} catch (Exception e) {
				}
				String description = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Description"));
				String url = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("URL"));
				int number = rs.getInt("Number");
				SignageContent c = new ChannelImpl(name, category, description, new URI(url), number);
				put(name, c);
				rs.next();
				count++;
			}
			stmt.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		System.out.println(getClass().getSimpleName() + ": Found " + count + " channels.");
	}

	public Map<String, SignageContent> getPublicChannels() {
		HashMap<String, SignageContent> retval = new HashMap<String, SignageContent>();

		for (String key : this.keySet()) {
			if (this.get(key).getCategory() == ChannelCategory.PUBLIC)
				retval.put(key, this.get(key));
		}
		return retval;
	}

	public Map<String, SignageContent> getDetailsChannels() {
		HashMap<String, SignageContent> retval = new HashMap<String, SignageContent>();

		for (String key : this.keySet()) {
			if (this.get(key).getCategory() == ChannelCategory.PUBLIC_DETAILS)
				retval.put(key, this.get(key));
		}
		return retval;
	}

	public Set<SignageContent> getChannelCatalog(ChannelCategory cat) {
		TreeSet<SignageContent> retval = new TreeSet<SignageContent>(comparator);

		for (String key : this.keySet()) {
			if (this.get(key).getCategory() == cat)
				retval.add(this.get(key));
		}
		return retval;
	}

	public Map<String, SignageContent> getAllDetailsChannels() {
		HashMap<String, SignageContent> retval = new HashMap<String, SignageContent>();

		for (String key : this.keySet()) {
			if (this.get(key).getCategory() == ChannelCategory.PUBLIC_DETAILS
					|| this.get(key).getCategory() == ChannelCategory.EXPERIMENT_DETAILS)
				retval.put(key, this.get(key));
		}
		return retval;
	}

	public SignageContent getDefaultChannel() {
		return defaultChannel;
	}

}
