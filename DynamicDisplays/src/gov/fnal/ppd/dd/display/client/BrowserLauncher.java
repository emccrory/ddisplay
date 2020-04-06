package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.util.GeneralUtilities.println;

import java.awt.Rectangle;

import gov.fnal.ppd.dd.display.ScreenLayoutInterpreter;

/**
 * Base class for communicating to the browser.
 * 
 * It kinda seems that this class is not needed - these functionalities can be moved to ConnectionToBrowserInstance. It does provide
 * a slightly cleaner system for the old "pmorch" plugin version.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public abstract class BrowserLauncher {
	/**
	 * Do we want more information when this class is executed?
	 */
	public static boolean	debug	= false;
	protected Rectangle		bounds;
	protected int			screenNumber;

	/**
	 * @param screenNumber
	 *            The physical screen number to display on. This is a little complicated. Internally, if the screen number is not
	 */
	public BrowserLauncher(final int screenNumber) {
		bounds = ScreenLayoutInterpreter.getBounds(screenNumber);
		if (screenNumber > 0) {
			println(BrowserLauncher.class, "There are multiple instances of displays being driven here.  Turning on debugging.");
			debug = true;
		}
		this.screenNumber = screenNumber;
	}

	/**
	 * Do whatever initialization is needed to communicate with the browser
	 */
	public abstract void startBrowser();

	/**
	 * 
	 */
	public abstract void exit();
}
