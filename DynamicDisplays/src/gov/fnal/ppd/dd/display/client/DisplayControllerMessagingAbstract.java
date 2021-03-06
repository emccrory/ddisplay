package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.dd.GlobalVariables.FORCE_REFRESH;
import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.dd.GlobalVariables.SELF_IDENTIFY;
import static gov.fnal.ppd.dd.GlobalVariables.getDefaultDwellTime;
import static gov.fnal.ppd.dd.GlobalVariables.getMessagingServerName;
import static gov.fnal.ppd.dd.GlobalVariables.setFlavorFromDatabase;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;
import static gov.fnal.ppd.dd.util.specific.PackageUtilities.convertContentToDBReadyString;
import static gov.fnal.ppd.dd.util.specific.PackageUtilities.getChannelFromNumber;
import static gov.fnal.ppd.dd.util.specific.PackageUtilities.makeEmptyChannel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.chat.DCProtocol;
import gov.fnal.ppd.dd.chat.ErrorProcessingMessage;
import gov.fnal.ppd.dd.chat.MessagingClient;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.display.DisplayImpl;
import gov.fnal.ppd.dd.emergency.EmergencyMessage;
import gov.fnal.ppd.dd.emergency.Severity;
import gov.fnal.ppd.dd.interfaces.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.EmergencyCommunication;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.nonguiUtils.Command;
import gov.fnal.ppd.dd.util.nonguiUtils.ExitHandler;
import gov.fnal.ppd.dd.util.nonguiUtils.PerformanceMonitor;
import gov.fnal.ppd.dd.util.specific.ObjectSigning;
import gov.fnal.ppd.dd.util.version.VersionInformation;
import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.messages.AreYouAliveMessage;
import gov.fnal.ppd.dd.xml.messages.ChangeChannelReply;
import gov.fnal.ppd.dd.xml.messages.EmergencyMessXML;

/**
 * <p>
 * This is the key class in the Display architecture, which looks like a "Display" to the rest of the system, but knows how to send
 * the display directives to the browser.
 * </p>
 * 
 * <p>
 * The key (concrete) sub classes are
 * <ul>
 * <li>DisplayAsConnectionToFireFox - Used at the beginning of the project: Firefox and the pmorch plugin:
 * <em>Stopped working with Firefox 60 in 2018, and this class was deleted from the repository 11/14/2019</em></li>
 * <li>DisplayAsConnectionThroughSelenium - Firefox and the Selenium framework. This is the full implementation of a Dynamic
 * Display.</li>
 * <li>DisplayAsSimpleBrowser - Uses system access to launch a browser and then relaunches the browser when the channel is changed.
 * This is a simplified implementation and it loses some of the functionality (full screen guarantee, display number on the screen,
 * emergency messages, ...</li>
 * </p>
 * 
 * <p>
 * It has these key attributes that deal with all the communications into an out of this class:
 * <ol>
 * <li>BrowserLauncher browserLauncher - Initializes the connection to the browser, e.g., starts it. However, as time as passed,
 * this class has become mostly obsolete.</li>
 * <li>ConnectionToBrowserInstance browserInstance - Handles all communications with the browser</li>
 * <li>MessagingClientLocal messagingClient - The messaging client that communicates with the server</li>
 * </ol>
 * </p>
 * 
 * Implement a Dynamic Display using an underlying browser through a simple messaging server
 * 
 * FIXME - Contains both Business logic and Database accesses (should be refactored)
 * 
 * TODO - Implement some Unit tests.  Some of this SHOULD be testable!
 * 
 * @author Elliott McCrory, Fermilab (2014-21)
 */
public abstract class DisplayControllerMessagingAbstract extends DisplayImpl implements BrowserErrorListener {

	private static final int				STATUS_UPDATE_PERIOD		= 30;
	private static final long				SHOW_SPLASH_SCREEN_TIME		= 15000l;
	private static final long				launchTime					= System.currentTimeMillis();
	private static final String				OFF_LINE					= "Off Line";

	/* --------------- The key attributes are here --------------- */
	private final MessagingClientLocal		messagingClient;
	protected ConnectionToBrowserInstance	browserInstance;
	/* --------------- ------------------------------------------- */

	protected final boolean					showNumber;
	protected long							lastFullRestTime;
	protected String						offlineMessage				= "";

	// private int statusUpdatePeriod = 10;
	private int								changeCount;
	private long							remainingTimeRemEmergMess	= 0l;
	private long							revertTimeRemaining			= 0L;
	private boolean							showingEmergencyMessage		= false;
	private boolean							offLine						= false;
	private boolean							showingSelfIdentify			= false;
	private boolean							skipRevert					= false;
	private double							cpuUsage					= 0.0;

	private final String					myName;
	private Thread							emergencyRemoveThread		= null;
	private Thread							revertThread				= null;

	private Command							lastCommand;
	private ThreadWithStop					playlistThread				= null;
	private EmergencyMessage				em;
	private ListOfValidChannels				listOfValidURLs				= new ListOfValidChannels();
	private VersionInformation				versionInfo					= VersionInformation.getVersionInformation();

