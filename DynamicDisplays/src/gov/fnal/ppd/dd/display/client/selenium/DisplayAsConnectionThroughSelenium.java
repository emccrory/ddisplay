package gov.fnal.ppd.dd.display.client.selenium;

import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameDisplay;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.prepareUpdateWatcher;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;

import java.awt.Color;

import gov.fnal.ppd.dd.CredentialsNotFoundException;
import gov.fnal.ppd.dd.display.client.DisplayControllerMessagingAbstract;
import gov.fnal.ppd.dd.util.nonguiUtils.JavaVersion;
import gov.fnal.ppd.dd.util.version.VersionInformation;

/**
 * Run a browser through the Selenium API
 * 
 * @author Elliott McCrory, Fermilab (2014-18)
 */
public class DisplayAsConnectionThroughSelenium extends DisplayControllerMessagingAbstract {

	/**
	 * @param ipName
	 *            - The Internet name for this computer
	 * @param vNumber
	 *            - the virtual display number
	 * @param dbNumber
	 *            - the database display number
	 * @param screenNumber
	 *            - The screen numbers (1/2021 - this is always 0)
	 * @param showNumber
	 *            - Do we want to show the number on this display?
	 * @param location
	 *            - The name of the location of this display
	 * @param color
	 *            - The proper background color for this display
	 */
	public DisplayAsConnectionThroughSelenium(final String ipName, final int vNumber, final int dbNumber, final int screenNumber,
			final boolean showNumber, final String location, final Color color) {
		super(ipName, vNumber, dbNumber, screenNumber, showNumber, location, color);
	}

	@Override
	public void initiate() {
		browserLauncher = new BrowserLauncherSelenium(screenNumber);
		contInitialization();

		// First connect to the browser, then connect to the messaging server.

		new Thread() {
			public void run() {
				browserInstance = null;
				int failCount = 0;
				while (browserInstance == null) {
					println(DisplayAsConnectionThroughSelenium.class,
							".initiate(): Here we go! display number=" + getVirtualDisplayNumber() + " (" + getDBDisplayNumber()
									+ ") " + ", screen number " + screenNumber + " "
									+ (showNumber ? "Showing display num" : "Hiding display num"));

					browserInstance = new SeleniumConnectionToBrowser(screenNumber, getVirtualDisplayNumber(), getDBDisplayNumber(),
							highlightColor, showNumber, DisplayAsConnectionThroughSelenium.this);
					catchSleep(2000); // Wait a bit before trying to talk to this instance of the browser.
					browserInstance.openConnection();
					catchSleep(1000); // Wait a bit longer

					if (!browserInstance.isConnected()) {
						// Well, that failed. Let's try again
						failCount++;
						browserInstance = null;
						printlnErr(DisplayAsConnectionThroughSelenium.class,
								failCount + " - Failed to connect to browser, will try again in a few seconds.");
						catchSleep(5000);
						continue;
					}
					browserInstance.addErrorListener(DisplayAsConnectionThroughSelenium.this);
					browserInstance.addErrorListener(new AlertThatPageIsNotShowing(DisplayAsConnectionThroughSelenium.this));
					lastFullRestTime = System.currentTimeMillis();
					if (initializeSavedChannelObject())
						return;

					localSetContent();
				}
			}
		}.start();

		super.initiate();
	}

	/**
	 * @param args
	 *            Expect no command line arguments
	 */
	public static void main(final String[] args) {
		println(DisplayAsConnectionThroughSelenium.class, "Running from java version " + JavaVersion.getCurrentVersion());

		prepareUpdateWatcher(false);

		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		getMessagingServerNameDisplay();

		try {
			System.out.println("Version: " + VersionInformation.getVersionInformation().getVersionString());

			@SuppressWarnings("unused")
			DisplayAsConnectionThroughSelenium add = (DisplayAsConnectionThroughSelenium) makeTheDisplays(
					DisplayAsConnectionThroughSelenium.class);
			// ExitHandler.addFinalCommand(new SendEmailCommand());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
