package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.THIS_IP_NAME_INSTANCE;
import static gov.fnal.ppd.dd.GlobalVariables.addLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.docentName;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.locationDescription;
import static gov.fnal.ppd.dd.GlobalVariables.locationName;
import static gov.fnal.ppd.dd.GlobalVariables.messagingServerName;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.interfaces.DatabaseNotVisibleException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class GetMessagingServer {

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
					+ "as LocationCode FROM LocationInformation,DisplaySort,Display where "
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
			connection = ConnectionToDatabase.getDbConnection();
		} catch (DatabaseNotVisibleException e1) {
			e1.printStackTrace();
			System.err.println("\nNo connection to the Signage/Displays database.");
			System.exit(-1);
			return null;
		}

		synchronized (connection) {
			// Use ARM to simplify these try blocks.
			try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
				try (ResultSet rs2 = stmt.executeQuery(query);) {
					if (rs2.first()) {
						messagingServerName = rs2.getString("MessagingServerName");
						locationName = rs2.getString("LocationName");
						locationDescription = rs2.getString("Description");
						int locationCode = rs2.getInt("LocationCode");
						addLocationCode(locationCode);
						System.out.println("MessagingServer= " + messagingServerName + "\nLocationCode= " + getLocationCode()
								+ "\nLocationName= " + locationName + "\nLocationDescription= " + locationDescription);

					} else {
						System.err.println("No location information for this device, " + myName + "\n\nQuery:\n" + query);
						new Exception().printStackTrace();
						String typeOfQuery = "The database contains NO INFORMATION for a display for node, " + myName;
						String programName = "Dynamic Display";
						if (query.contains("Instance")) {
							typeOfQuery = "The database contains NO INFORMATION for a channel selector for node, " + myName
									+ ", instance='" + THIS_IP_NAME_INSTANCE + "'";
							programName = "ChannelSelector instance";
						}

						Object[] options = { "Exit program", "Show failed database query and then exit" };
						if (JOptionPane
								.showOptionDialog(
										null,
										"<html><h1>Database configuration error!</h1>"
												+ typeOfQuery
												+ ".<br>The database entry for this node must be entered properly in order to run this program.</html>",
										"Cannot start " + programName, JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null,
										options, options[0]) == 1) {
							JOptionPane.showMessageDialog(null, query.replace("from", "\nfrom").replace("FROM", "\nFROM"),
									"The failed query", JOptionPane.ERROR_MESSAGE);
						}

						System.exit(0);
					}
				}
			} catch (SQLException e) {
				System.err.println(query);
				e.printStackTrace();
			}

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
					+ "as LocationCode FROM LocationInformation,SelectorLocation where "
					+ "LocationInformation.LocationCode=SelectorLocation.LocationCode AND IPName='"
					+ myName
					+ "' AND Instance='"
					+ THIS_IP_NAME_INSTANCE + "'";

			return getMessagingServerName(query, myName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	/**
	 * Get the name of my docent for the Channel Selector
	 */
	public static void getDocentNameSelector() {
		if (docentName == null) {
			docentName = "Default";
			try {
				InetAddress ip = InetAddress.getLocalHost();
				String myName = ip.getCanonicalHostName().replace(".dhcp", "");

				String query = "select DocentName FROM SelectorLocation where IPName='" + myName + "' AND Instance='"
						+ THIS_IP_NAME_INSTANCE + "'";

				Connection connection = null;
				try {
					connection = ConnectionToDatabase.getDbConnection();
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
								docentName = rs2.getString("DocentName");
							} else {
								System.err.println("No location information for this device, " + myName + "\n\nQuery:\n" + query);
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
			System.out.println("Docent tab name is '" + docentName + "'");
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		getMessagingServerNameDisplay();
	}
}