	@SuppressWarnings("unused")
	private final String					mySubject;
	private Object							revertThreadWaitObject		= "Object for doing synchronization of revert thread";
	private int								numChannelChanges			= 0;
	protected boolean						noErrorsSeen				= true;

	/**
	 * Orchestrate the playing of a list of channels. This extension to the Thread class also allows us to stop it later,
	 * gracefully.
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	protected class ThreadWithStop extends Thread {
		public boolean			stopMe	= false;
		private ChannelPlayList	localPlayListCopy;

		public ThreadWithStop(final ChannelPlayList pl) {
			super("PlayChannelList_DisplayAsConnectionToSelenium");
			this.localPlayListCopy = pl;
		}

		public void run() {
			if (browserInstance == null) {
				printlnErr(ThreadWithStop.class, "run() - Browser connection not established.");
				return;
			}
			println(DisplayControllerMessagingAbstract.this.getClass(), " -- " + hashCode() + browserInstance.getConnectionCode()
					+ " : starting to play a list of length " + localPlayListCopy.getChannels().size());

			// NOTE : This implementation does not support lists of lists. And this does not make logical sense if we assume
			// that a list will play forever.

			int count = 0;
			while (!stopMe) {
				if (!showingEmergencyMessage) {
					localSetContent_notLists();
					catchSleep(localPlayListCopy.getTime());	

					localPlayListCopy.advanceChannel();
					println(DisplayControllerMessagingAbstract.this.getClass(),
							" " + Integer.toHexString(hashCode()) + browserInstance.getConnectionCode()
									+ ": List play continues with channel=[" + localPlayListCopy.getName() + ", "
									+ localPlayListCopy.getTime() + "msec], iteration " + count++);
				} else
					catchSleep(Math.min(localPlayListCopy.getTime(), ONE_MINUTE));
			}
			println(DisplayControllerMessagingAbstract.this.getClass(),
					" -- " + hashCode() + browserInstance.getConnectionCode() + " : Exiting the list player. " + count);
		}
	}

	/**
	 * @param ipName
	 *            - the IPName of this node
	 * @param vNumber
	 *            - the virtual (local) display number for this display
	 * @param dbNumber
	 *            - the index number in the database for this display
	 * @param screenNumber
	 *            - the screen number for this display
	 * @param showNumber
	 *            - does this display show its number?
	 * @param location
	 *            - The description of the location
	 * @param color
	 *            - the highlight color
	 */
	public DisplayControllerMessagingAbstract(final String ipName, final int vNumber, final int dbNumber, final int screenNumber,
			final boolean showNumber, final String location, final Color color) {
		super(ipName, vNumber, dbNumber, screenNumber, location, color);

		if (getContent() == null)
			throw new IllegalArgumentException("No content defined!");

		this.showNumber = showNumber;
		mySubject = myName = ipName + ":" + screenNumber + " (" + getVirtualDisplayNumber() + ")";
		messagingClient = new MessagingClientLocal(getMessagingServerName(), MESSAGING_SERVER_PORT, myName);

		cpuUsage = PerformanceMonitor.getCpuUsage();
	}

	protected String getStatusString() {
		String retval = "";
		if (browserInstance == null) {
			retval += "*NOT CONNECTED TO BROWSER* Initializing ...";
		} else if (!browserInstance.isConnected()) {
			retval += "*NOT CONNECTED TO BROWSER* Last page " + getContent().getURI();
		} else if (getContent() == null)
			retval += OFF_LINE + " " + offlineMessage;
		else {
			if (specialURI(getContent())) {
				retval += (previousChannelStack.peek().getURI() + " (" + getStatus() + ")").replace("'", "");
			} else
				retval += (getStatus() + " (" + getContent().getURI() + ")").replace("'", "")
						.replace("dynamicdisplays.fnal.gov", "dd").replace("Pictures", "Pics").replace("http://", "")
						.replace("https://", "").replace(".fnal.gov", "");
		}
		return retval;
	}

	protected boolean isVerifiedChannel(SignageContent c) {
		if (c == null)
			return false;
		return listOfValidURLs.contains(c);
	}

	@Override
	public void disconnect() {
		if (browserInstance != null)
			browserInstance.exit();
	}

	/**
	 * Must be called to start all the threads in this class instance! Changed (11/2018) to not block
	 */
	public void initiate() {
		new Thread("WaitForServerToAppear") {
			public void run() {
				if (!messagingClient.start())
					messagingClient.retryConnection();
			}
		}.start();
	}

	public String getMessagingName() {
		return myName;
	}

