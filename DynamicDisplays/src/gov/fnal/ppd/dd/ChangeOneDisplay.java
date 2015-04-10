package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.PRIVATE_KEY_LOCATION;
import static gov.fnal.ppd.dd.GlobalVariables.THIS_IP_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.THIS_IP_NAME_INSTANCE;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.locationCode;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.chat.MessagingClient;
import gov.fnal.ppd.dd.display.DisplayListDatabaseRemote;
import gov.fnal.ppd.dd.display.client.DisplayControllerMessagingAbstract;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.JOptionPane;

/**
 * A simple, command-line program for setting the content of one display to one channel. This would be used by a scheduling system.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2015
 * 
 */
public class ChangeOneDisplay {

	/**
	 * @param server
	 *            The name of the server
	 * @param port
	 *            The port number to connect to the server on
	 * @param username
	 *            The username to use to connect to the server with. This user MUST BE AUTHORIZED with a cryptographic
	 *            public/private key pair in the Dynamic Displays database.
	 */
	public ChangeOneDisplay(final String username) {
	}

	public static void main(String[] args) {

		int targetDisplayNumber = Integer.parseInt(args[0]);
		Display display = getControllableDisplay(targetDisplayNumber);

		if (display == null)
			return;

		int targetChannelumber = Integer.parseInt(args[1]);
		SignageContent content = DisplayControllerMessagingAbstract.getChannelFromNumber(targetChannelumber);

		// Create the message

		display.setContent(content);

		System.out.println("Attempted to change display [" + display + "] to channel [" + content + "]");

		catchSleep(5000);
		System.exit(0);

	}

	private static Display getControllableDisplay(int dNum) {
		Display retval = null;
		Connection connection = null;
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		} catch (DatabaseNotVisibleException e1) {
			e1.printStackTrace();
			System.err.println("\nNo connection to the Signage/Displays database.");
			System.exit(-1);
		}

		// Use ARM to simplify these try blocks.
		try (Statement stmt = connection.createStatement();) {
			try (ResultSet rs = stmt.executeQuery("USE " + DATABASE_NAME)) {
			}

			InetAddress ip = InetAddress.getLocalHost();
			String query = "SELECT * FROM SelectorLocation where IPAddress='" + ip.getHostAddress() + "'";
			try (ResultSet rs = stmt.executeQuery(query);) {
				if (rs.first())
					try { // Move to first returned row (there can be more than one; not sure how to deal with that yet)
						locationCode = rs.getInt("LocationCode");
						System.out.println("Location code is " + locationCode);
						String myClassification = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Type"));

						IS_PUBLIC_CONTROLLER = "Public".equals(myClassification);

						final SignageType sType = SignageType.valueOf(myClassification);

						if (!MessageCarrier.initializeSignature()) {
							JOptionPane.showMessageDialog(null, "The private key file, for security, cannot be found.\n"
									+ "Click 'Dismiss' to close the GUI.\nThen get help to fix this problem.", "No Private Key",
									JOptionPane.ERROR_MESSAGE);
							System.exit(-1);
						}
						System.out.println("Initialized our digital signature from '" + PRIVATE_KEY_LOCATION + "'.");
						System.out.println("\t Expect my client name to be '" + THIS_IP_NAME + " selector " + THIS_IP_NAME_INSTANCE
								+ "'\n");

						DisplayListDatabaseRemote g = new DisplayListDatabaseRemote(locationCode, dNum);
						retval = g.get(0);
						if ( retval.getDBDisplayNumber() == dNum)
							return retval;

					} catch (Exception e) {
						e.printStackTrace();
					}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.err.println("Not sure why, but the display number " + dNum + " was not found.  Sorry.");
		return null;
	}
}
