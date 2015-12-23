package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameDisplay;
import static gov.fnal.ppd.dd.GlobalVariables.DEFAULT_DWELL_TIME;
import static gov.fnal.ppd.dd.GlobalVariables.FORCE_REFRESH;
import static gov.fnal.ppd.dd.GlobalVariables.SELF_IDENTIFY;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.display.client.BrowserLauncher.BrowserInstance;
import gov.fnal.ppd.dd.signage.SignageContent;
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
 * <p>
 * (November, 2015) Removed all syncronizations to the firefox attribute -- move this functionality into that class. This may be
 * causing a problem--I saw displays that were trying to run a list of channels get hosed after three or so hours of the (which
 * would be, in this case, about 10 iterations of the list)
 * </p>
 * 
 * @author Elliott McCrory, Fermilab (2014-15)
 */
public class DisplayAsConnectionToFireFox extends DisplayControllerMessagingAbstract {

	private ConnectionToFirefoxInstance	firefox;
	private boolean						showingSelfIdentify	= false;
	// private WrapperType defaultWrapperType = WrapperType.valueOf(System.getProperty("ddisplay.wrappertype",
	// "NORMAL"));

	private int							changeCount;
	protected long						lastFullRestTime;
	protected long						revertTimeRemaining	= 0L;
	private Thread						revertThread		= null;

