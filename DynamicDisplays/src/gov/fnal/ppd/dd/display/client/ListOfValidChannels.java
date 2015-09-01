package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ListOfValidChannels extends HashSet<String> {

	private static final long	serialVersionUID	= -6161805334135669689L;

	/**
	 * Create a new instance by reading the list of valid URLs from the database
	 */
	public ListOfValidChannels() {
		super();
		readValidChannels();
	}

	private void readValidChannels() {
		Statement stmt = null;
		ResultSet rs = null;
		Connection connection;
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();

			synchronized (connection) {
				stmt = connection.createStatement();
				rs = stmt.executeQuery("USE " + DATABASE_NAME);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
		}

		int count = 0;
		try {
			rs = stmt.executeQuery("SELECT URL FROM Channel");
			rs.first(); // Move to first returned row
			while (!rs.isAfterLast())
				try {
					String theURL = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("URL"));
					add(theURL);

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
		println(this.getClass(), "Found " + count + " URLs.");
	}

	/**
	 * Re-read the list of valid URLs from the database
	 */
	public void resetChannelList() {
		super.clear();
		readValidChannels();
	}

	/**
	 * @param c
	 *            the content to check
	 * @return If this bit of content is "approved"
	 */
	public boolean contains(SignageContent c) {
		boolean r = contains(c.getURI().toString());
		if (!r) {
			System.out.println("----- signage content " + c + "\n----- REJECTED!  Reloading the list");
			resetChannelList();
			r = contains(c.getURI().toString());
			System.out.println("---- " + (r ? "accepted now" : "STILL REJECTED!  later, gator."));
		}
		return r;
	}

	public static void main(String[] args) {
		credentialsSetup();

		ListOfValidChannels v = new ListOfValidChannels();
		for (String S : v)
			System.out.println(S);
	}
}
