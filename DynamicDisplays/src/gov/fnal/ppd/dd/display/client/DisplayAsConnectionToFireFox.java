package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DEFAULT_DWELL_TIME;
import static gov.fnal.ppd.dd.GlobalVariables.SELF_IDENTIFY;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.display.client.BrowserLauncher.BrowserInstance;
import gov.fnal.ppd.dd.signage.SignageType;

import java.awt.Color;
import java.io.UnsupportedEncodingException;

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
	private boolean						showingSelfIdentify	= false;
	private boolean						useWrapper			= true;
	private int							changeCount;

	/**
	 * @param portNumber
	 * @param ipName
	 * @param displayID
	 * @param screenNumber
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayAsConnectionToFireFox(final String ipName, final int displayID, final int screenNumber, final int portNumber,
			final String location, final Color color, final SignageType type) {
		super(ipName, displayID, screenNumber, portNumber, location, color, type);

		if (getContent() == null)
			throw new IllegalArgumentException("No content defined!");

		browserLauncher = new BrowserLauncher(screenNumber, BrowserInstance.FIREFOX);
		browserLauncher.startBrowser(getContent().getURI().toASCIIString());

		contInitialization();
	}

	@Override
	public void initiate() {
		super.initiate();

		new Thread() {
			public void run() {
				println(DisplayAsConnectionToFireFox.class, ".initiate(): Here we go! display number=" + getNumber());
				catchSleep(4000); // Wait a bit before trying to contact the instance of FireFox.
				firefox = new ConnectionToFirefoxInstance(screenNumber, getNumber(), highlightColor);
				catchSleep(500); // Wait a bit more before trying to tell it to go to a specific page
				try {
					String url = getContent().getURI().toASCIIString();
					firefox.changeURL(url, true);
					setResetThread(DEFAULT_DWELL_TIME, url);
					updateMyStatus();
				} catch (UnsupportedEncodingException e) {
					System.err.println(DisplayAsConnectionToFireFox.class.getSimpleName() + ": Somthing is wrong with this URL: ["
							+ getContent().getURI().toASCIIString() + "]");
					e.printStackTrace();
				}
			}
		}.start();
	}

	protected void localSetContent() {
		final String url = getContent().getURI().toASCIIString().replace("&amp;", "&"); // FIXME This could be risky! But it is
																						// needed for URL arguments

		final long dwellTime = (getContent().getTime() == 0 ? DEFAULT_DWELL_TIME : getContent().getTime());
		println(getClass(), ": Dwell time is " + dwellTime);

		synchronized (firefox) {
			if (url.equalsIgnoreCase(SELF_IDENTIFY)) {
				if (!showingSelfIdentify) {
					showingSelfIdentify = true;
					firefox.showIdentity();
					new Thread() {
						public void run() {
							try {
								sleep(SHOW_SPLASH_SCREEN_TIME);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							firefox.removeIdentify();
							showingSelfIdentify = false;
							setContentBypass(lastChannel);
							updateMyStatus();
						}
					}.start();
					// }
				}
			} else
				try {
					// Someday we may need this: (getContent().getCode() & 1) != 0;
					changeCount++;
					if (firefox.changeURL(url, useWrapper)) {
						lastChannel = getContent();
						showingSelfIdentify = false;
					} else {
						System.err.println(getClass().getSimpleName() + ".localSetContent(): Failed to set content");
					}

					setResetThread(dwellTime, url);
					updateMyStatus();
				} catch (UnsupportedEncodingException e) {
					System.err.println(getClass().getSimpleName() + ": Somthing is wrong with this URL: [" + url + "]");
					e.printStackTrace();

				}
		}
	}

	private void setResetThread(final long dwellTime, final String url) {
		if (dwellTime > 0) {
			final int thisChangeCount = changeCount;
			new Thread("RefreshContent" + getMessagingName()) {

				@Override
				public void run() {
					while (true) {
						catchSleep(dwellTime);
						synchronized (firefox) {
							if (changeCount == thisChangeCount) {
								println(DisplayAsConnectionToFireFox.class, ".localSetContent(): Reloading web page "
										+ url);
								try {
									if (!firefox.changeURL(url, useWrapper)) {
										println(DisplayAsConnectionToFireFox.class,
												".localSetContent().refreshThread: Failed to REFRESH content");
										return; // All bets are off!!
									}
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
							} else
								return;
						}
					}
				}
			}.start();
		}		
	}

	@Override
	protected void endAllConnections() {
		if (firefox != null)
			firefox.exit();
		if (browserLauncher != null)
			browserLauncher.exit();
	}

	@Override
	public void disconnect() {
		endAllConnections();
	}

	/**
	 * @return will the next URL request go through the normal wrapper page?
	 */
	public boolean isUsingWrapper() {
		return useWrapper;
	}

	/**
	 * @param useWrapper
	 *            Do you want to use the standard URL wrapper page? Be very careful if you set this to false; not tested at this
	 *            time (12/2014)
	 */
	public void setUseWrapper(final boolean useWrapper) {
		this.useWrapper = useWrapper;
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
