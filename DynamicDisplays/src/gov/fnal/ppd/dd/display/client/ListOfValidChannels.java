package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.SINGLE_IMAGE_DISPLAY;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

/**
 * FIXME - This class contains both business logic and database access
 * 
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
		int count = 0;

		try {
			connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				stmt = connection.createStatement();
				rs = stmt.executeQuery("USE " + DATABASE_NAME);
				rs = stmt.executeQuery("SELECT URL FROM Channel");
				rs.first(); // Move to first returned row
				while (!rs.isAfterLast())
					try {
						String theURL = rs.getString("URL");
						add(theURL);

						rs.next();
						count++;
					} catch (Exception e) {
						e.printStackTrace();
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
		// FIXME -- this next line is partially right. The check if this hash contains this URL is OK, but the second part, the
		// check of a fixed URL against the given URL, is inelegant. This should somehow be in the database.
		boolean r = contains(c.getURI().toString()) || c.getURI().toString().startsWith(SINGLE_IMAGE_DISPLAY);
		if (!r) {
			System.out
					.println("----- signage content " + c + " " + c.getURI().toString() + "\n----- REJECTED!  Reloading the list");
			resetChannelList();
			r = contains(c.getURI().toString());
			System.out
					.println("---- "
							+ (r ? "accepted now." : "STILL REJECTED!  The URL " + c.getURI().toString()
									+ " is deemed to be inappropriate."));
		}
		return r;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		credentialsSetup();

		ListOfValidChannels v = new ListOfValidChannels();
		for (String S : v)
			System.out.println(S);
	}
}
