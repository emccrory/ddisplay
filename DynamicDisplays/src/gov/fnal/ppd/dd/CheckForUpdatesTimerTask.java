package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.getFlavor;
import static gov.fnal.ppd.dd.util.DownloadNewSoftwareVersion.failedOnce;
import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.util.Calendar;
import java.util.TimerTask;

import gov.fnal.ppd.dd.util.DownloadNewSoftwareVersion;
import gov.fnal.ppd.dd.util.ExitHandler;
import gov.fnal.ppd.dd.util.NotificationClient;
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
	private static boolean		workingOnAnUpdate	= false;
	private NotificationClient	client;

	public CheckForUpdatesTimerTask(NotificationClient client) {
		this.client = client;
	}

	@Override
	public void run() {
		try {
			if (failedOnce) {
				println(getClass(), "There was a problem updating the software before, so all bets are off until "
						+ "that problem is fixed and this application is restarted");
				return;
			}
			if (workingOnAnUpdate) {
				// This should never happen in a real installation as this update takes a few minutes, but these checks happen only
				// once per day. But there are test situations when the updates happen more frequently than normal, and this can
				// overlap with the next update check.
				println(getClass(), "Skipping update check because we are already working on one.");
				return;
			}
			workingOnAnUpdate = true;
			// Add a little complication: Skip the updates on weekends (Sat, Sun, and Mon mornings)
			Calendar date = Calendar.getInstance();
			int dow = date.get(Calendar.DAY_OF_WEEK);
			int hour = date.get(Calendar.HOUR_OF_DAY);
			if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY || (dow == Calendar.MONDAY && hour < 10)) {
				println(getClass(), "Skipping update check on weekends");
				workingOnAnUpdate = false;
				return;
			}
			// Let the Properties file change during the running of this daemon.
			FLAVOR flavor = getFlavor(true);
			println(getClass(), "Checking to see if there is a " + flavor + " update for the software");

			// 1. See if an update is available

			if (VersionInformationComparison.lookup(flavor, true)) {

				// 2. If so, download it.
				VersionInformation viWeb = VersionInformation.getDBVersionInformation(flavor);
				client.showNewUpdateInformation(flavor, viWeb.getVersionString());

				// -----------------------------------------------------------------------------------------------------------
				// Download and install the new code (This takes up to five minutes)

				DownloadNewSoftwareVersion d = new DownloadNewSoftwareVersion(viWeb.getVersionString());
				if (d.hasSucceeded()) {
					workingOnAnUpdate = false;
					client.success();
					// 3. Exit the entire process so we will restart.
					boolean isWindows = System.getProperty("os.name").toUpperCase().contains("WINDOWS");
					if (isWindows)
						ExitHandler.saveAndExit("Deployed new software version.", 99);
					else
						ExitHandler.saveAndExit("Deployed new software version.");
				} else {
					println(getClass(), "\n\n\nSomething went wrong with the update!!\n\n\n");
					client.failure();
				}
				// -----------------------------------------------------------------------------------------------------------

			} else {
				println(getClass(), "No new software is present.  Will check again soon.");
			}
		} catch (Throwable e) {
			if (e instanceof Exception) {
				printlnErr(getClass(), "\nException while trying to do a self-update. Will check for new software again soon "
						+ "(but it is pretty likely that this will happen again: Seek help!).\n");
				e.printStackTrace();
			} else {
				printlnErr(getClass(),
						"\nSERIOUS ERROR while trying to do a self-update!!  Will try to restart this application program.");
				e.printStackTrace();
				ExitHandler.saveAndExit("Runtime error during self-aware update");
			}
		}
		workingOnAnUpdate = false;
	}

}
