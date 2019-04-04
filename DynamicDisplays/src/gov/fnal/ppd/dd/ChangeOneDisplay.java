/*
 * ChangeOneDisplay
 *
 * v1.0
 *
 * 2015-01-01
 * 
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.PRIVATE_KEY_LOCATION;
import static gov.fnal.ppd.dd.GlobalVariables.addLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getFullSelectorName;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import static gov.fnal.ppd.dd.db.DisplayUtilDatabase.getADisplay;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.getChannelFromNumber;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.swing.JOptionPane;

import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

/**
 * A simple, command-line program for setting the content of one display to one channel. This would be used by a scheduling system.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChangeOneDisplay {

	private ChangeOneDisplay() {
	}

	private static boolean	success	= false;
	private static boolean	verbose	= Boolean.getBoolean("ddisplay.onedisplay.verbose");

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Usage: java " + ChangeOneDisplay.class.getCanonicalName() + " <displayID> <channelNumber>");
			return;
		}

		credentialsSetup();

		selectorSetup();

		int targetDisplayNumber = Integer.parseInt(args[0]);
		Display display = getControllableDisplay(targetDisplayNumber);

		if (display == null) {
			System.err.println("There is no controllable display for displayID=" + targetDisplayNumber);
			return;
		}
		display.addListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("CHANGE_COMPLETED"))
					success = true;
				if (verbose)
					System.out.println(e);
			}
		});

		int targetChannelNumber = Integer.parseInt(args[1]);
		SignageContent content = getChannelFromNumber(targetChannelNumber);

		// Set this content to this display
		display.setContent(content);
		// TODO -- Determine if this was successful.

		if (verbose)
			System.out.println("Attempted to change display " + targetDisplayNumber + " [" + display + ", " + display.getLocation()
					+ "] to channel " + targetChannelNumber + " [" + content + "]");
		catchSleep(1000);
		if (verbose)
			if (success)
				System.out.println("It was successful!");
			else
				System.out.println("XXX   It FAILED!   XXX");

		System.exit(success ? 0 : -1);
	}

	private static Display getControllableDisplay(int dNum) {
		Display retval = null;
		Connection connection = null;
		try {
			connection = ConnectionToDatabase.getDbConnection();
			if ( connection == null ) return null;
		} catch (DatabaseNotVisibleException e1) {
			e1.printStackTrace();
			System.err.println("\nNo connection to the Signage/Displays database.");
			System.exit(-1);
			return null; // This is here for the compiler
		}

		synchronized (connection) {
			// Use ARM to simplify these try blocks.
			try (Statement stmt = connection.createStatement();) {
				try (ResultSet rs = stmt.executeQuery("USE " + DATABASE_NAME)) {
				}

				InetAddress ip = InetAddress.getLocalHost();
				String ipName = ip.getHostName().replace(".dhcp", "");
				String query = "SELECT LocationCode FROM SelectorLocation WHERE IPName='" + ipName + "'";
				try (ResultSet rs = stmt.executeQuery(query);) {
					if (rs.first())
						try { // Move to first returned row (there can be more than one; not sure how to deal with that yet)
							addLocationCode(rs.getInt("LocationCode"));
							if (verbose)
								System.out.println("Location code is " + getLocationCode());
							if (!MessageCarrier.initializeSignature()) {
								JOptionPane.showMessageDialog(null, "The private key file, for security, cannot be found.\n"
										+ "Click 'Dismiss' to close the GUI.\nThen get help to fix this problem.",
										"No Private Key", JOptionPane.ERROR_MESSAGE);
								System.exit(-1);
							}
							if (verbose) {
								System.out.println("Initialized our digital signature from '" + PRIVATE_KEY_LOCATION + "'.");
								System.out.println("\t Expect my client name to be '" + getFullSelectorName() + "'\n");
							}
							List<Display> g = getADisplay(getLocationCode(), dNum);
							retval = g.get(0);
							if (retval.getDBDisplayNumber() == dNum)
								return retval;

						} catch (Exception e) {
							e.printStackTrace();
						}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.err.println("Not sure why, but the display number " + dNum + " was not found.  Sorry.");
		return null;
	}
}
