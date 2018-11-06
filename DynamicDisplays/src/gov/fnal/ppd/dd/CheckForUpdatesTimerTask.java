package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.getFlavor;
import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;

import gov.fnal.ppd.dd.util.DownloadNewSoftwareVersion;
import gov.fnal.ppd.dd.util.ExitHandler;
import gov.fnal.ppd.dd.util.version.VersionInformation;
import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;
import gov.fnal.ppd.dd.util.version.VersionInformationComparison;

/**
 * Handles the procedure of checking for an update. Also includes some visual clues for the user that may be watching
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class CheckForUpdatesTimerTask extends TimerTask {
	private JFrame	frame;

	public CheckForUpdatesTimerTask() {
	}

	@Override
	public void run() {
		try {
			// Add a little complication: Skip the updates on weekends (Sat, Sun and Mon mornings)
			Calendar date = Calendar.getInstance();
			int dow = date.get(Calendar.DAY_OF_WEEK);
			int hour = date.get(Calendar.HOUR_OF_DAY);
			if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY || (dow == Calendar.MONDAY && hour < 10) ) {
				println(getClass(), "Skipping update check on weekends");
				return;
			}
			// Let the Properties file change during the running of this daemon.
			FLAVOR flavor = getFlavor(true);
			println(getClass(), "Checking to see if there is a " + flavor + " update for the software");

			// 1. See if an update is available

			if (VersionInformationComparison.lookup(flavor, true)) {

				// 2. If so, download it. 
				VersionInformation viWeb = VersionInformation.getDBVersionInformation(flavor);
				showNewUpdateInformation(flavor, viWeb.getVersionString());

				// -----------------------------------------------------------------------------------------------------------
				// Download and install the new code (This takes up to five minutes)

				DownloadNewSoftwareVersion d = new DownloadNewSoftwareVersion(viWeb.getVersionString());
				if (d.hasSucceeded())
					// 3. Exit the entire process so we will restart.
					ExitHandler.saveAndExit("Deployed new software version.");
				else {
					println(getClass(), "\n\n\nSomething went wrong with the update!!\n\n\n");
					failure();
				}
				// -----------------------------------------------------------------------------------------------------------

			} else {
				println(getClass(), "No new software is present.  Will check again soon.");
			}
		} catch (Exception e) {
			printlnErr(getClass(), "\nException while trying to do a self-update. Will check for new software again soon.\n");
			e.printStackTrace();
		}
	}

	private void showNewUpdateInformation(FLAVOR flavor, String versionString) {
		// Isolate the GUI stuff here.
		String message1 = "There is a new " + flavor + " version of the software, Version " + versionString + ".";
		String message2 = "This version is newer than the code we are running.";
		String message3 = "Updating the software and then restarting this application program.";
		println(getClass(), message1 + message2 + "\n" + message3);

		frame = new JFrame("Dynamic Displays: New Software Update");
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		JLabel big = new JLabel(message1);
		big.setFont(new Font("Arial", Font.BOLD, 32));
		Box p = Box.createVerticalBox();
		p.add(big);
		big = new JLabel(message2);
		big.setFont(new Font("Arial", Font.BOLD, 32));
		p.add(Box.createRigidArea(new Dimension(80, 80)));
		p.add(big);

		big = new JLabel(message3);
		big.setFont(new Font("Arial", Font.ITALIC, 32));
		p.add(Box.createRigidArea(new Dimension(80, 80)));
		p.add(big);

		big = new JLabel("This will take about 5 minutes, " + new Date());
		big.setFont(new Font("Arial", Font.ITALIC, 18));
		p.add(Box.createRigidArea(new Dimension(80, 80)));
		p.add(big);

		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30),
				BorderFactory.createLineBorder(Color.red.darker(), 20)));

		frame.setContentPane(p);
		frame.pack();
		frame.setAlwaysOnTop(true);
		frame.setVisible(true);
	}

	private void failure() {
		frame.invalidate();
		frame.dispose();
		JLabel big = new JLabel("Something went wrong with the update - Call for help.");
		big.setFont(new Font("Arial", Font.ITALIC, 32));
		frame.setContentPane(big);
		frame.validate();
	}
}
