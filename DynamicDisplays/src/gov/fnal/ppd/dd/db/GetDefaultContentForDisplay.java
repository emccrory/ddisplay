package gov.fnal.ppd.dd.db;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.util.Util.getChannelFromNumber;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import gov.fnal.ppd.dd.CredentialsNotFoundException;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

public class GetDefaultContentForDisplay {

	private static final String	DEFAULT_DEFAULT_URL	= "https://www.fnal.gov";

	private int					channelNumber		= 1;
	public int getChannelNumber() {
		return channelNumber;
	}

	public String getDisplayType() {
		return displayType;
	}

	public String getDefaultURL() {
		return defaultURL;
	}

	private String				displayType			= "XOC";
	private String				defaultURL			= DEFAULT_DEFAULT_URL;

	/**
	 * Read the database to figure out what this display shows by default
	 * 
	 * @param clazz
	 *            The type of object to instantiate here
	 * @return The object that is the display controller.
	 */
	public GetDefaultContentForDisplay() {
		String myNode = "localhost";

		try {
			myNode = InetAddress.getLocalHost().getCanonicalHostName().replace(".dhcp.", ".");
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
		}

		String query = "SELECT Content,Port FROM Display where IPName='" + myNode + "'";

		Connection connection;
		try {
			connection = ConnectionToDatabase.getDbConnection();
		} catch (DatabaseNotVisibleException e1) {
			e1.printStackTrace();
			System.err.println("\nNo connection to the Signage/Displays database.");
			return;
		}

		synchronized (connection) {
			// Use ARM to simplify these try blocks.
			try (Statement stmt = connection.createStatement();) {
				try (ResultSet rs = stmt.executeQuery("USE " + DATABASE_NAME)) {
				}

				try (ResultSet rs = stmt.executeQuery(query);) {
					// Move to first returned row (there will be more than one if there are multiple screens on this PC)
					if (rs.first()) {
						while (!rs.isAfterLast()) {
							try {

								channelNumber = rs.getInt("Content");
								if (channelNumber > 0) {
									SignageContent cont = getChannelFromNumber(channelNumber);
									if (cont instanceof Channel) {
										defaultURL = ((Channel) cont).getURI().toASCIIString();
									}
								} else {
									defaultURL = "LIST";
								}
								int dt = rs.getInt("Port");
								displayType = (dt <= 1) ? "Regular" : "FirefoxOnly";
								return;
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} else {
						new Exception("No database rows returned for query, '" + query + "'").printStackTrace();
						System.err.println("\n** Cannot continue! **\nExit");
						connection.close();
					}

				} catch (SQLException e) {
					System.err.println("Faulty query is [" + query + "]");
					e.printStackTrace();
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
		return;
	}

	public static void main(String[] args) {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		GetDefaultContentForDisplay val = new GetDefaultContentForDisplay();
		System.out.println("CHANNELNUMBER   " + val.channelNumber);
		System.out.println("DISPLAYTYPE     " + val.displayType);
		System.out.println("DEFAULTCONTENT  " + val.defaultURL);
	}
}
