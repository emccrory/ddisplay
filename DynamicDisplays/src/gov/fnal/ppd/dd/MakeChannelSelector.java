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
import static gov.fnal.ppd.dd.GlobalVariables.PRIVATE_KEY_LOCATION;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.THIS_IP_NAME_INSTANCE;
import static gov.fnal.ppd.dd.GlobalVariables.UNRECOVERABLE_ERROR;
import static gov.fnal.ppd.dd.GlobalVariables.addLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.getFullSelectorName;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationName;
import static gov.fnal.ppd.dd.GlobalVariables.prepareUpdateWatcher;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;

import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.db.ChannelsFromDatabase;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.display.ScreenLayoutInterpreter;
import gov.fnal.ppd.dd.interfaces.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.guiUtils.SplashScreens;
import gov.fnal.ppd.dd.util.nonguiUtils.CredentialsNotFoundException;
import gov.fnal.ppd.dd.util.nonguiUtils.JavaVersion;
import gov.fnal.ppd.dd.util.specific.ObjectSigning;
import gov.fnal.ppd.dd.util.specific.SelectorInstructions;

/**
 * Use the database to determine what sort of ChannelSelector to actually run, and then run it.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class MakeChannelSelector {

	/**
	 * Extension of java.swing.ProgressMonitor so that it can be checked if the user has cancelled the startup
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 *
	 */
	private static class MyProgressMonitor extends ProgressMonitor {
		public MyProgressMonitor(String string, int maxProgress) {
			super(null, string, "Starting", 0, maxProgress);
			setMillisToDecideToPopup(100);
			setMillisToPopup(100);
			setProgress(0);
		}

		public void setNote(String note) {
			if (isCanceled())
				System.exit(0);
			super.setNote(note);
		}

		public void setProgress(int p) {
			if (isCanceled())
				System.exit(0);
			super.setProgress(p);
		}
	}

	public static ProgressMonitor	progressMonitor;

	static boolean					theControllerIsProbablyInFront	= true;

	/**
	 * @param args
	 *            - no command-line arguments are used in this program; everything is communicated through the database, through
	 *            "-D" arguments to the JVM, or through the properties file
	 */
	public static void main(final String[] args) {
		println(MakeChannelSelector.class, "Running from java version " + JavaVersion.getInstance().get());

		// MessageConveyor.debug = true;

		prepareUpdateWatcher(false);
		ChannelsFromDatabase.setVerbose(true);

		SplashScreens.prepareSaverImages();

		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		selectorSetup();
		progressMonitor = new MyProgressMonitor("Building Channel Selector GUI for location=" + getLocationCode(),
				10 * (displayList.size() + 1));

		ChannelSelector channelSelector = new ChannelSelector(progressMonitor);
		channelSelector.setSelectorID(myDB_ID);
		channelSelector.start();
		// channelSelector.createRefreshActions();

		final JFrame f = new JFrame(getLocationName() + " (" + getLocationCode() + ")");
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

		progressMonitor.close();

		if (missing)
			JOptionPane
					.showMessageDialog(null,
							"This device, " + myIPName + ", is not listed in the Dynamic Displays database; "
									+ "It cannot start an instance of ChannelSelector.",
							"Cannot Continue", JOptionPane.ERROR_MESSAGE);

	}

	private static boolean	missing		= true;
	private static String	myIPName	= "TBD";
	private static int		myDB_ID		= 0;

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

				String query = "SELECT LocalID,DocentTab,LocationCode FROM SelectorLocation where IPName='" + myIPName
						+ "' AND Instance='" + THIS_IP_NAME_INSTANCE + "'";
				println(MakeChannelSelector.class, ": '" + query + "'");

				try (ResultSet rs2 = stmt.executeQuery(query);) {
					if (rs2.first())
						while (!rs2.isAfterLast())
							try { // Move to first returned row (there can be more than one; not sure how to deal with that yet)
								int lc = rs2.getInt("LocationCode");
								addLocationCode(lc);
								println(MakeChannelSelector.class, ":Location code is " + lc);

								SHOW_DOCENT_TAB = rs2.getBoolean("DocentTab");
								myDB_ID = rs2.getInt("LocalID");

								if (displayList == null)
									displayList = DisplayListFactory.getInstance(lc);
								else {
									List<Display> moreDisplays = DisplayListFactory.getInstance(lc);
									if (displayList.addAll(moreDisplays) == false) {
										System.err.println("Could not add the " + moreDisplays.size()
												+ " new display instances to displayList!?");
									}
								}
								missing = false;
								println(MakeChannelSelector.class, ": DisplayList has " + displayList.size() + " entries");
								if (!rs2.next())
									break;
							} catch (Exception e) {
								e.printStackTrace();
								System.exit(UNRECOVERABLE_ERROR);
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

			// if (!MessageCarrier.initializeSignature()) {
			if (!ObjectSigning.getInstance().loadPrivateKey(PRIVATE_KEY_LOCATION)) {
				JOptionPane.showMessageDialog(null,
						"The private key file, for security, cannot be found.\n"
								+ "Click 'Dismiss' to close the GUI.\nThen get help to fix this problem.",
						"No Private Key", JOptionPane.ERROR_MESSAGE);
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

		/* See GIT repository for what this code tried */
	}
}
