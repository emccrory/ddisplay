package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.messagingServerName;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GetMessagingServer {
	private static final String	ms	= System.getProperty("ddisplay.messagingserver", "X");

	private GetMessagingServer() {
	}

	/**
	 * @return The name of MY messaging server (if I am a Display)!
	 */
	public static String getMessagingServerNameDisplay() {
		return getMessagingServerName("Display");
	}

	private static String getMessagingServerName(String table) {
		if (!ms.equals("X")) {
			System.err.println("Overriding messaging server to be '" + ms + "'");
			messagingServerName = ms;
		} else {
			Connection connection = null;
			try {
				connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				System.err.println("\nNo connection to the Signage/Displays database.");
				System.exit(-1);
			}

			// Use ARM to simplify these try blocks.
			try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {

				InetAddress ip = InetAddress.getLocalHost();
				String query = "select MessagingServerName from LocationInformation," + table + " where "
						+ "LocationInformation.LocationCode=" + table + ".LocationCode and IPName='"
						+ ip.getHostName().replace(".dhcp", "") + "'";
				try (ResultSet rs2 = stmt.executeQuery(query);) {
					if (rs2.first()) {
						messagingServerName = rs2.getString("MessagingServerName");
						System.out.println("MessagingServer= " + messagingServerName);
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		return messagingServerName;
	}

	/**
	 * @return The name of MY messaging server (if I am a selector)!
	 */
	public static String getMessagingServerNameSelector() {
		return getMessagingServerName("SelectorLocation");
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		getMessagingServerNameDisplay();
	}
}
