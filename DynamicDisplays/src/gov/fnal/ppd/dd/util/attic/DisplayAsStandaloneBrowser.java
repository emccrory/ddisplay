package gov.fnal.ppd.dd.util.attic;

import gov.fnal.ppd.dd.display.client.BrowserLauncher;
import gov.fnal.ppd.dd.display.client.BrowserLauncher.BrowserInstance;
import gov.fnal.ppd.dd.signage.SignageType;

import java.awt.Color;

/**
 * Implement a Dynamic Display using an underlying browser
 * 
 * @author Elliott McCrory, Fermilab (2014)
 * @deprecated
 */
public class DisplayAsStandaloneBrowser extends DisplayControllerAbstract {

	/**
	 * @param portNumber
	 * @param ipName
	 * @param displayID
	 * @param screenNumber
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayAsStandaloneBrowser(String ipName, int displayID, int screenNumber, int portNumber, String location, Color color,
			SignageType type) {
		super(ipName, displayID, screenNumber, portNumber, location, color, type);

		browserLauncher = new BrowserLauncher(screenNumber, BrowserInstance.OPERA);
		browserLauncher.changeURL(getContent().getURI().toString());
		contInitialization(portNumber);
	}

	protected boolean localSetContent() {
		String url = getContent().getURI().toASCIIString();

		synchronized (browserLauncher) {
			if (url.equalsIgnoreCase(SELF_IDENTIFY)) {
				browserLauncher.changeURL(IDENTIFY_URL + number);
				
// removed this block so there are no compile errors
//				new Thread("Identify_" + displayNumber + "_wait") {
//					public void run() {
//						try {
//							sleep(SHOW_SPLASH_SCREEN_TIME);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//						browserLauncher.changeURL(lastURL);
//					}
//				}.start();
				
			} else {
				browserLauncher.changeURL(url);
				lastURL = url;
			}
		}
		return true;
	}

	@Override
	protected void endAllConnections() {
		browserLauncher.exit();
	}

	/**
	 * @param args Expect one command line argument
	 */
	public static void main(final String[] args) {
		/*
		 * TODO --- Certificates
		 * 
		 * it is possible to use SSL certificate. While there is no direct way, but you can copy the profile files (cert8.db,
		 * key3.db and cert_override.txt) from the firefox profile (with SSL certificate) folder into
		 * "jxbrowser firefox profile folder" . Then JxBrowser has the certificates and use them.
		 */

		@SuppressWarnings("unused")
		DisplayAsStandaloneBrowser add;

		if (args.length == 0) {
			System.err.println("Usage: " + DisplayAsStandaloneBrowser.class.getCanonicalName() + " -display=<displayIDNumber>");
			System.err.println("       -- or --");
			System.err.println("       " + DisplayAsStandaloneBrowser.class.getCanonicalName() + " -screen=<displayNumber>");
			System.exit(-1);
		}

		if (args.length == 1) {
			if (args[0].startsWith("-display=")) {
				int num = Integer.parseInt(args[0].substring("-display=".length()));
				try {
					add = (DisplayAsStandaloneBrowser) makeDynamicDisplayByNumber(num, DisplayAsStandaloneBrowser.class);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (args[0].startsWith("-screen=")) {
				int screen = Integer.parseInt(args[0].substring("-screen=".length()));
				try {
					add = (DisplayAsStandaloneBrowser) makeDynamicDisplayByScreen(screen, DisplayAsStandaloneBrowser.class);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	}

	@Override
	public String getMessagingName() {
		// TODO Auto-generated method stub
		return null;
	}

}
