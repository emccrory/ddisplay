package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.SINGLE_IMAGE_DISPLAY;
import static gov.fnal.ppd.dd.util.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.GeneralUtilities.printlnErr;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

/**
 * Used by the actual display instances to assure that the channel requested is an approved on in the central database.
 * 
 * FIXME - This class contains both business logic and database access
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ListOfValidChannels extends HashSet<String> {

	private static final long serialVersionUID = -6161805334135669689L;

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
				String q = "SELECT URL FROM Channel WHERE Approval=1";
				rs = stmt.executeQuery(q);
				if (!rs.first()) { // Move to first returned row
					printlnErr(getClass(),
							"\n\n** This is a big problem **\n\tThere are no approved channels in the system!  Here is the query that returned no rows: "
									+ q
									+ "\nRecommended fix: Examine your life choices and then do something else that makes you happy.\n"
									+ "\n... Or, I suppose, add some channels to the database.");
				} else
					while (!rs.isAfterLast())
						try {
							String theURL = rs.getString("URL");
							// Here is a question: Should we strip off the "http://" or the "https://" from the URL? Is this
							// something that should cause this verification to fail?? We seem to need to do this for the individual
							// photos we show here.
							add(theURL);

							if (!rs.next())
								break;
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

	private static final String singleURL = SINGLE_IMAGE_DISPLAY.replace("http://", "").replace("https://", "");

	/**
	 * @param c
	 *            the content to check
	 * @return If this bit of content is "approved"
	 */
	public boolean contains(SignageContent c) {
		// FIXME -- The check of a fixed URL for the single photo web pages is inelegant. This should somehow be in the database.
		// The single image display URL is http:// and sometimes it is https:// - this should not mess us up!

		String thisURI = c.getURI().toString();
		if (contains(thisURI))
			return true;

		thisURI = thisURI.replace("http://", "").replace("https://", "");
		if (thisURI.startsWith(singleURL))
			return true;

		System.out.println("----- The web page " + c.getURI().toString()
				+ "\n----- seems not to be in the databse!  Reloading the list: Maybe this channel was added recently.");
		resetChannelList();
		boolean r = contains(c.getURI().toString());
		System.out.println("---- " + (r ? "Yes, this channel was added recently.  All is right."
				: "The URL " + c.getURI().toString() + " is is not in the database and therefore cannot be shown."));

		// TODO - It seems appropriate to have the display show some sort of indicator that this failure has happened. At this time
		// all that happens is that this is recorded in the log file.

		return r;
	}
}
