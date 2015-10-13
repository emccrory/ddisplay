package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A simple utility class to see if this node has an entry in the SelectorLocation table
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class HasChannelSelector {

	/**
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		boolean hasChannelSelector = false;

		credentialsSetup();

		try {
			InetAddress ip = InetAddress.getLocalHost();
			String myName = ip.getCanonicalHostName().replace(".dhcp", "");

			String query = "select count(*) from SelectorLocation where IPName='" + myName + "'";

			Connection connection = null;
			try {
				connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				System.err.println("\nNo connection to the Signage/Displays database.");
				System.exit(-1);
			}

			synchronized (connection) {
				// Use ARM to simplify these try blocks.
				try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
					try (ResultSet rs2 = stmt.executeQuery(query);) {
						if (rs2.first()) {
							int count = rs2.getInt("COUNT(*)");
							hasChannelSelector |= (count != 0);
						} else {
							System.err.println("No location information for this device, " + myName);
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
		
		System.out.println (hasChannelSelector? "This node is expected to run a channel selector" : "This node will NOT run a channel selector");
		System.exit(hasChannelSelector ? 0 : -1);

	}
}