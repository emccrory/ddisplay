package gov.fnal.ppd.dd.display.client.selenium;

import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameDisplay;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.prepareUpdateWatcher;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Color;

import gov.fnal.ppd.dd.display.client.DisplayControllerMessagingAbstract;
import gov.fnal.ppd.dd.util.ExitHandler;
import gov.fnal.ppd.dd.util.JavaVersion;
import gov.fnal.ppd.dd.util.SendEmailCommand;
import gov.fnal.ppd.dd.util.version.VersionInformation;

/**
 * Run a browser through the Selenium API
 * 
 * @author Elliott McCrory, Fermilab (2014-18)
 */
public class DisplayAsConnectionThroughSelenium extends DisplayControllerMessagingAbstract {

	/**
	 * @param showNumber
	 * @param ipName
	 * @param vNumber
	 * @param dbNumber
	 * @param screenNumber
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayAsConnectionThroughSelenium(final String ipName, final int vNumber, final int dbNumber, final int screenNumber,
			final boolean showNumber, final String location, final Color color) {
		super(ipName, vNumber, dbNumber, screenNumber, showNumber, location, color);

		browserLauncher = new BrowserLauncherSelenium(screenNumber);
		contInitialization();
	}

	@Override
	public void initiate() {
		// First connect to the browser, then connect to the messaging server.
		
		new Thread() {
			public void run() {
				println(DisplayAsConnectionThroughSelenium.class,
						".initiate(): Here we go! display number=" + getVirtualDisplayNumber() + " (" + getDBDisplayNumber() + ") "
								+ (showNumber ? "Showing display num" : "Hiding display num"));

				browserInstance = new SeleniumConnectionToBrowser(screenNumber, getVirtualDisplayNumber(), getDBDisplayNumber(),
						highlightColor, showNumber, DisplayAsConnectionThroughSelenium.this);
				catchSleep(2000); // Wait a bit before trying to talk to this instance of the browser.
				browserInstance.openConnection();
				catchSleep(1000); // Wait a bit longer

				lastFullRestTime = System.currentTimeMillis();
				if (initializeSavedChannelObject())
					return;

				localSetContent();
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

		credentialsSetup();

		getMessagingServerNameDisplay();

		try {
			System.out.println("Version: " + VersionInformation.getVersionInformation().getVersionString());
			
			@SuppressWarnings("unused")
			DisplayAsConnectionThroughSelenium add = (DisplayAsConnectionThroughSelenium) makeTheDisplays(
					DisplayAsConnectionThroughSelenium.class);
			ExitHandler.addFinalCommand(new SendEmailCommand());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
