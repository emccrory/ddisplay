package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameDisplay;
import static gov.fnal.ppd.dd.GlobalVariables.DEFAULT_DWELL_TIME;
import static gov.fnal.ppd.dd.GlobalVariables.FORCE_REFRESH;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.SELF_IDENTIFY;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.prepareUpdateWatcher;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.emergency.EmergencyMessage;
import gov.fnal.ppd.dd.emergency.Severity;
import gov.fnal.ppd.dd.signage.EmergencyCommunication;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;

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
 * @author Elliott McCrory, Fermilab (2014-18)
 */
public class DisplayAsConnectionToFireFox extends DisplayControllerMessagingAbstract {

	private static final long	FRAME_DISAPPEAR_TIME		= 2 * ONE_MINUTE;

	private int					changeCount;
	private boolean				skipRevert					= false;
	private boolean				showingSelfIdentify			= false;
	private long				remainingTimeRemEmergMess	= 0l;

	private boolean[]			removeFrame					= { false, false, false, false, false };
	private long[]				frameRemovalTime			= { 0L, 0L, 0L, 0L, 0L };
	private Thread[]			frameRemovalThread			= { null, null, null, null, null };

	private Thread				revertThread				= null;
	private Thread				emergencyRemoveThread		= null;
	private ListOfValidChannels	listOfValidURLs				= new ListOfValidChannels();
	private WrapperType			wrapperType;
	private ThreadWithStop		playlistThread				= null;
	private EmergencyMessage	em;

	protected boolean			newListIsPlaying			= false;
	protected SignageContent	previousPreviousChannel		= null;
	protected long				lastFullRestTime;
	protected long				revertTimeRemaining			= 0L;

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

	

	protected boolean localSetContent() {
		if (playlistThread != null) {
			playlistThread.stopMe = true;
			playlistThread = null;
		}

		if (getContent() instanceof EmergencyCommunication) {
			// Emergency communication!!!!
			em = ((EmergencyCommunication) getContent()).getMessage();
			boolean retval = browserInstance.showEmergencyCommunication(em);
			showingEmergencyMessage = true;
			if (em.getSeverity() == Severity.REMOVE) {
				setContent(previousChannel);
				showingEmergencyMessage = false;
			}

			// Remove this message after a while
			remainingTimeRemEmergMess = em.getDwellTime();
			if (emergencyRemoveThread == null || !emergencyRemoveThread.isAlive()) {
				emergencyRemoveThread = new Thread("RemoveEmergencyCommunication") {
					long interval = 2000L;

					public void run() {
						for (; remainingTimeRemEmergMess > 0; remainingTimeRemEmergMess -= interval) {
							catchSleep(Math.min(interval, remainingTimeRemEmergMess));
						}
						setContent(previousChannel);
						showingEmergencyMessage = false;
						emergencyRemoveThread = null;
					}
				};
				emergencyRemoveThread.start();
			}
			return retval;

		} else if (getContent() instanceof ChannelPlayList) {

			/**
			 * FIXME -- There might be a way to implement this that does not require this "instanceof" check.
			 * 
			 * The basic idea is that when a channel is accessed, it would be given the opportunity to adjust itself. This is
			 * obviously useful for a list of channels, but would/could this be useful for a regular channel? I'm not sure at this
			 * time if this can be completely hidden in the current signature of a SignageContent, or if we would have to change the
			 * signature. e.g., signageContent.finished(), or signageContent.prepare() (or something equivalent). As you see here,
			 * the idea is to notice that this is a play list and then set up a thread specifically to advance the channel. There is
			 * already a thread below to refresh the channel when the dwell time is completed--maybe we just use that thread, as
			 * suggested here.
			 * 
			 */

			playlistThread = new ThreadWithStop((ChannelPlayList) getContent());
			playlistThread.start();
		} else
			return localSetContent_notLists();

		return true;
	}