	private ListOfValidChannels			listOfValidURLs		= new ListOfValidChannels();
	private WrapperType					wrapperType;

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
			final boolean showNumber, final String location, final Color color, final SignageType type) {
		super(ipName, vNumber, dbNumber, screenNumber, showNumber, location, color, type);

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
				println(DisplayAsConnectionToFireFox.class, ".initiate(): Here we go! display number=" + getVirtualDisplayNumber()
						+ " (" + getDBDisplayNumber() + ") " + (showNumber ? "Showing display num" : "Hiding display num"));
				catchSleep(2000); // Wait a bit before trying to contact the instance of FireFox.
				firefox = new ConnectionToFirefoxInstance(screenNumber, getVirtualDisplayNumber(), getDBDisplayNumber(),
						highlightColor, showNumber);
				lastFullRestTime = System.currentTimeMillis();
				catchSleep(500); // Wait a bit more before trying to tell it to go to a specific page
				localSetContent();
			}
		}.start();
	}

	protected boolean localSetContent() {
		// FIXME This could be risky! But it is needed for URL arguments
		final String url = getContent().getURI().toASCIIString().replace("&amp;", "&");

		final int frameNumber = getContent().getFrameNumber();
		final long expiration = getContent().getExpiration();

		final long dwellTime = (getContent().getTime() == 0 ? DEFAULT_DWELL_TIME : getContent().getTime());
		println(getClass(), firefox.getInstance() + " Dwell time is " + dwellTime + ", expiration is " + expiration);

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
		} else if (url.equalsIgnoreCase(FORCE_REFRESH)) {
			try {

				// First, do a brute-force refresh to the browser
				firefox.forceRefresh(frameNumber);

				// Now tell the lower-level code that we need a full reset
				firefox.resetURL();

				// Finally, re-send the last URL we knew about
				if (!firefox.changeURL(lastChannel.getURI().toASCIIString(), wrapperType, frameNumber)) {
					println(getClass(), ".localSetContent():" + firefox.getInstance() + " Failed to set content");
					return false;
				}
			} catch (UnsupportedEncodingException e) {
				System.err.println(getClass().getSimpleName() + ":" + firefox.getInstance()
						+ " Somthing is wrong with this re-used URL: [" + lastChannel.getURI().toASCIIString() + "]");
				e.printStackTrace();
				return false;
			}
		} else
			try {
				// Someday we may need this: (getContent().getCode() & 1) != 0;
				changeCount++;
				if (expiration > 0) {
					revertTimeRemaining = expiration;
					if (revertToThisChannel == null) {
						revertToThisChannel = previousChannel;
					}
				} else {
					revertToThisChannel = null;
					revertTimeRemaining = 0;
				}
				if (firefox.changeURL(url, wrapperType, frameNumber)) {
					lastChannel = getContent();
					showingSelfIdentify = false;
				} else {
					println(getClass(), ".localSetContent():" + firefox.getInstance() + " Failed to set content");
					return false;
				}

				if (expiration <= 0)
					setResetThread(dwellTime, url, frameNumber);
				else
					setRevertThread();

				updateMyStatus();
			} catch (UnsupportedEncodingException e) {
				System.err.println(getClass().getSimpleName() + ":" + firefox.getInstance() + " Somthing is wrong with this URL: ["
						+ url + "]");
				e.printStackTrace();
				return false;
			}
		return true;
	}

	private void setResetThread(final long dwellTime, final String url, final int frameNumber) {

		if (dwellTime > 0) {
			final int thisChangeCount = changeCount;

			// The Use Case for the extra frame is to be able to show announcements. In that vein, it seems that
			// we want the frame to go away after a while.
			if (frameNumber != 0) {
				new Thread("TurnOffFrame" + getMessagingName()) {
					@Override
					public void run() {
						catchSleep(dwellTime);
						println(DisplayAsConnectionToFireFox.class, ".localSetContent():" + firefox.getInstance()
								+ " turning off the announcement frame");
						firefox.turnOffFrame(frameNumber);
					}
				}.start();
			} else {
				new Thread("RefreshContent" + getMessagingName()) {

					@Override
					public void run() {
						long increment = 15000L;
						while (true) {
							for (long t = dwellTime; t > 0 && changeCount == thisChangeCount; t -= increment) {
								catchSleep(Math.min(increment, t));
							}
							if (changeCount == thisChangeCount) {
								println(DisplayAsConnectionToFireFox.class, ".localSetContent():" + firefox.getInstance()
										+ " Reloading web page " + url);
								try {
									if (!firefox.changeURL(url, wrapperType, frameNumber)) {
										println(DisplayAsConnectionToFireFox.class, ".localSetContent(): Failed to REFRESH content");
										firefox.resetURL();
										continue; // TODO -- Figure out what to do here. For now, just try again later
									}
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
							} else {
								println(DisplayAsConnectionToFireFox.class, ".localSetContent():" + firefox.getInstance()
										+ " Not necessary to refresh " + url + " because the channel was changed.  Bye!");
								return;
							}
						}
					}
				}.start();
			}
		}
	}

	private void setRevertThread() {
		final String revertURL = revertToThisChannel.getURI().toString();

		if (revertThread != null) {
			// Do not create a new thread, just reset the counter in it.
			revertTimeRemaining = getContent().getExpiration();
		} else {
			revertThread = new Thread("RevertContent" + getMessagingName()) {

				/*
				 * 7/6/15 -------------------------------------------------------------------------------------------------------
				 * Something is getting messed up here, between a channel expiring and a channel that might need to be refreshed.
				 * Also in the mix, potentially, is when a channel change to a fixed image is requested and this launches a full
				 * refresh of the browser.
				 * 
				 * Ugh! not sure what is going on here.
				 * ----------------------------------------------------------------------------------------------------------------
				 * 
				 * Update 7/9/15: It looks like there are can be several of these threads run at once, and when they all expire,
				 * something bad seems to happen. New code added to only let one of these thread be created (and it can be "renewed"
				 * if necessary, before it expires).
				 */

				@Override
				public void run() {
					long increment = 15000L;
					while (true) {
						for (; revertTimeRemaining > 0; revertTimeRemaining -= increment) {
							if (revertToThisChannel == null) {
								println(DisplayAsConnectionToFireFox.class, ".setRevertThread():" + firefox.getInstance()
										+ " No longer necessary to revert.");
								revertThread = null;
								return;
							}
							catchSleep(Math.min(increment, revertTimeRemaining));
						}
						revertTimeRemaining = 0L;
						println(DisplayAsConnectionToFireFox.class, ".localSetContent():" + firefox.getInstance()
								+ " Reverting to web page " + revertURL);
						try {
							if (!firefox.changeURL(revertURL, wrapperType, 0)) {
								println(DisplayAsConnectionToFireFox.class, ".setRevertThread():" + firefox.getInstance()
										+ " Failed to REVERT content");
								firefox.resetURL();
								continue; // TODO -- Figure out what to do here. For now, just try again later
							}
							println(DisplayAsConnectionToFireFox.class, ".setRevertThread():" + firefox.getInstance()
									+ " Reverted to original web page.");
							return;
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
							revertThread = null;
							return;
						}
					}
				}
			};
			revertThread.start();
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
		return wrapperType != WrapperType.NONE;
	}

	protected boolean isVerifiedChannel(SignageContent c) {
		return listOfValidURLs.contains(c);
	}

	/**
	 * @param args
	 *            Expect one command line argument
	 */
	public static void main(final String[] args) {
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
	protected void setWrapperType(WrapperType wrapperType) {
		this.wrapperType = wrapperType;
	}

	@Override
	protected String getStatusString() {
		String retval = "";
		if (!firefox.isConnected()) {
			retval = "*NOT CONNECTED TO BROWSER* Last page " + getContent().getURI();
		} else if (getContent() == null)
			retval = "Off Line";
		else {
			retval = (getStatus() + " (" + getContent().getURI() + ")").replace("'", "\\'");
		}
		return retval;
	}
}
