/*
 * BrowserLauncher
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.display.ScreenLayoutInterpreter;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Control the launching of an external browser as the Display implementation
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class BrowserLauncher {

	/**
	 * Do we want more information when this class is executed?
	 */
	public static boolean		debug			= true;
	private static final String	FIREFOX_USER	= "DynamicDisplay";

	private Process				browserProcess	= null;
	private Rectangle			bounds;

	private BrowserInstance		whichInstance;
	private int					screenNumber;

	/**
	 * What kind of browser are we to use?
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	public enum BrowserInstance {
		/**
		 * Use the Opera browser for the Dynamic Display
		 */
		OPERA,

		/**
		 * use the FireFox browser for the Dynamic Display
		 */
		FIREFOX
	};

	/**
	 * @param screenNumber
	 *            The physical screen number to display on. This is a little complicated. Internally, if the screen number is not
	 *            zero it causes firefox to be launched as user DynamicDisplay2 (for screen=1), listening on port number 32001.
	 * @param i
	 *            Which sort of Browser to use? Only Firefox is supported at this time.
	 */
	public BrowserLauncher(final int screenNumber, final BrowserInstance i) {
		println(getClass(), ": Launching browser " + i + " on screen #" + screenNumber);
		bounds = ScreenLayoutInterpreter.getBounds(screenNumber);
		whichInstance = i;
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
					println(getClass(), ": Launched " + whichInstance + " browser, geometry=" + geom);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
			break;

		case FIREFOX:
			String firefoxUser = FIREFOX_USER;
			if (screenNumber > 0) {
				firefoxUser += (screenNumber + 1);
			}
			if (browserProcess != null)
				return;

			String geom = bounds.width + "x" + bounds.height + "+" + (int) bounds.getX() + "+" + (int) bounds.getY();
			String moveTo = "window.moveTo(" + bounds.getX() + "," + bounds.getY() + ");";
			try {
				// extensions.remotecontrol.portNumber is the environment variable that gives the port number, I think (9/15/2014)
				// I tried to set it (and several variations) using System.setProperty(), to no avail.
				File file = new File(firefoxUser + ".log");
				ProcessBuilder pb = new ProcessBuilder("firefox", "-new-instance", "-P", firefoxUser, "-remote-control"); 
				//,"javascript:%20" + moveTo, url);
				pb.redirectErrorStream(true);
				pb.redirectOutput(file);
				List<String> c = pb.command();
				browserProcess = pb.start();

				println(getClass(), ": Firefox launch command: '");
				for (String S : c) {
					System.out.print(S + " ");
				}
				System.out.println("'");

				if (debug)
					println(getClass(), ": Launched " + whichInstance + " browser, geometry=" + geom);
			} catch (IOException e) {
				// This probably failed because it could not find the browser executable.
				e.printStackTrace();
				System.err.println("Assuming that this is Windows now.  We'll try again!");
				try {
					browserProcess = new ProcessBuilder("c:/Program Files (x86)/Mozilla Firefox/firefox.exe", "-remote-control",
							"-P " + firefoxUser, "--geometry=" + geom, "-new-instance", "-fullscreen", url).start();
					if (debug)
						System.out.println("Launched " + whichInstance + " browser, geometry=" + geom);
				} catch (IOException e1) {
					System.err.println("All options exhausted -- abort");
					e1.printStackTrace();
					System.exit(-1);
				}

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

	/**
	 * @param args
	 *            Expect at least two arguments
	 */
	public static void main(final String[] args) {
		if (args.length < 2) {
			System.err.println("Give me at least two URLs, please.");
			System.exit(-1);
		}
		BrowserLauncher x = new BrowserLauncher(0, BrowserInstance.FIREFOX);
		while (true)
			for (String U : args) {
				System.out.println("Changing URL to '" + U + "' ... ");
				x.changeURL(U);
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
	}
}
