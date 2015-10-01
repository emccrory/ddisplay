/*
 * ConnectionToDynamicDisplaysDatabase
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_PASSWORD;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_USER_NAME;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

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
 * 
 */
public class ConnectionToDynamicDisplaysDatabase {
	private static Connection	connection;
	private static long			lastConnection	= 0;

	private static String		thisNode		= null;
	private static String		serverNode		= DATABASE_SERVER_NAME;

	/**
	 * @return the DB connection object.  All clients should synchronize this object in order to use it!
	 * 
	 * @throws DatabaseNotVisibleException
	 *             if it cannot connect to the database server
	 * @throws SQLException
	 *             if something goes wrong with the database connection check
	 */
	public static Connection getDbConnection() throws DatabaseNotVisibleException {
		try {
			if (connection != null && connection.isValid(100)) {
				lastConnection = System.currentTimeMillis();
				return connection;
			}
		} catch (SQLException e1) {
			// Something went wrong with the connection.isValid() check. This means we have to re-generate the connection
			println(ConnectionToDynamicDisplaysDatabase.class,
					": Connection is not valid.  Last connection was " + (System.currentTimeMillis() - lastConnection) / 1000
							+ " seconds ago.  Regenerating the connection.");
		}

		/*
		 * If the connection is too old, it fails. I got this error on 3/9/2015:
		 * 
		 * The last packet successfully received from the server was 248,955,159 milliseconds [69 hours] ago. The last packet sent
		 * successfully to the server w as 248,955,160 milliseconds ago. is longer than the server configured value of
		 * 'wait_timeout'. You should consider either expiring and/or testing connection validity before use in your application,
		 * increasing the server configured values for client timeouts, or using the Connector connection property
		 * 'autoReconnect=true' to avoid this problem.
		 */

		connection = null;
		System.gc();

		if (thisNode == null) {
			InetAddress ip = null;
			try {
				ip = InetAddress.getLocalHost();
				thisNode = ip.getCanonicalHostName();
			} catch (UnknownHostException e) {
				println(ConnectionToDynamicDisplaysDatabase.class, ": Cannot get my own IP name.  IP Address is " + ip);
				e.printStackTrace();
			}
		}

		if (serverNode.equals(thisNode))
			serverNode = "localhost";

		try {
			// The newInstance() call is a work around for some broken Java implementations
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception ex) {
			ex.printStackTrace();
			println(ConnectionToDynamicDisplaysDatabase.class, ": It seems that we can't load the jdbc driver.  "
					+ "Do not know how to recover from this error!");
			System.exit(1);
		}
		try {
			String url = "jdbc:mysql://" + serverNode + "/" + DATABASE_NAME;
			String user = DATABASE_USER_NAME;
			String passwd = DATABASE_PASSWORD;
			println(ConnectionToDynamicDisplaysDatabase.class, ":\n\t\t\t\tConnection is through [" + url
					+ "]\n\t\t\t\twith username [" + user + "] and password (hidden)");
			connection = DriverManager.getConnection(url, user, passwd);
			lastConnection = System.currentTimeMillis();
			return connection;

		} catch (SQLException ex) {
			println(ConnectionToDynamicDisplaysDatabase.class, " -- SQLException: '" + ex.getMessage() + "'");
			println(ConnectionToDynamicDisplaysDatabase.class, " -- SQLState: " + ex.getSQLState());
			println(ConnectionToDynamicDisplaysDatabase.class, " -- VendorError: " + ex.getErrorCode());
			try {
				ex.printStackTrace();
				if (ex.getMessage().contains("Access denied for user")) {
					println(ConnectionToDynamicDisplaysDatabase.class, "ERROR Cannot access the Channel/Display database.");
				} else if (ex.getMessage().contains("Operation timed out")) {
					println(ConnectionToDynamicDisplaysDatabase.class,
							"ERROR Cannot access the Channel/Display database, possibly because we have been idle for a long time.");

				} else if (ex.getMessage().contains("Communications link failure")) {
					println(ConnectionToDynamicDisplaysDatabase.class,
							"ERROR The database seems to have reset.  Let's try again later.");
					//
					// Bug fix. There was a call to System.exit() here. This call seems to have been made, but then it blocked
					// and did not actually exit the Java VM. I am not sure about this, but here is the Oracle doc on it:
					// http://docs.oracle.com/javase/7/docs/api/java/lang/Runtime.html#exit%28int%29
					//
					// It says:
					//
					// "If this method is invoked after the virtual machine has begun its shutdown sequence then if shutdown hooks
					// are being run this method will block indefinitely. If shutdown hooks have already been run and on-exit
					// finalization has been enabled then this method halts the virtual machine with the given status code if the
					// status is nonzero; otherwise, it blocks indefinitely."
					//
					// The displays are behaving consistent with the "block indefinitely" part. Moreover, I have seen cases where an
					// OS "kill $PID" does not work either (it requires a "kill -9 $PID").
					//
					// This would require that this method (right here) be called in quick succession (REAL quick) and the same
					// error condition was obtained. Not sure about this!
					//
				} else {
					println(ConnectionToDynamicDisplaysDatabase.class, "ERROR This is an unexpected error. Oh well.");
				}
				throw new DatabaseNotVisibleException(ex.getMessage());

			} catch (Exception e2) {
				println(ConnectionToDynamicDisplaysDatabase.class, " -- Exception within the exception!! " + e2.getMessage());
				e2.printStackTrace();
				throw new DatabaseNotVisibleException(ex.getMessage());
			}
		}
		// return null;
	}

	/**
	 * A simplified way to get a string from an InputStream.
	 * 
	 * @param asciiStream
	 *            The InputStream to read
	 * @return The String that has been read from the InputStream
	 * @throws IOException
	 */
	// public static String makeString(InputStream asciiStream) throws IOException {
	// String retval = "";
	//
	// int c;
	// while ((c = asciiStream.read()) > 0) {
	// if (c == '\\') {
	// if (Character.valueOf((char) asciiStream.read()) == 'u') {
	// String blah = "" + Character.valueOf((char) asciiStream.read()) + Character.valueOf((char) asciiStream.read())
	// + Character.valueOf((char) asciiStream.read()) + Character.valueOf((char) asciiStream.read());
	// int cod = Integer.parseInt(blah, 16);
	// retval += (char) cod;
	// }
	// } else
	// retval += Character.valueOf((char) c);
	// }
	//
	// return retval;
	// }
}
