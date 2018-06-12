package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Rectangle;

import gov.fnal.ppd.dd.display.ScreenLayoutInterpreter;

/**
 * Base class for communicating to the browser.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public abstract class BrowserLauncher {
	/**
	 * Do we want more information when this class is executed?
	 */
	public static boolean		debug			= false;

	protected Process			browserProcess	= null;
	protected Rectangle			bounds;

	protected int				screenNumber;

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
	 * @param url
	 */
	public void startBrowser(final String url) {
		changeURL(url);
	}

	/**
	 * @param url
	 *            The URL to show
	 */
	public abstract void changeURL(final String url);

	/**
	 * 
	 */
	public void exit() {
		if (browserProcess != null) {
			browserProcess.destroy();
		}
	}
}
