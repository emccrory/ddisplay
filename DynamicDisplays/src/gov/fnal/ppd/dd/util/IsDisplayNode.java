package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import gov.fnal.ppd.dd.db.ConnectionToDatabase;

/**
 * A simple utility class to see if this node is supposed to show a display.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class IsDisplayNode {

	/**
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		boolean hasDisplay = false;

		credentialsSetup();

		try {
			InetAddress ip = InetAddress.getLocalHost();
			String myName = ip.getCanonicalHostName().replace(".dhcp", "");

			String query = "select count(*) from Display where IPName='" + myName + "'";

			Connection connection = null;
			try {
				connection = ConnectionToDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				System.err.println("\nNo connection to the Signage/Displays database.");
				System.exit(-1);
				return;
			}

			synchronized (connection) {
				// Use ARM to simplify these try blocks.
				try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
					try (ResultSet rs2 = stmt.executeQuery(query);) {
						if (rs2.first()) {
							int count = rs2.getInt("COUNT(*)");
							hasDisplay |= (count != 0);
						} else {
							System.err.println("No information for this node, " + myName);
							new Exception().printStackTrace();
							System.exit(-1);
						}
					}
				} catch (SQLException e) {
					System.err.println(query);
					e.printStackTrace();
				}

			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		System.out.println (hasDisplay? "This node is expected to run a display" : "This node will NOT run a display");
		System.exit(hasDisplay ? 0 : -1);

	}
}
