package gov.fnal.ppd.signage.display.testing;

import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.display.testing.BrowserLauncher.BrowserInstance;

import java.awt.Color;

/**
 * <p>
 * Implement a Dynamic Display using a back-door communication channel to FireFox (see https://github.com/pmorch/FF-remote-control)
 * </p>
 * 
 * <p>
 * Key relationships for this class:
 * <ul>
 * <li>DisplayControllerMessagingAbstract (base class) -- Handles all the startup and all the DisplayImpl methods. Only method
 * localSetContent() and endAllConnections() are implemented here.</li>
 * 
 * <li>ConnectionToFirefoxInstance firefox -- Establishes and maintains the back-door connection to our FireFox instance so we can
 * tell it what web page to show</li>
 * 
 * <li>BrowserLauncher browserLauncher -- (In base class) Starts an instance of FireFox</li>
 * </ul>
 * </p>
 * 
 * @author Elliott McCrory, Fermilab (2014)
 */
public class DisplayAsConnectionToFireFox extends DisplayControllerMessagingAbstract {

	private ConnectionToFirefoxInstance	firefox;

	/**
	 * @param portNumber
	 * @param ipName
	 * @param displayID
	 * @param screenNumber
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayAsConnectionToFireFox(String ipName, int displayID, int screenNumber, int portNumber, String location,
			Color color, SignageType type) {
		super(ipName, displayID, screenNumber, portNumber, location, color, type);

		if (getContent() == null)
			throw new IllegalArgumentException("No content defined!");

		browserLauncher = new BrowserLauncher(screenNumber, BrowserInstance.FIREFOX);
		browserLauncher.startBrowser(getContent().getURI().toASCIIString());

		new Thread() {
			public void run() {
				try {
					sleep(2000); // Wait a bit before trying to contact the instance of FireFox.
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				firefox = new ConnectionToFirefoxInstance();
				firefox.changeURL(getContent().getURI().toASCIIString());
			}
		}.start();

		contInitialization(portNumber);
	}

	protected void localSetContent() {
		String url = getContent().getURI().toASCIIString().replace("&amp;", "&"); // FIXME This could be risky! But it is needed for
																					// URL arguments

		synchronized (firefox) {
			if (url.equalsIgnoreCase(SELF_IDENTIFY)) {
				firefox.changeURL(IDENTIFY_URL + number);
				new Thread("Identify_" + displayNumber + "_wait") {
					public void run() {
						try {
							sleep(SHOW_SPLASH_SCREEN_TIME);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						firefox.changeURL(lastURL);
					}
				}.start();
			} else {
				firefox.changeURL(url);
				lastURL = url;
				new Thread("UpdateStatus" + getMessagingName()) {
					public void run() {
						try {
							sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						updateMyStatus();						
					}
				}.start();
			}
		}
	}

	@Override
	protected void endAllConnections() {
		firefox.exit();
		browserLauncher.exit();
	}

	/**
	 * @param args
	 *            Expect one command line argument
	 */
	public static void main(final String[] args) {
		@SuppressWarnings("unused")
		DisplayAsConnectionToFireFox add;

		if (args.length == 0) {
			System.err.println("Usage: " + DisplayAsConnectionToFireFox.class.getCanonicalName() + " -display=<displayIDNumber>");
			System.err.println("       -- or --");
			System.err.println("       " + DisplayAsConnectionToFireFox.class.getCanonicalName() + " -screen=<displayNumber>");
			System.exit(-1);
		}

		if (args.length == 1) {
			if (args[0].startsWith("-display=")) {
				int num = Integer.parseInt(args[0].substring("-display=".length()));
				try {
					add = (DisplayAsConnectionToFireFox) makeDynamicDisplayByNumber(num, DisplayAsConnectionToFireFox.class);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (args[0].startsWith("-screen=")) {
				int screen = Integer.parseInt(args[0].substring("-screen=".length()));
				try {
					add = (DisplayAsConnectionToFireFox) makeDynamicDisplayByScreen(screen, DisplayAsConnectionToFireFox.class);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
