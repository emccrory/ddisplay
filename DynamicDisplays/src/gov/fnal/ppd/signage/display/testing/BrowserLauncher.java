package gov.fnal.ppd.signage.display.testing;

import gov.fnal.ppd.signage.display.ScreenLayoutInterpreter;
import gov.fnal.ppd.signage.display.testing.BrowserLauncher.BrowserInstance;

import java.awt.Rectangle;
import java.io.IOException;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class BrowserLauncher {

	/**
	 * Do we want more information when this class is executed?
	 */
	public static boolean	debug			= false;

	private Process			browserProcess	= null;
	private Rectangle		bounds;

	private BrowserInstance	whichInstance;

	/**
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * @copy 2014
	 * 
	 */
	public enum BrowserInstance {
		/**
		 * SUse the Opera browser for the Dynamic Display
		 */
		OPERA,

		/**
		 * use the FireFox browser for the Dynamic Display
		 */
		FIREFOX
	};

	/**
	 * @param screenNumber
	 */
	public BrowserLauncher(final int screenNumber, final BrowserInstance i) {
		System.out.println("Launching browser " + i + " on screen #" + screenNumber);
		bounds = ScreenLayoutInterpreter.getBounds(screenNumber);
		whichInstance = i;
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
	public void changeURL(final String url) {
		switch (whichInstance) {
		case OPERA:
			try {
				if (browserProcess != null) {
					browserProcess.destroy();
					Thread.sleep(100);
					browserProcess = null;
					Thread.sleep(100); // Opera needs just over a full second to kill itself completely.
					if (debug)
						System.out.println("Killed last process...");
				}
				String geom = bounds.width + "x" + bounds.height + "+" + (int) bounds.getX() + "+" + (int) bounds.getY();
				browserProcess = new ProcessBuilder("opera", "-nosession", "-geometry", geom, "-fullscreen", url).start();
				if (debug)
					System.out.println("Launched " + whichInstance + " browser, geometry=" + geom);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
			break;

		case FIREFOX:
			if (browserProcess != null)
				return;

			try {
				String geom = bounds.width + "x" + bounds.height + "+" + (int) bounds.getX() + "+" + (int) bounds.getY();
				browserProcess = new ProcessBuilder("firefox", "-remote-control", "--geometry=" + geom, "-fullscreen", url).start();

				if (debug)
					System.out.println("Launched " + whichInstance + " browser, geometry=" + geom);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}

	}

	/**
	 * 
	 */
	public void exit() {
		if (browserProcess != null) {
			browserProcess.destroy();
		}
	}

}
