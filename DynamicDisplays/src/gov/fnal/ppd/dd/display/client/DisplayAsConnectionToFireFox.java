package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameDisplay;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.prepareUpdateWatcher;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Color;

/**
 * <p>
 * Implement a Dynamic Display using a back-door communication channel to FireFox (see https://github.com/pmorch/FF-remote-control)
 * </p>
 * 
 * <p>
 * Key relationships for this class:
 * <ul>
 * <li>DisplayControllerMessagingAbstract - the base class -- Handles all the startup and all the DisplayImpl methods. Only method
 * localSetContent() and endAllConnections() are implemented here.</li>
 * 
 * <li>ConnectionToBrowserInstance browserInstance (base class) -- Establishes and maintains the back-door connection to our FireFox
 * instance so we can tell it what web page to show</li>
 * 
 * <li>BrowserLauncher browserLauncher -- (In base class) Starts an instance of FireFox</li>
 * </ul>
 * </p>
 * 
 * <p>
 * (November, 2015) Removed all syncronizations to the Firefox attribute -- move this functionality into that class. This may be
 * causing a problem--I saw displays that were trying to run a list of channels get hosed after three or so hours of the (which
 * would be, in this case, about 10 iterations of the list)
 * </p>
 * 
 * <h2>Transition to Selenium</h2>
 * <p>
 * Over the years, the functionality of the system has been improved and extended quite a lot. Not (June 2018) that we are
 * transitioning away from the "pmorch" plugin to Selenium, a lot of that built-up functionality and reliability needs to be moved
 * to either this base class, or to the base class of the attribute browserInstance.
 * </p>
 * <p>
 * So, only the functionality specific to the Firefox browser and the pmorch plugin should remain here.
 * </p>
 * 
 * @deprecated
 * @author Elliott McCrory, Fermilab (2014-18)
 */
public class DisplayAsConnectionToFireFox extends DisplayControllerMessagingAbstract {

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
	public DisplayAsConnectionToFireFox(final String ipName, final int vNumber, final int dbNumber, final int screenNumber,
			final boolean showNumber, final String location, final Color color) {
		super(ipName, vNumber, dbNumber, screenNumber, showNumber, location, color);

		browserLauncher = new BrowserLauncherFirefox(screenNumber);
		contInitialization();
	}

	@Override
	public void initiate() {
		super.initiate();

		new Thread() {
			public void run() {
				println(DisplayAsConnectionToFireFox.class, ".initiate(): Here we go! display number=" + getVirtualDisplayNumber()
						+ " (" + getDBDisplayNumber() + ") " + (showNumber ? "Showing display num" : "Hiding display num"));

				browserInstance = new ConnectionToFirefoxInstance(screenNumber, getVirtualDisplayNumber(), getDBDisplayNumber(),
						highlightColor, showNumber);
				browserInstance.setBadNUC(badNUC);
				catchSleep(2000); // Wait a bit before trying to talk to this instance of Firefox.

				lastFullRestTime = System.currentTimeMillis();
				if (initializeSavedChannelObject())
					return;

				localSetContent();
			}
		}.start();
	}

	/**
	 * @param args
	 *            Expect one command line argument
	 */
	public static void main(final String[] args) {
		System.err.println("This code is not valid anymore.");
		System.exit(-1);
		
		prepareUpdateWatcher(false);

		credentialsSetup();

		getMessagingServerNameDisplay();

		try {
			@SuppressWarnings("unused")
			DisplayAsConnectionToFireFox add = (DisplayAsConnectionToFireFox) makeTheDisplays(DisplayAsConnectionToFireFox.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void errorInPageLoad(long value) {
		// TODO We should delete this class now		
	}

}
