package gov.fnal.ppd.dd;

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
import gov.fnal.ppd.dd.util.version.VersionInformationComparison;
import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;

/**
 * Handles the procedure of checking for an update. Also includes some visual clues for the user that may be watching
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class CheckForUpdatesTimerTask extends TimerTask {
	private JFrame	frame;
	private FLAVOR	flavor;

	public CheckForUpdatesTimerTask(FLAVOR f) {
		this.flavor = f;
	}

	@Override
	public void run() {
		try {
			// Add a little complication: Skip the updates on weekends (Sat, Sun and Mon mornings)
			Calendar date = Calendar.getInstance();
			int dow = date.get(Calendar.DAY_OF_WEEK);
			if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY || dow == Calendar.MONDAY) {
				println(getClass(), "Skipping update check on weekend days");
				return;
			}
			println(getClass(), "Checking to see if there is a " + flavor + " update for the software");

			// 1. See if an update is available
			double days = VersionInformationComparison.lookup(flavor, false);

			if (days > 0) {

				// 2. If so, download it. Then exit the entire process so we will restart.
				VersionInformation viWeb = VersionInformation.getDBVersionInformation(flavor);
				showNewUpdateInformation(flavor, viWeb.getVersionString(), days);

				// -----------------------------------------------------------------------------------------------------------
				// Download and install the new code (This takes up to five minutes)

				DownloadNewSoftwareVersion d = new DownloadNewSoftwareVersion(viWeb.getVersionString());
				if (d.hasSucceeded())
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
			printlnErr(getClass(), "\nException while trying to do a self-update. Will check again soon.\n");
			e.printStackTrace();
		}
	}

	private void showNewUpdateInformation(FLAVOR flavor, String versionString, double days) {
		// Isolate the GUI stuff here.

		String timeString = String.format("%.1f days", days);
		if (days < 0.1)
			timeString = "a few minutes";
		else if (days < 0.5)
			timeString = "a few hours";
		else if (days < 3)
			timeString = String.format("%.1f hours", (days * 24.0));
		else if (days > 27)
			timeString = String.format("%.1f weeks", (days / 7.0));
		else if (days > 420)
			timeString = String.format("about %.2f years" + (days / 365.0));

		String message1 = "There is a " + flavor + " version of the software, Version " + versionString + ".";
		String message2 = "This version is " + timeString + " newer than the code we are running.";
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
