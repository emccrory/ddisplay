/*
 * MakeChannelSelector
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.ChannelSelector.SHOW_DOCENT_TAB;
import static gov.fnal.ppd.dd.ChannelSelector.screenDimension;
import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameSelector;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.IS_DOCENT_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.PRIVATE_KEY_LOCATION;
import static gov.fnal.ppd.dd.GlobalVariables.RUN_RAISE_SELECTOR_BUTTON;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_EXTENDED_DISPLAY_NAMES;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.THIS_IP_NAME_INSTANCE;
import static gov.fnal.ppd.dd.GlobalVariables.WAIT_FOR_SERVER_TIME;
import static gov.fnal.ppd.dd.GlobalVariables.addLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.getFullSelectorName;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationName;
import static gov.fnal.ppd.dd.GlobalVariables.prepareSaverImages;
import static gov.fnal.ppd.dd.GlobalVariables.prepareUpdateWatcher;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.display.ScreenLayoutInterpreter;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.util.SelectorInstructions;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Use the database to determine what sort of ChannelSelector to actually run, and then run it.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class MakeChannelSelector {

	/*
	 * TODO -- Implement a way for this application to check (with the Mother Ship) to see if these is an updated version of
	 * the ZIP file available.
	 */
	static boolean	theControllerIsProbablyInFront	= true;

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		prepareUpdateWatcher(false);
		
		prepareSaverImages();

		credentialsSetup();

		selectorSetup();

		WAIT_FOR_SERVER_TIME = 2500L; // Be impatient when a ChannelSelector cannot reach the server

		ChannelSelector channelSelector = new ChannelSelector();
		channelSelector.start();
		// channelSelector.createRefreshActions();

		final JFrame f = new JFrame(myClassification + " " + getLocationName() + " (" + getLocationCode() + ")");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		if (SHOW_IN_WINDOW) {
			f.setContentPane(channelSelector);
			f.setSize(new Dimension(950, 710));
			theControllerIsProbablyInFront = true;
		} else {
			// Full-screen display of this app!
			JPanel h = new JPanel(new BorderLayout());
			h.add(channelSelector, BorderLayout.CENTER);
			SelectorInstructions label = new SelectorInstructions();
			label.start();
			h.add(label, BorderLayout.SOUTH);
			// Dimension dim = new Dimension(screenDimension.width, screenDimension.height-10);
			Dimension dim = screenDimension;
			h.setPreferredSize(dim);
			h.setMaximumSize(dim);
			channelSelector.setAlignmentX(JComponent.CENTER_ALIGNMENT);
			label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
			f.setUndecorated(true);
			f.setContentPane(h);
			f.pack();
		}

		// makeRaiseMeButton(f);

		// If there is no "screen number 1", this call will return the same rectangle as a call to getBounds(0).
		Rectangle bounds = ScreenLayoutInterpreter.getBounds(1);

		f.setVisible(true);
		f.setLocation(bounds.x, bounds.y);

		if (missing)
			JOptionPane.showMessageDialog(null, "This device, " + myIPName + ", is not listed in the Dynamics Display database; "
					+ "It cannot start an instance of ChannelSelector.", "Cannot Continue", JOptionPane.ERROR_MESSAGE);
	}

	private static String	myClassification	= "XOC";
	private static boolean	missing				= true;
	private static String	myIPName			= "TBD";

	/**
	 * Use this static method to initialize a program that is acting like a channel selector.
	 */
	public static void selectorSetup() {
		getMessagingServerNameSelector();
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

				InetAddress ip = InetAddress.getLocalHost();
				myIPName = ip.getCanonicalHostName().replace(".dhcp", "");

				String query = "SELECT * FROM SelectorLocation where IPName='" + myIPName + "' AND Instance='"
						+ THIS_IP_NAME_INSTANCE + "'";
				println(MakeChannelSelector.class, ": '" + query + "'");

				try (ResultSet rs2 = stmt.executeQuery(query);) {
					if (rs2.first())
						while (!rs2.isAfterLast())
							try { // Move to first returned row (there can be more than one; not sure how to deal with that yet)
								int lc = rs2.getInt("LocationCode");
								addLocationCode(lc);
								println(MakeChannelSelector.class, ":Location code is " + lc);

								SHOW_DOCENT_TAB = rs2.getBoolean("DocentTab");
								myClassification = rs2.getString("Type");
								// IS_PUBLIC_CONTROLLER = "Public".equals(myClassification) || "Experiment".equals(myClassification);
								// IS_DOCENT_CONTROLLER = "Experiment".equals(myClassification);
								if (IS_PUBLIC_CONTROLLER)
									SHOW_EXTENDED_DISPLAY_NAMES = true;

								final SignageType sType = SignageType.valueOf(myClassification);

								if (displayList == null)
									displayList = DisplayListFactory.getInstance(sType, lc);
								else {
									List<Display> moreDisplays = DisplayListFactory.getInstance(sType, lc);
									if (displayList.addAll(moreDisplays) == false) {
										System.err.println("Could not add the " + moreDisplays.size()
												+ " new display instances to displayList!?");
									}
								}
								println(MakeChannelSelector.class, ": DisplayList has " + displayList.size() + " entries");
								rs2.next();
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

			if (!MessageCarrier.initializeSignature()) {
				JOptionPane.showMessageDialog(null, "The private key file, for security, cannot be found.\n"
						+ "Click 'Dismiss' to close the GUI.\nThen get help to fix this problem.", "No Private Key",
						JOptionPane.ERROR_MESSAGE);
				System.exit(-1);
			}
			println(MakeChannelSelector.class, ": Initialized our digital signature from '" + PRIVATE_KEY_LOCATION + "'.");
			println(MakeChannelSelector.class, ": Expect my client name to be '" + getFullSelectorName() + "'\n");
		}
	}

	@SuppressWarnings("unused")
	static private void makeRaiseMeButton(final JFrame f) {

		/**
		 * This code was created in an attempt to have an always-visible button on the screens that have both a Display and a
		 * Selector. This did not work--this button could not get in front of the full-screen Firefox window. The TimerTask at the
		 * bottom had no effect on the visibility. If one were to select this special window from the window manager, it would bring
		 * it to the front and keep it there, but the WM dressings (the menu bars, etc.) would become visible.
		 */

		if (SHOW_IN_WINDOW && RUN_RAISE_SELECTOR_BUTTON) {
			final String RAISE_ME = "Raise Dynamic Display Controller";
			final String LOWER_ME = "Hide Dynamic Displays Controller";
			String s = (theControllerIsProbablyInFront ? LOWER_ME : RAISE_ME);
			final JFrame ff = new JFrame(s);
			final JButton show = new JButton(s);
			show.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(),
							BorderFactory.createRaisedBevelBorder()), show.getBorder()));
			show.setFont(show.getFont().deriveFont(10.0F));
			show.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// Raise/lower the controller that is being made here
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							if (theControllerIsProbablyInFront) {
								f.toBack();
								show.setText(RAISE_ME);
							} else {
								f.toFront();
								show.setText(LOWER_ME);
							}
							f.repaint();
							theControllerIsProbablyInFront = !theControllerIsProbablyInFront;
						}
					});
				}
			});

			ff.setAlwaysOnTop(true);
			ff.setUndecorated(true);
			ff.setContentPane(show); // May need to add some border
			ff.setLocation(210, 5); // What is the right position for this?
			ff.pack();
			ff.setVisible(true);

			// A kludge: Make sure this button is always on top.
			Timer timer = new Timer();

			TimerTask tt = new TimerTask() {
				public void run() {
					ff.toFront();
					ff.repaint();
				}
			};
			timer.scheduleAtFixedRate(tt, 2 * ONE_MINUTE, ONE_MINUTE);
		}
	}
}