	/**
	 * This method must be called by concrete class. Start various threads necessary to maintain this job.
	 */
	protected synchronized final void contInitialization() {
		Timer timer = new Timer("DisplayDaemons");

		timer.schedule(new TimerTask() {
			public void run() {
				// The thread that updates the status in the database
				try {
					updateMyStatus();
				} catch (Exception e) {
					printlnErr(DisplayControllerMessagingAbstract.class,
							" !! Exception caught in status update thread, " + e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		}, STATUS_UPDATE_PERIOD * ONE_SECOND / 2L, STATUS_UPDATE_PERIOD * ONE_SECOND);

		timer.schedule(new TimerTask() {
			// Once an hour, print something meaningful in the log
			public void run() {
				try {
					String message = screenNumber + " showing: " + (getContent() == null ? " *null* " : getContent().getName());
					if (getContent() instanceof Channel)
						message += "\n                             -- (" + ((Channel) getContent()).getURI() + ")";
					message += "\n                             -- Last communication with server: "
							+ new Date(messagingClient.getServerTimeStamp());
					println(DisplayControllerMessagingAbstract.this.getClass(), message);
				} catch (Exception e) {
					printlnErr(DisplayControllerMessagingAbstract.this.getClass(),
							" !! Exception caught in diagnostic thread, " + e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		}, FIFTEEN_MINUTES, ONE_HOUR);

		Runtime.getRuntime().addShutdownHook(new Thread("ConnectionShutdownHook") {
			public void run() {
				// The shutdown hook.
				println(DisplayControllerMessagingAbstract.this.getClass(), "Exit hook called.");
				// keepGoing = false;
				offLine = true;
				updateMyStatus();
				catchSleep(250);
				disconnect();

				// In the Selenium framework, the geckodriver process sticks around. It is assumed that these extra processes are
				// removed elsewhere. At this time, that task is in the runDisplay startup script.
			}
		});
	}

	/**
	 * Update my status to the database
	 */
	protected final synchronized void updateMyStatus() {
		Connection connection;
		cpuUsage = PerformanceMonitor.getCpuUsage();
		DecimalFormat dd = new DecimalFormat("#.#");

		try {
			connection = ConnectionToDatabase.getDbConnection();

			String statementString = "";
			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {
					Date dNow = new Date();
					long uptime = System.currentTimeMillis() - launchTime;

					SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String statusString = getStatusString();
					if (offLine)
						statusString = OFF_LINE;
					String version = versionInfo.getVersionString();
					if (cpuUsage > 0.1)
						statusString = "L" + dd.format(cpuUsage) + " " + statusString;
					statusString = statusString.replace("'", "").replace("\"", "").replace(";", "").replace("\\", "");
					if (statusString.length() > 255)
						// The Content field is limited to 255 characters.
						statusString = statusString.substring(0, 249) + " ...";

					String contentName = getContent().getName().replace("'", "").replace("\"", "").replace(";", "").replace("\\",
							"");

					// Create the stream of the current SignageContent object
					String blob = convertContentToDBReadyString(getContent());

					String X = "x";
					if (blob != null && blob.length() > 5 && blob.substring(0, 5).equalsIgnoreCase("<?xml"))
						X = "";

					statementString = "UPDATE DisplayStatus set Time='" + ft.format(dNow) + "',Content='" + statusString
							+ "',ContentName='" + contentName + "', SignageContent=" + X + "'" + blob + "', Uptime=" + uptime
							+ ", Version='" + version + "', NumChanges=" + numChannelChanges + " where DisplayID="
							+ getDBDisplayNumber();

					int numRows = stmt.executeUpdate(statementString);
					if (numRows == 0 || numRows > 1) {
						println(getClass(),
								" screen " + screenNumber
										+ ": Problem while updating status of Display: Expected to modify exactly one row, but  modified "
										+ numRows + " rows instead. SQL='" + statementString + "'");
					}
					stmt.close();

				} catch (Exception ex) {
					String mess = " screen " + screenNumber + " -- Unexpected exception in method updateMyStatus: " + ex
							+ "\n\t\t\tLast query: [" + statementString + "]\n\t\t\tSkipping this update.";
					for (StackTraceElement F : ex.getStackTrace()) {
						mess += "\n\t\t\t -- " + F;
					}
					println(getClass(), mess);
					ex.printStackTrace();
					return;
				}
			}
		} catch (Exception e) {
			println(getClass(), screenNumber + " -- Unexpected exception (" + e
					+ ") in method updateMyStatus while reestablishing connection to the database.");
			e.printStackTrace();
			return;
		}
	}

	public final void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		respondToContentChange(localSetContent(), "setContent failed");
	}

	protected final boolean localSetContent_notLists() {
		if (browserInstance == null) {
			printlnErr(getClass(), "localSetContent_notLists() - Browser connection not established.");
			return false;
		}
		// FIXME - This line could be risky! But it is needed for URL arguments
		final String url = getContent().getURI().toASCIIString().replace("&amp;", "&");
		final long expiration = getContent().getExpiration();
		final long dwellTime = (getContent().getTime() == 0 ? getDefaultDwellTime() : getContent().getTime());

		println(getClass(), browserInstance.getConnectionCode() + " Dwell time is " + dwellTime + ", expiration is " + expiration);

		if (url.equalsIgnoreCase(SELF_IDENTIFY)) {
			// if (previousPreviousChannel == null)
			// previousPreviousChannel = previousChannel;

			if (!showingSelfIdentify) {
				showingSelfIdentify = true;
				browserInstance.showIdentity();
				new Thread() {
					public void run() {
						// Wait for the prescribed time to remove the self-identification screen
						catchSleep(SHOW_SPLASH_SCREEN_TIME);
						browserInstance.removeIdentify();
						showingSelfIdentify = false;
						// setContentBypass(previousChannel);
						previousChannelStack.pop();
						setContentBypass(previousChannelStack.peek());
						updateMyStatus();
					}
				}.start();
			}
		} else if (url.equalsIgnoreCase(FORCE_REFRESH)) {
			try {
				// Tell the browser to show the current page, again.
				previousChannelStack.pop(); // Remove the "Refresh" fake channel
				String lastURL = previousChannelStack.peek().getURI().toASCIIString().replace("&amp;", "&");
				int code = previousChannelStack.peek().getCode();
				if (browserInstance.changeURL(lastURL, code)) {
					showingSelfIdentify = false;
				} else {
					println(getClass(), ".localSetContent():" + browserInstance.getConnectionCode() + " Failed to set content");
					return false;
				}
			} catch (Exception e) {
				System.err.println(getClass().getSimpleName() + ":" + browserInstance.getConnectionCode()
						+ " Somthing is wrong with this re-used URL: [" + previousChannelStack.peek().getURI().toASCIIString()
						+ "]");
				e.printStackTrace();
				return false;
			}
		} else
			try {
				if (expiration > 0) {
					// if (previousPreviousChannel == null)
					// previousPreviousChannel = previousChannel;
					revertTimeRemaining = expiration;
					skipRevert = false;
				} else {
					revertTimeRemaining = 0;
					skipRevert = true;
				}

				changeCount++;

				// ******************** Normal channel change here ********************
				if (browserInstance.changeURL(url, getContent().getCode())) {
					previousChannelStack.pop(); // Replace the top element of this history stack with the new channel
					previousChannelStack.push(getContent());
					showingSelfIdentify = false;
				} else {
					println(getClass(), ".localSetContent():" + browserInstance.getConnectionCode() + " Failed to set content");
					return false;
				}

				noErrorsSeen = true;
				if (expiration <= 0)
					setupRefreshThread(dwellTime, url);
				else
					setRevertThread();

				updateMyStatus();
			} catch (UnsupportedEncodingException e) {
				System.err.println(getClass().getSimpleName() + ":" + browserInstance.getConnectionCode()
						+ " Somthing is wrong with this URL: [" + url + "]");
				e.printStackTrace();
				return false;
			}
		return true;
	}

	protected final boolean localSetContent() {
		if (browserInstance == null) {
			printlnErr(getClass(), "localSetContent() - Browser connection not established.");
			return false;
		}

		if (playlistThread != null) {
			playlistThread.stopMe = true;
			playlistThread = null;
		}

		numChannelChanges++;
		if (getContent() instanceof EmergencyCommunication) {
			// Emergency communication!!!!
			em = ((EmergencyCommunication) getContent()).getMessage();
			boolean retval = browserInstance.showEmergencyCommunication(em);
			showingEmergencyMessage = true;
			if (em.getSeverity() == Severity.REMOVE) {
				// setContent(previousChannelStack.pop());
				showingEmergencyMessage = false;
				browserInstance.removeEmergencyCommunication();
			} else {
				// Remove this message after a while
				remainingTimeRemEmergMess = em.getDwellTime();
				System.err.println(
						"Initial time for emergency message on " + getDBDisplayNumber() + ": " + remainingTimeRemEmergMess);
				if (emergencyRemoveThread == null || !emergencyRemoveThread.isAlive()) {
					emergencyRemoveThread = new Thread("RemoveEmergencyCommunication") {
						long interval = 2000L;

						public void run() {
							for (; remainingTimeRemEmergMess > 0
									&& showingEmergencyMessage; remainingTimeRemEmergMess -= interval) {
								catchSleep(Math.min(interval, remainingTimeRemEmergMess));
							}
							showingEmergencyMessage = false;
							emergencyRemoveThread = null;
							browserInstance.removeEmergencyCommunication();
						}
					};
					emergencyRemoveThread.start();
				}
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

			previousChannelStack.pop();
			previousChannelStack.push(getContent());
			playlistThread = new ThreadWithStop((ChannelPlayList) getContent());
			playlistThread.start();
		} else {
			previousChannelStack.pop(); // Get rid of top of previous
			previousChannelStack.push(getContent());
			return localSetContent_notLists();
		}
		return true;
	}

	@Override
	protected void respondToContentChange(boolean b, String why) {
		// generate a reply message to the sender
		println(getClass(),
				" -- Responding to channel change with " + (b ? "SUCCESS" : "failure") + " to " + messagingClient.getLastFrom());
		MessageCarrierXML msg = null;
		if (b) {
			ChangeChannelReply replyMessage = new ChangeChannelReply();
			ChannelSpec channelSpec = new ChannelSpec(getContent());
			replyMessage.setChannelSpec(channelSpec);
			replyMessage.setDisplayNum(getDBDisplayNumber());
			msg = MessageCarrierXML.getReplyMessage(messagingClient.getName(), messagingClient.getLastFrom(), replyMessage);

		} else {
			msg = MessageCarrierXML.getErrorMessage(messagingClient.getName(), messagingClient.getLastFrom(),
					"Channel change not successful: " + why);
		}

		messagingClient.sendMessage(msg);

	}

	private void setRevertThread() {
		if (browserInstance == null) {
			printlnErr(getClass(), "setRevertThread() - Browser connection not established.");
			return;
		}
		synchronized (revertThreadWaitObject) {
			revertTimeRemaining = getContent().getExpiration();
			println(getClass(), browserInstance.getConnectionCode() + " Setting the revert time for this temporary channel to "
					+ revertTimeRemaining);

			if (revertThread == null) {
				revertThread = new Thread("RevertContent" + getMessagingName()) {
					/*
					 * 7/6/15
					 * -------------------------------------------------------------------------------------------------------
					 * Something is getting messed up here, between a channel expiring and a channel that might need to be
					 * refreshed. Also in the mix, potentially, is when a channel change to a fixed image is requested and this
					 * launches a full refresh of the browser.
					 * 
					 * -------------------------------------------------------------------------------------------------------------
					 * ---
					 * 
					 * Update 7/9/15: It looks like there are can be several of these threads run at once, and when they all expire,
					 * something bad seems to happen. New code added to only let one of these thread be created (and it can be
					 * "renewed" if necessary, before it expires).
					 * 
					 * -------------------------------------------------------------------------------------------------------------
					 * ---
					 * 
					 * 1/14/2016: This is all resolved now. Also, changed to handling the channel list down here. This ends up
					 * making the channel list class behave almost identically to all the other SingnageContent classes, except for
					 * right here where (as they say) the rubber meets the road.
					 * 
					 * -------------------------------------------------------------------------------------------------------------
					 * ---
					 *
					 * 8/23/2018: I think there is a problem with this "revert" idea if more than one channel gets shown that needs
					 * to disappear (be reverted). That is, the recursion of this operation is not right. There are at least two
					 * ways to fix this: (1) Make iut truly recursive, so that if you send channels, A, B, and C, which all have the
					 * revert time set, it will revert from C to B, wait, from B to A, wait, and then from A to the original
					 * channel. (2) Don't ever revert to a channel that will also revert, but keep track of the last non-reverting
					 * channel and then revert to that when the last reverting-channel expires (this is in line with the actual
					 * "Docent" use case here)
					 */

					@Override
					public void run() {
						long increment = 5000L;
						println(DisplayControllerMessagingAbstract.class,
								".setRevertThread():  Will revert to previous channel in " + revertTimeRemaining + " msec");

						while (true) {
							for (; revertTimeRemaining > 0; revertTimeRemaining -= increment) {
								catchSleep(Math.min(increment, revertTimeRemaining));
								if (skipRevert)
									synchronized (revertThreadWaitObject) {
										println(DisplayControllerMessagingAbstract.class, ".setRevertThread():"
												+ browserInstance.getConnectionCode() + " No longer necessary to revert.");
										revertThread = null;
										revertTimeRemaining = 0L;
										return;
									}
							}
							synchronized (revertThreadWaitObject) {
								previousChannelStack.pop(); // The channel already playing is the one at the top of the stack.
								println(DisplayControllerMessagingAbstract.class,
										".setRevertThread():" + browserInstance.getConnectionCode() + " Reverting to channel "
												+ previousChannelStack.peek());
								revertThread = null;
								revertTimeRemaining = 0L;

								try {
									setContent(previousChannelStack.pop());
								} catch (Exception e) {
									e.printStackTrace();
								}
								return;
							}
						}
					}
				};
				revertThread.start();
			} else {
				println(DisplayControllerMessagingAbstract.class, ".setRevertThread(): revert thread is already running.");
			}
		}
	}

	// @SuppressWarnings("unused")
	// private void setupRemoveFrameThread(final int frameNumber) {
	// if (frameRemovalTime[frameNumber] <= 0)
	// return;
	//
	// if (frameRemovalThread[frameNumber] != null) {
	// println(this.getClass(), ".setupRemovalFrameThread() Already running.");
	// return;
	// }
	// println(getClass(), browserInstance.getInstance() + " Will turn off frame " + frameNumber + " in "
	// + frameRemovalTime[frameNumber] + " msec.");
	//
	// Thread t = new Thread("RemoveExtraFrame") {
	// @Override
	// public void run() {
	// long increment = 15000L;
	// while (removeFrame[frameNumber] == false && frameRemovalTime[frameNumber] > 0) {
	// println(DisplayControllerMessagingAbstract.this.getClass(), browserInstance.getInstance()
	// + " Continuing; frame off in " + frameNumber + " in " + frameRemovalTime[frameNumber] + " msec");
	// catchSleep(Math.min(increment, frameRemovalTime[frameNumber]));
	// frameRemovalTime[frameNumber] -= increment;
	// }
	// println(DisplayControllerMessagingAbstract.this.getClass(),
	// ".setupRemovalFrameThread() removing frame " + frameNumber + " now.");
	//
	// browserInstance.hideFrame(frameNumber);
	// frameRemovalThread[frameNumber] = null;
	// removeFrame[frameNumber] = false;
	// }
	// };
	// frameRemovalThread[frameNumber] = t;
	// t.start();
	// }

	/**
	 * This method handles the setup and the execution of the thread that refreshes the content after content.getTime()
	 * milliseconds.
	 */
	protected void setupRefreshThread(final long dwellTime, final String url) {
		if (browserInstance == null) {
			printlnErr(getClass(), "setupRefreshThread() - Browser connection not established.");
			return;
		}
		if (dwellTime > 0) {
			final int thisChangeCount = changeCount;
			noErrorsSeen = true;

			new Thread("RefreshContent" + getMessagingName()) {

				@Override
				public void run() {
					long increment = 15000L;
					long localDwellTime = dwellTime;
					while (true) {

						for (long t = localDwellTime; t > 0 && changeCount == thisChangeCount && noErrorsSeen; t -= increment) {
							catchSleep(Math.min(increment, t));
						}
						if (showingEmergencyMessage) {
							// Emergency message is showing - do not refresh the web page. Check again in one minute.
							localDwellTime = Math.min(dwellTime, ONE_MINUTE);
							continue;
						}

						if (changeCount == thisChangeCount) {
							println(DisplayControllerMessagingAbstract.this.getClass(),
									".setupRefreshThread():" + browserInstance.getConnectionCode() + " Reloading web page " + url);
							try {
								noErrorsSeen = true;
								if (!browserInstance.changeURL(url, getContent().getCode())) {
									println(DisplayControllerMessagingAbstract.this.getClass(),
											".setupRefreshThread(): Failed to REFRESH content");
									browserInstance.resetURL();
									continue; // This might not be the right thing to do, but so far, it seems OK
								}
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						} else {
							println(DisplayControllerMessagingAbstract.this.getClass(),
									".setupRefreshThread():" + browserInstance.getConnectionCode() + " Not necessary to refresh "
											+ url + " because channel was changed already.");
							return;
						}
					}
				}

			}.start();
		}
	}

	/**
	 * Read the database to figure out what this display is supposed to do
	 * 
	 * @param clazz
	 *            The type of object to instantiate here
	 * @return The object that is the display controller.
	 */
	protected static DisplayControllerMessagingAbstract makeTheDisplays(Class<?> clazz) {
		DisplayControllerMessagingAbstract d = null;
		String myNode = "localhost";
		try {
			myNode = InetAddress.getLocalHost().getCanonicalHostName().replace(".dhcp.", ".");
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
		}

		setFlavorFromDatabase(myNode);
		String query = "SELECT IPName,DisplayID,VirtualDisplayNumber,Location,ColorCode,Port,Content,ScreenNumber FROM Display where IPName='"
				+ myNode + "'";

		Connection connection;
		try {
			connection = ConnectionToDatabase.getDbConnection();
		} catch (DatabaseNotVisibleException e1) {
			e1.printStackTrace();
			System.err.println("\nNo connection to the Signage/Displays database.");
			return failSafeVersion(clazz);
		}

		synchronized (connection) {
			// Use ARM to simplify these try blocks.
			try (Statement stmt = connection.createStatement();) {
				try (ResultSet rs = stmt.executeQuery("USE " + DATABASE_NAME)) {
				}

				int displayCount = 0;

				try (ResultSet rs = stmt.executeQuery(query);) {
					// Move to first returned row (there will be more than one if there are multiple screens on this PC)
					if (rs.first()) {
						while (!rs.isAfterLast()) {
							try {
								String myName = rs.getString("IPName");
								String ordinal = "First";
								if (displayCount++ == 1)
									ordinal = "Second";
								else if (displayCount == 2)
									ordinal = "Third";
								else if (displayCount > 2)
									ordinal = ordinal + "th";

								System.out.println("\n********** " + ordinal + " Display, name=" + myName + " **********\n");

								if (!myName.equals(myNode)) {
									// TODO This will not work if the IPName in the database is an IP address.
									System.out.println(
											"The node name of this display, according to the database, is supposed to be '" + myName
													+ "', but InetAddress.getLocalHost().getCanonicalHostName() says it is '"
													+ myNode + "'\n\t** We'll try to run anyway, but this should be fixed **");
									// System.exit(-1);
								}
								int dbNumber = rs.getInt("DisplayID");
								int vNumber = rs.getInt("VirtualDisplayNumber");

								println(DisplayControllerMessagingAbstract.class,
										"The node name of this display (no. " + vNumber + "/" + dbNumber + ") is '" + myNode + "'");

								String location = rs.getString("Location");
								String colorString = rs.getString("ColorCode");
								Color color = new Color(Integer.parseInt(colorString, 16));

								// Note that this field, "Port", is a boolean flag as to whether or not it should show its virtual
								// display number on itself; 0 = show the number, non-zero = hide the number
								boolean showNumber = rs.getInt("Port") == 0;
								int channelNumber = rs.getInt("Content");
								SignageContent cont = getChannelFromNumber(channelNumber);

								int screenNumber = rs.getInt("ScreenNumber");

								// Now create the class object

								Constructor<?> cons;
								try {
									cons = clazz.getConstructor(String.class, int.class, int.class, int.class, boolean.class,
											String.class, Color.class);
									d = (DisplayControllerMessagingAbstract) cons.newInstance(
											new Object[] { myName, vNumber, dbNumber, screenNumber, showNumber, location, color });
									d.setContentBypass(cont);
									d.initiate();
									// return d;
								} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InstantiationException
										| IllegalArgumentException | InvocationTargetException e) {
									e.printStackTrace();
									System.err.println("Here are the arguments: " + myName + ", " + vNumber + ", " + dbNumber
											+ ", 0, " + (showNumber ? "ShowNum" : "HideNum") + ", " + location + ", " + color);
								}
								// Allow it to loop over multiple displays
								// return null;
							} catch (Exception e) {
								e.printStackTrace();
							}

							if (!rs.next())
								break;
						}
					} else {
						new Exception("No database rows returned for query, '" + query + "'").printStackTrace();
						System.err.println("\n** Cannot continue! **\nExit");
						connection.close();
						return failSafeVersion(clazz);
					}

				} catch (SQLException e) {
					System.err.println("Faulty query is [" + query + "]");
					e.printStackTrace();
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
				return failSafeVersion(clazz);
			}
		}
		return d;
	}

	private static DisplayControllerMessagingAbstract failSafeVersion(Class<?> clazz) {
		Constructor<?> cons;
		try {
			cons = clazz.getConstructor(String.class, int.class, int.class, int.class, String.class, Color.class);
			DisplayControllerMessagingAbstract d = (DisplayControllerMessagingAbstract) cons
					.newInstance(new Object[] { "Failsafe Display", 99, 0, 0, "Failsafe location", Color.red });
			d.initiate();
			d.setContentBypass(makeEmptyChannel(null));

			return d;
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InstantiationException
				| IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected class MessagingClientLocal extends MessagingClient {
		private DCProtocol	dcp				= new DCProtocol();
		private Thread		myShutdownHook;
		private String		lastFrom;
		private int			messageCount	= 0;

		// private int myDisplayNumber, myScreenNumber;

		public MessagingClientLocal(String server, int port, String username) { // , int myDisplayNumber, int myScreenNumber) {
			super(server, port, username);
			dcp.addListener(DisplayControllerMessagingAbstract.this);
			dcp.addListener(new UpdateDatabaseForDisplayChannels(getDBDisplayNumber()));
			// this.myDisplayNumber = myDisplayNumber;
			// this.myScreenNumber = myScreenNumber;

			this.myShutdownHook = new Thread("ShutdownHook_of_MessagingClient_" + username) {
				public void run() {
					close();
					// Runtime.getRuntime().removeShutdownHook(myShutdownHook); One does not remove the shutdown hook during a
					// shutdown
					// myShutdownHook = null;
				}
			};

			Runtime.getRuntime().addShutdownHook(myShutdownHook);
		}

		public String getLastFrom() {
			return lastFrom;
		}

		public long getServerTimeStamp() {
			return lastMessageReceived;
		}

		@Override
		public void connectionAccepted() {
			displayLogMessage("Connection accepted at " + (new Date()));
		}

		@Override
		public void receiveIncomingMessage(MessageCarrierXML msg) throws ErrorProcessingMessage {
			// TODO -- Should this method be synchronized?
			// Does it make sense to have two messages processed at the same time, or not?

			if ((messageCount++ % 5) == 0 || !(msg.getMessageValue() instanceof AreYouAliveMessage)) {
				println(this.getClass(), screenNumber + ":" + MessagingClientLocal.class.getSimpleName()
						+ ".displayIncomingMessage(): Got this message:\n[" + msg + "]");
			}
			lastFrom = msg.getMessageOriginator();

			if (!msg.isReadOnly())
				if (!(msg.getMessageValue() instanceof EmergencyMessXML)) {
					if (!ObjectSigning.isClientAuthorized(msg.getMessageOriginator(), msg.getMessageRecipient())) {
						// Improperly signed message -- oops!
						sendMessage(MessageCarrierXML.getErrorMessage(msg.getMessageRecipient(), msg.getMessageOriginator(),
								"The directive to change the channel was not properly signed."));
						return;
					}
					if (showingEmergencyMessage) {
						// Reject this channel change since we are showing an emergency message right now.
						sendMessage(MessageCarrierXML.getErrorMessage(msg.getMessageRecipient(), msg.getMessageOriginator(),
								"This display is showing an emergency message - channel change rejected."));
						return;
					}
				}
			// if (msg.getMessageRecipient().equals(getName()) || msg.getMessageRecipient().startsWith(getName())) {
			// ^^ If we get a message down here, it will always be to this node. So no need to check. ^^
			if (!dcp.processInput(msg)) {
				sendMessage(MessageCarrierXML.getErrorMessage(msg.getMessageRecipient(), msg.getMessageOriginator(),
						dcp.getErrorMessageText()));
			}
			// } else if (debug)
			// println(this.getClass(),
			// screenNumber + ": Ignoring a message of type " + msg.getMessageValue().getClass().getSimpleName()
			// + ", sent to [" + msg.getMessageRecipient() + "] because I am [" + getName() + "]");
			// Hmmm. This is not right. I see this print all the time:
			// 1: Ignoring a message of type AreYouAliveMessage, sent to [ad130482.fnal.gov:1 (4)_1] because I am
			// [ad130482.fnal.gov:1 (4)]

		}

		@Override
		public void replyToAnError(final String why) {
			respondToContentChange(false, why);
		}
	}

	/**
	 * Set up the final channel object save (prior to an unexpected exit)
	 * 
	 * @return did we manage to set a channel from the save file?
	 */
	protected boolean initializeSavedChannelObject() {
		boolean retval = false;
		final String filename = "lastChannel_display" + getDBDisplayNumber() + ".ser";

		try {
			// read the last channel object, if it is there.
			try (FileInputStream fin = new FileInputStream(filename)) {
				try (ObjectInputStream ois = new ObjectInputStream(fin)) {

					Object content = ois.readObject();

					println(DisplayControllerMessagingAbstract.this.getClass(),
							": Retrieved a channel from the file system: [" + content + "]");

					if (content instanceof SignageContent) {
						if (specialURI((SignageContent) content))
							// An error condition from Feb, 2016 that (otherwise) really fouled things up!
							retval = false;
						else {
							// TODO -- Do we use localSetContent() here??
							setContent((SignageContent) content);
							retval = true;
						}
					} else {
						// Why in the world would the streamed content NOT be a SignageContent??
						throw new RuntimeException("expecting to read a streamed SignageContent but instead got a "
								+ content.getClass().getCanonicalName());
					}
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println(new Date() + " - " + getClass().getSimpleName() + ": '" + filename
					+ "'\n\t\t\tThere is no recovery channel information available in this file.  This is normal during a clean startup.");
		} catch (Exception e) {
			e.printStackTrace();
		}

		(new File(filename)).delete(); // Make sure this file is deleted now that the object stream has been used.

		ExitHandler.removeFinalCommand(lastCommand);
		lastCommand = ExitHandler.addFinalCommand(new Command() {

			@Override
			public void execute(String why) {
				try {
					SignageContent c = getContent();
					if (specialURI(c))
						c = previousChannelStack.peek();
					if (c == null)
						return;
					try (FileOutputStream fout = new FileOutputStream(filename)) {
						try (ObjectOutputStream oos = new ObjectOutputStream(fout)) {
							oos.writeObject(c);
						}
					}
					println(DisplayControllerMessagingAbstract.class,
							": Saved the currently-playing channel to the file system: [" + c + "]");

				} catch (Exception e) {
					e.printStackTrace();
				}
				// Can ignore closing all these open connections because we ASSUME that this is happening at the end of the life of
				// the VM.
			}

		});
		return retval;
	}

	protected static boolean specialURI(SignageContent content) {
		return content == null || content instanceof EmergencyCommunication
				|| content.getURI().toASCIIString().equals(FORCE_REFRESH) || content.getURI().toASCIIString().equals(SELF_IDENTIFY);
	}

	public String getOfflineMessage() {
		return offlineMessage;
	}

	public void setOfflineMessage(String offlineMessage) {
		this.offlineMessage = offlineMessage;
	}

	public void errorInPageLoad(int value) {
		// The Selenium interface tells us that there is an error in loading the last page. Deal with it.
		printlnErr(getClass(),
				"Error value has been returned from the browser, value=[" + value + "].  Forcing a refresh of this page");

		// TODO - We expect these errors to be 403 ("Forbidden"), 404 ("Not Found") or 408 ("Timeout") (and maybe others someday).
		// Only with the timeout would we expect a simple refresh of the page to fix it.

		// Important false-positive: If the web page is secure, "https://", it can fail and the underlying javaScript that
		// is supposed to catch it will not see it. And since almost all of our channels are secure now, this code does
		// not do very much.

		switch (value) {
		case 403:
		case 404:
			// Refreshing the page is not going to work, but that's all we have right now

		case 408:
			noErrorsSeen = false;
			break;

		default:
			// An unknown error.
			noErrorsSeen = false;
			break;
		}
	}
}
