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
import javax.swing.JComponent;
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
	private JFrame frame;

	public CheckForUpdatesTimerTask() {
	}

	@Override
	public void run() {
		try {
			// Add a little complication: Skip the updates on weekends (Sat, Sun and Mon mornings)
			Calendar date = Calendar.getInstance();
			int dow = date.get(Calendar.DAY_OF_WEEK);
			int hour = date.get(Calendar.HOUR_OF_DAY);
			if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY || (dow == Calendar.MONDAY && hour < 10)) {
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

	private static class LocalJLabel extends JLabel {
		private static final long serialVersionUID = 4744851697649452477L;

		public LocalJLabel(String title, int style, int size) {
			super(title);
			setAlignmentX(JComponent.CENTER_ALIGNMENT);
			setFont(new Font("Arial", style, size));
		}
	}

	private void showNewUpdateInformation(FLAVOR flavor, String versionString) {
		// Isolate the GUI stuff here.
		String message0 = "Dynamic Displays Software Update in progress";
		String message1 = "There is a new " + flavor + " version, " + versionString + ", of the Dynamic Displays software.";
		String message3 = "After the software has been updated, the application program will be restarted.";
		println(getClass(), message0 + message1 + "\n" + message3);

		Dimension bigGap = new Dimension(60,60);
		
		frame = new JFrame("Dynamic Displays: New Software Update");
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		Box p = Box.createVerticalBox();
		JLabel big = new LocalJLabel(message0, Font.BOLD, 46);
		p.add(big);

		big = new LocalJLabel(message1, Font.PLAIN, 32);
		p.add(Box.createRigidArea(bigGap));
		p.add(big);

		big = new LocalJLabel(message3, Font.ITALIC, 32);
		p.add(Box.createRigidArea(bigGap));
		p.add(big);

		big = new LocalJLabel("This will take about 5 minutes, " + new Date(), Font.ITALIC, 18);
		p.add(Box.createRigidArea(bigGap));
		p.add(big);

		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30),
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.red.darker(), 20),
						BorderFactory.createEmptyBorder(10, 10, 10, 10))));

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
