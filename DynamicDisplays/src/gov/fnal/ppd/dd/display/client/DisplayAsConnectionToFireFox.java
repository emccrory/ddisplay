package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameDisplay;
import static gov.fnal.ppd.dd.GlobalVariables.DEFAULT_DWELL_TIME;
import static gov.fnal.ppd.dd.GlobalVariables.SELF_IDENTIFY;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.display.client.BrowserLauncher.BrowserInstance;
import gov.fnal.ppd.dd.display.client.ConnectionToFirefoxInstance.WrapperType;
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
 * @author Elliott McCrory, Fermilab (2014-15)
 */
public class DisplayAsConnectionToFireFox extends DisplayControllerMessagingAbstract {

	private ConnectionToFirefoxInstance	firefox;
	private boolean						showingSelfIdentify	= false;
	private WrapperType					defaultWrapperType	= WrapperType.valueOf(System.getProperty("ddisplay.wrappertype",
																	"NORMAL"));

	private int							changeCount;
	protected long						lastFullRestTime;
	protected long						revertTimeRemaining	= 0L;
	private Thread						revertThread		= null;

	/**
	 * @param portNumber
	 * @param ipName
	 * @param vNumber
	 * @param dbNumber
	 * @param screenNumber
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayAsConnectionToFireFox(final String ipName, final int vNumber, final int dbNumber, final int screenNumber,
			final int portNumber, final String location, final Color color, final SignageType type) {
		super(ipName, vNumber, dbNumber, screenNumber, portNumber, location, color, type);

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
				println(DisplayAsConnectionToFireFox.class, ".initiate(): Here we go! display number=" + getVirtualDisplayNumber());
				catchSleep(2000); // Wait a bit before trying to contact the instance of FireFox.
				firefox = new ConnectionToFirefoxInstance(screenNumber, getVirtualDisplayNumber(), getDBDisplayNumber(),
						highlightColor);
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
		println(getClass(), ": Dwell time is " + dwellTime + ", expiration is " + expiration);

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
					if (expiration > 0) {
						revertTimeRemaining = expiration;
						if (revertToThisChannel == null) {
							revertToThisChannel = previousChannel;
						}
					} else {
						revertToThisChannel = null;
						revertTimeRemaining = 0;
					}
					if (firefox.changeURL(url, defaultWrapperType, frameNumber)) {
						lastChannel = getContent();
						showingSelfIdentify = false;
					} else {
						System.err.println(getClass().getSimpleName() + ".localSetContent(): Failed to set content");
						return false;
					}

					if (expiration <= 0)
						setResetThread(dwellTime, url, frameNumber);
					else
						setRevertThread();

					updateMyStatus();
				} catch (UnsupportedEncodingException e) {
					System.err.println(getClass().getSimpleName() + ": Somthing is wrong with this URL: [" + url + "]");
					e.printStackTrace();
					return false;
				}
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
						synchronized (firefox) {
							println(DisplayAsConnectionToFireFox.class, ".localSetContent(): turning off the announcement frame");
							firefox.turnOffFrame(frameNumber);
						}
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
								synchronized (firefox) {
									println(DisplayAsConnectionToFireFox.class, ".localSetContent(): Reloading web page " + url);
									try {
										if (!firefox.changeURL(url, defaultWrapperType, frameNumber)) {
											println(DisplayAsConnectionToFireFox.class,
													".localSetContent(): Failed to REFRESH content");
											firefox.resetURL();
											continue; // TODO -- Figure out what to do here. For now, just try again later
										}
									} catch (UnsupportedEncodingException e) {
										e.printStackTrace();
									}
								}
							} else {
								println(DisplayAsConnectionToFireFox.class, ".localSetContent(): Not necessary to refresh " + url
										+ " because the channel was changed.  Bye!");
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
				 * 
				 */

				@Override
				public void run() {
					long increment = 15000L;
					while (true) {
						for (; revertTimeRemaining > 0; revertTimeRemaining -= increment) {
							if (revertToThisChannel == null) {
								println(DisplayAsConnectionToFireFox.class, ".setRevertThread(): No longer necessary to revert.");
								revertThread = null;
								return;
							}
							catchSleep(Math.min(increment, revertTimeRemaining));
						}
						revertTimeRemaining = 0L;
						synchronized (firefox) {
							println(DisplayAsConnectionToFireFox.class, ".localSetContent(): Reverting to web page " + revertURL);
							try {
								if (!firefox.changeURL(revertURL, defaultWrapperType, 0)) {
									println(DisplayAsConnectionToFireFox.class, ".setRevertThread(): Failed to REVERT content");
									firefox.resetURL();
									continue; // TODO -- Figure out what to do here. For now, just try again later
								}
								println(DisplayAsConnectionToFireFox.class, ".setRevertThread(): Reverted to original web page.");
								return;
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
								revertThread = null;
								return;
							}
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
		return defaultWrapperType != WrapperType.NONE;
	}

	/**
	 * @param useWrapper
	 *            Do you want to use the standard URL wrapper page? Be very careful if you set this to false; not tested at this
	 *            time (12/2014)
	 */
	public void setUseWrapper(final WrapperType useWrapper) {
		this.defaultWrapperType = useWrapper;
	}

	/**
	 * @param args
	 *            Expect one command line argument
	 */
	public static void main(final String[] args) {
		getMessagingServerNameDisplay();

		try {
			@SuppressWarnings("unused")
			DisplayAsConnectionToFireFox add = (DisplayAsConnectionToFireFox) makeTheDisplays(DisplayAsConnectionToFireFox.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