	@Override
	protected boolean localSetContent_notLists() {
		// TODO - It is likely that this method goes into the superclass (June 2018)
		
		// FIXME This line could be risky! But it is needed for URL arguments
		final String url = getContent().getURI().toASCIIString().replace("&amp;", "&");

		final int frameNumber = getContent().getFrameNumber();
		final long expiration = getContent().getExpiration();

		final long dwellTime = (getContent().getTime() == 0 ? DEFAULT_DWELL_TIME : getContent().getTime());
		if (frameNumber > 0)
			frameRemovalTime[frameNumber] = FRAME_DISAPPEAR_TIME;
		println(getClass(), browserInstance.getInstance() + " Dwell time is " + dwellTime + ", expiration is " + expiration);

		if (url.equalsIgnoreCase(SELF_IDENTIFY)) {
			if (previousPreviousChannel == null)
				previousPreviousChannel = previousChannel;

			if (!showingSelfIdentify) {
				showingSelfIdentify = true;
				browserInstance.showIdentity();
				new Thread() {
					public void run() {
						catchSleep(SHOW_SPLASH_SCREEN_TIME);
						browserInstance.removeIdentify();
						showingSelfIdentify = false;
						setContentBypass(previousChannel);
						updateMyStatus();
					}
				}.start();
				// }
			}
		} else if (url.equalsIgnoreCase(FORCE_REFRESH)) {
			try {
				if (previousPreviousChannel == null)
					previousPreviousChannel = previousChannel;

				// First, do a brute-force refresh to the browser
				browserInstance.forceRefresh(frameNumber);
			} catch (Exception e) {
				System.err.println(getClass().getSimpleName() + ":" + browserInstance.getInstance()
						+ " Somthing is wrong with this re-used URL: [" + previousChannel.getURI().toASCIIString() + "]");
				e.printStackTrace();
				return false;
			}
		} else
			try {
				if (expiration > 0) {
					if (previousPreviousChannel == null)
						previousPreviousChannel = previousChannel;
					revertTimeRemaining = expiration;
					skipRevert = false;
				} else {
					revertTimeRemaining = 0;
					skipRevert = true;
				}

				/*
				 * TODO -- Issues with multiple frames
				 * 
				 * 1. the previousChannel gets lost because there is only one "previousChannel"
				 * 
				 * 2. The status will read the contents of this extra frame
				 */
				if (frameNumber > 0 && frameRemovalTime[frameNumber] > 0) {
					if (browserInstance.changeURL(url, wrapperType, frameNumber, getContent().getCode())) {
						removeFrame[frameNumber] = false;
						setupRemoveFrameThread(frameNumber);
					} else {
						println(getClass(), ".localSetContent():" + browserInstance.getInstance() + " Failed to set frame "
								+ frameNumber + " content");
						return false;
					}
				} else {
					changeCount++;
					// if (playlistThread != null) {
					// playlistThread.stopMe = true;
					// playlistThread = null;
					// }

					// ******************** Normal channel change here ********************
					if (browserInstance.changeURL(url, wrapperType, frameNumber, getContent().getCode())) {
						previousChannel = getContent();
						showingSelfIdentify = false;
					} else {
						println(getClass(), ".localSetContent():" + browserInstance.getInstance() + " Failed to set content");
						return false;
					}

					if (expiration <= 0)
						setupRefreshThread(dwellTime, url, frameNumber);
					else
						setRevertThread();

				}

				updateMyStatus();
			} catch (UnsupportedEncodingException e) {
				System.err.println(getClass().getSimpleName() + ":" + browserInstance.getInstance()
						+ " Somthing is wrong with this URL: [" + url + "]");
				e.printStackTrace();
				return false;
			}
		return true;
	}

