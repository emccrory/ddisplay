package gov.fnal.ppd.signage.changer;

import gov.fnal.ppd.signage.SignageDatabaseNotVisibleException;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Deal with the connection to the Dynamic Displays database.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 *
 */
public class ConnectionToDynamicDisplaysDatabase {
	private static Connection	connection;

	private static String		thisNode;
	private static String		serverNode	= System.getProperty("signage.dbserver", "mccrory.fnal.gov");

	/**
	 * @return the DB connection object
	 * 
	 * @throws SignageDatabaseNotVisibleException if it cannot connect to the database server
	 */
	public static Connection getDbConnection() throws SignageDatabaseNotVisibleException {
		if (connection != null) {
			return connection;
		}

		InetAddress ip = null;
		try {
			ip = InetAddress.getLocalHost();
			thisNode = ip.getCanonicalHostName();
		} catch (UnknownHostException e) {
			System.err.println("Cannot get my own IP name.  IP Address is " + ip);
			e.printStackTrace();
		}

		if (serverNode.equals(thisNode))
			serverNode = "localhost";

		try {
			// The newInstance() call is a work around for some broken Java implementations
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("It seems that we can't load the jdbc driver: " + ex);
			System.exit(1);
		}
		try {

			String url = "jdbc:mysql://" + serverNode + "/xoc";
			String user = "xocuser";
			String passwd = "DynamicDisplays";
			connection = DriverManager.getConnection(url, user, passwd);

			// connection = DriverManager.getConnection("jdbc:mysql://" + serverNode +
			// "/xoc?user=xocuser&password=DynamicDisplays");
			return connection;

		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			ex.printStackTrace();
			if (ex.getMessage().contains("Access denied for user")) {
				System.err.println("Cannont access the Channel/Display database.");
				throw new SignageDatabaseNotVisibleException(ex.getMessage());
			} else {
				System.err.println("Aborting");
				System.exit(1);
			}
		}
		return null;
	}

	/**
	 * A simplified way to get a string from an InputStream.
	 * 
	 * @param asciiStream The InputStream to read
	 * @return The String that has been read from the InputStream
	 */
	public static String makeString(InputStream asciiStream) {
		String retval = "";
		int c;
		try {
			while ((c = asciiStream.read()) > 0) {
				retval += Character.valueOf((char) c);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return retval;
	}
}
