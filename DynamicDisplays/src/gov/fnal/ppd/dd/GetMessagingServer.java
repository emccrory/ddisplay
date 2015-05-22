package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.addLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.locationDescription;
import static gov.fnal.ppd.dd.GlobalVariables.locationName;
import static gov.fnal.ppd.dd.GlobalVariables.messagingServerName;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class GetMessagingServer {
	private static final String	ms	= System.getProperty("ddisplay.messagingserver", "X");

	private GetMessagingServer() {
	}

	/**
	 * @return The name of MY messaging server (if I am a Display)!
	 */
	public static String getMessagingServerNameDisplay() {
		try {
			InetAddress ip = InetAddress.getLocalHost();
			String myName = ip.getCanonicalHostName().replace(".dhcp", "");

			String query = "select MessagingServerName,LocationInformation.LocationName,LocationInformation.Description,LocationInformation.LocationCode "
					+ "as LocationCode from LocationInformation,DisplaySort,Display where "
					+ "LocationInformation.LocationCode=DisplaySort.LocationCode AND "
					+ "DisplaySort.DisplayID=Display.DisplayID and IPName='" + myName + "'";

			return getMessagingServerName(query, myName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	private static String getMessagingServerName(String query, String myName) {

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
			try (ResultSet rs2 = stmt.executeQuery(query);) {
				if (rs2.first()) {
					messagingServerName = rs2.getString("MessagingServerName");
					locationName = rs2.getString("LocationName");
					locationDescription = rs2.getString("Description");
					int locationCode = rs2.getInt("LocationCode");
					addLocationCode(locationCode);
					println(GetMessagingServer.class, "MessagingServer= " + messagingServerName + ", LocationCode="
							+ getLocationCode() + ", LocationName= " + locationName + ", LocationDescription= "
							+ locationDescription);

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

		if (!ms.equals("X")) {
			System.err.println("Overriding messaging server to be '" + ms + "'");
			messagingServerName = ms;
		}

		return messagingServerName;
	}

	/**
	 * @return The name of MY messaging server (if I am a selector)!
	 */
	public static String getMessagingServerNameSelector() {
		try {
			InetAddress ip = InetAddress.getLocalHost();
			String myName = ip.getCanonicalHostName().replace(".dhcp", "");

			String query = "select MessagingServerName,LocationInformation.LocationName,LocationInformation.Description,LocationInformation.LocationCode "
					+ "as LocationCode from LocationInformation,SelectorLocation where "
					+ "LocationInformation.LocationCode=SelectorLocation.LocationCode AND IPName='" + myName + "'";

			return getMessagingServerName(query, myName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		getMessagingServerNameDisplay();
	}
}