	/**
	 * This method handles the setup and the execution of the thread that refreshes the content after content.getTime()
	 * milliseconds.
	 */
	protected void setupRefreshThread(final long dwellTime, final String url, final int frameNumber) {
		if (dwellTime > 0) {
			final int thisChangeCount = changeCount;

			// The Use Case for the extra frame is to be able to show announcements. In that vein, it seems that
			// we want the frame to go away after a while.
			if (frameNumber != 0) {
				new Thread("TurnOffFrame" + getMessagingName()) {
					@Override
					public void run() {
						catchSleep(dwellTime);
						println(DisplayAsConnectionToFireFox.class,
								".localSetContent():" + browserInstance.getInstance() + " turning off the announcement frame");
						browserInstance.turnOffFrame(frameNumber);
					}
				}.start();
			} else {
				new Thread("RefreshContent" + getMessagingName()) {
					@Override
					public void run() {
						long increment = 15000L;
						long localDwellTime = dwellTime;
						while (true) {

							// TODO -- If there is an emergency message up, we don't want to refresh and make it go away!

							for (long t = localDwellTime; t > 0 && changeCount == thisChangeCount; t -= increment) {
								catchSleep(Math.min(increment, t));
							}
							if (showingEmergencyMessage) {
								localDwellTime = Math.min(dwellTime, ONE_MINUTE);
								continue;
							}
							if (changeCount == thisChangeCount) {
								println(DisplayAsConnectionToFireFox.class,
										".localSetContent():" + browserInstance.getInstance() + " Reloading web page " + url);
								try {
									if (!browserInstance.changeURL(url, wrapperType, frameNumber, getContent().getCode())) {
										println(DisplayAsConnectionToFireFox.class,
												".localSetContent(): Failed to REFRESH content");
										browserInstance.resetURL();
										continue; // TODO -- Figure out what to do here. For now, just try again later
									}
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
							} else {
								println(DisplayAsConnectionToFireFox.class, ".localSetContent():" + browserInstance.getInstance()
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
		// final String revertURL = revertToThisChannel.getURI().toString();

		revertTimeRemaining = getContent().getExpiration();
		println(getClass(),
				browserInstance.getInstance() + " Setting the revert time for this temporary channel to " + revertTimeRemaining);

		if (revertThread == null) {
			revertThread = new Thread("RevertContent" + getMessagingName()) {
				/*
				 * 7/6/15 -------------------------------------------------------------------------------------------------------
				 * Something is getting messed up here, between a channel expiring and a channel that might need to be refreshed.
				 * Also in the mix, potentially, is when a channel change to a fixed image is requested and this launches a full
				 * refresh of the browser.
				 * 
				 * ----------------------------------------------------------------------------------------------------------------
				 * 
				 * Update 7/9/15: It looks like there are can be several of these threads run at once, and when they all expire,
				 * something bad seems to happen. New code added to only let one of these thread be created (and it can be "renewed"
				 * if necessary, before it expires).
				 * 
				 * ----------------------------------------------------------------------------------------------------------------
				 * 
				 * 1/14/2016: This is all resolved now. Also, changed to handling the channel list down here. This ends up making
				 * the channel list class behave almost identically to all the other SingnageContent classes, except for right here
				 * where (as they say) the rubber meets the road.
				 */

				@Override
				public void run() {
					long increment = 15000L;
					println(DisplayAsConnectionToFireFox.class, ".setRevertThread(): Revert thread started.");

					while (true) {
						for (; revertTimeRemaining > 0; revertTimeRemaining -= increment) {
							catchSleep(Math.min(increment, revertTimeRemaining));
							if (skipRevert) {
								println(DisplayAsConnectionToFireFox.class,
										".setRevertThread():" + browserInstance.getInstance() + " No longer necessary to revert.");
								revertThread = null;
								revertTimeRemaining = 0L;
								return;
							}
						}
						println(DisplayAsConnectionToFireFox.class, ".localSetContent():" + browserInstance.getInstance()
								+ " Reverting to channel " + previousPreviousChannel);
						try {
							setContent(previousPreviousChannel);
							// TODO -- Do we need to have a stack of previous channels so one can traverse backwards in a situation
							// like this??
							previousChannel = previousPreviousChannel;
							previousPreviousChannel = null;
							println(DisplayAsConnectionToFireFox.class, ".setRevertThread():" + browserInstance.getInstance()
									+ " Reverted to original web page, " + previousChannel);
						} catch (Exception e) {
							e.printStackTrace();
						}
						revertThread = null;
						revertTimeRemaining = 0L;
						return;
					}
				}
			};
			revertThread.start();
		} else {
			println(DisplayAsConnectionToFireFox.class, ".setRevertThread(): revert thread is already running.");
		}
	}

	private void setupRemoveFrameThread(final int frameNumber) {
		if (frameRemovalTime[frameNumber] <= 0)
			return;

		if (frameRemovalThread[frameNumber] != null) {
			println(this.getClass(), ".setupRemovalFrameThread() Already running.");
			return;
		}
		println(getClass(), browserInstance.getInstance() + " Will turn off frame " + frameNumber + " in "
				+ frameRemovalTime[frameNumber] + " msec.");

		Thread t = new Thread("RemoveExtraFrame") {
			@Override
			public void run() {
				long increment = 15000L;
				while (removeFrame[frameNumber] == false && frameRemovalTime[frameNumber] > 0) {
					println(DisplayAsConnectionToFireFox.class, browserInstance.getInstance() + " Continuing; frame off in  "
							+ frameNumber + " in " + frameRemovalTime[frameNumber] + " msec");
					catchSleep(Math.min(increment, frameRemovalTime[frameNumber]));
					frameRemovalTime[frameNumber] -= increment;
				}
				println(DisplayAsConnectionToFireFox.class, ".setupRemovalFrameThread() removing frame " + frameNumber + " now.");

				browserInstance.hideFrame(frameNumber);
				frameRemovalThread[frameNumber] = null;
				removeFrame[frameNumber] = false;
			}
		};
		frameRemovalThread[frameNumber] = t;
		t.start();
	}

	@Override
	protected void endAllConnections() {
		if (browserInstance != null)
			browserInstance.exit();
		if (browserLauncher != null)
			browserLauncher.exit();
	}

	@Override
	public void disconnect() {
		endAllConnections();
	}

	protected boolean isVerifiedChannel(SignageContent c) {
		if (c == null)
			return false;
		return listOfValidURLs.contains(c);
	}

	/**
	 * @param args
	 *            Expect one command line argument
	 */
	public static void main(final String[] args) {
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
	protected void setWrapperType(WrapperType wrapperType) {
		this.wrapperType = wrapperType;
	}

	@Override
	protected String getStatusString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (messagingClient.getServerTimeStamp() + 6 * ONE_HOUR > System.currentTimeMillis())
			sdf = new SimpleDateFormat("HH:mm:ss");

		String retval = sdf.format(new Date(messagingClient.getServerTimeStamp())) + " ";

		if (browserInstance == null) {
			retval += "*NOT CONNECTED TO BROWSER* Initializing ...";
		} else if (!browserInstance.isConnected()) {
			retval += "*NOT CONNECTED TO BROWSER* Last page " + getContent().getURI();
		} else if (getContent() == null)
			retval += "Off Line";
		else {
			if (specialURI(getContent())) {
				retval += (getStatus() + " (" + previousChannel.getURI() + ")").replace("'", "\\'");
			} else
				retval += (getStatus() + " (" + getContent().getURI() + ")").replace("'", "\\'");
		}
		return retval;
	}

	@SuppressWarnings("unused")
	private String javascriptStringToSendCharToBrowser(char c, boolean ctl, boolean alt, boolean shift, boolean meta) {
		// 40 seems to be up-arrow; 38 seems to be down-arrow. See https://jsfiddle.net/5se13tmg/
		String javaScriptCommand = "var keyboardEvent = document.createEvent(\"KeyboardEvent\");\n";
		javaScriptCommand += "var initMethod = typeof keyboardEvent.initKeyboardEvent !== 'undefined' ? \"initKeyboardEvent\" : \"initKeyEvent\";\n";
		javaScriptCommand += "keyboardEvent[initMethod](\"keydown\",true,true,window," + ctl + "," + alt + "," + shift + "," + meta
				+ "," + c + ",0);\n";
		javaScriptCommand += "document.dispatchEvent(keyboardEvent);\n";
		return javaScriptCommand;
	}
}
