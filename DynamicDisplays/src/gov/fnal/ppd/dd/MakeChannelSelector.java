/*
 * MakeChannelSelector
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.ChannelSelector.screenDimension;
import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameSelector;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.PRIVATE_KEY_LOCATION;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.THIS_IP_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.THIS_IP_NAME_INSTANCE;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.locationCode;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.util.SelectorInstructions;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Use the database to determine what sort of ChannelSelector to actually run
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class MakeChannelSelector {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		boolean missing = true;
		getMessagingServerNameSelector();
		String myIPName = "TBD";
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
			myIPName = ip.getHostName().replace(".dhcp", "");
			String query = "SELECT * FROM SelectorLocation where IPName='" + myIPName + "'";
			try (ResultSet rs2 = stmt.executeQuery(query);) {
				if (rs2.first())
					try { // Move to first returned row (there can be more than one; not sure how to deal with that yet)
						locationCode = rs2.getInt("LocationCode");
						System.out.println("Location code is " + locationCode);
						String myClassification = ConnectionToDynamicDisplaysDatabase.makeString(rs2.getAsciiStream("Type"));

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

						displayList = DisplayListFactory.getInstance(sType, locationCode);
						ChannelSelector channelSelector = new ChannelSelector();
						channelSelector.start();

						// TODO -- Create/fix mechanism for restarting a ChannelSelector

						JFrame f = new JFrame(myClassification + " " + locationCode);

						f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

						if (SHOW_IN_WINDOW) {
							f.setContentPane(channelSelector);
							f.pack();
						} else {
							// Full-screen display of this app!
							Box h = Box.createVerticalBox();
							h.add(channelSelector);
							SelectorInstructions label = new SelectorInstructions();
							label.start();
							h.add(label);
							channelSelector.setAlignmentX(JComponent.CENTER_ALIGNMENT);
							label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
							f.setUndecorated(true);
							f.setContentPane(h);
							f.setSize(screenDimension);
						}
						f.setVisible(true);
						missing = false;
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(-2);
					}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-3);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-4);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-5);
		}
		if (missing)
			JOptionPane.showMessageDialog(null, "This device, " + myIPName + ", is not listed in the Dynamics Display database; "
					+ "It cannot start an instance of ChannelSelector.", "Cannot Continue", JOptionPane.ERROR_MESSAGE);
	}
}
