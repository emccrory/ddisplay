package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.dd.GlobalVariables.FORCE_REFRESH;
import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.SELF_IDENTIFY;
import static gov.fnal.ppd.dd.GlobalVariables.getMessagingServerName;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.convertObjectToHexBlob;
import static gov.fnal.ppd.dd.util.Util.getChannelFromNumber;
import static gov.fnal.ppd.dd.util.Util.makeEmptyChannel;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import javax.xml.bind.JAXBException;

import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.chat.DCProtocol;
import gov.fnal.ppd.dd.chat.ErrorProcessingMessage;
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.chat.MessagingClient;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.display.DisplayImpl;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.EmergencyCommunication;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.Command;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.util.ExitHandler;
import gov.fnal.ppd.dd.util.PerformanceMonitor;
import gov.fnal.ppd.dd.util.version.VersionInformation;
import gov.fnal.ppd.dd.xml.ChangeChannelReply;
import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;

/**
 * <p>
 * This is the key class in the Display architecture, which looks like a "Display" to the rest of the system, but knows how to send
 * the display directives to the browser.
 * </p>
 * 
 * <p>
 * The key (concrete) sub classes are
 * <ul>
 * <li>DisplayAsConnectionToFireFox - Used since the beginning of the project: Firefox and the pmorch plugin</li>
 * <li>DisplayAsConnectionThroughSelenium - Under development: Firefox and the Selenium framework</li>
 * </p>
 * 
 * <p>
 * It has these key attributes that deal with all the communications into an out of this class:
 * <ol>
 * <li>BrowserLauncher browserLauncher - Initializes the connection to the browser, e.g., starts it.</li>
 * <li>ConnectionToBrowserInstance browserInstance - Handles all communications with the browser</li>
 * <li>MessagingClientLocal messagingClient - The messaging client that communicates with the server</li>
 * </ol>
 * </p>
 * 
 * Implement a Dynamic Display using an underlying browser through a simple messaging server
 * 
 * FIXME - Contains business logic and database accesses
 * 
 * @author Elliott McCrory, Fermilab (2014-18)
 */
public abstract class DisplayControllerMessagingAbstract extends DisplayImpl {

	protected static final int				STATUS_UPDATE_PERIOD	= 30;
	protected static final long				SOCKET_ALIVE_INTERVAL	= 2500l;
	protected static final long				SHOW_SPLASH_SCREEN_TIME	= 15000l;
	protected static final String			OFF_LINE				= "Off Line";
	protected static boolean				dynamic					= false;

	/* --------------- The key attributes are here --------------- */
	protected BrowserLauncher				browserLauncher;
	protected ConnectionToBrowserInstance	browserInstance;
	protected MessagingClientLocal			messagingClient;
	/* --------------- ------------------------------------------- */

	protected boolean						keepGoing				= true;
	protected boolean						showNumber				= true;
	protected VersionInformation			versionInfo				= VersionInformation.getVersionInformation();
	protected boolean						badNUC;
	protected boolean						showingEmergencyMessage	= false;

	private boolean							offLine					= false;
	private String							myName;
	private int								statusUpdatePeriod		= 10;
	private double							cpuUsage				= 0.0;
	private Command							lastCommand;

	@SuppressWarnings("unused")
	private String							mySubject;

	// Use messaging to get change requests from the changers -->
	// private boolean actAsServerNoMessages = true;

	/**
	 * Extension to the Thread class to allow me to stop it later, gracefully.
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	protected class ThreadWithStop extends Thread {
		public boolean			stopMe	= false;
		private ChannelPlayList	localPlayListCopy;

		public ThreadWithStop(final ChannelPlayList pl) {
			super("PlayChannelList_" + DisplayAsConnectionToFireFox.class.getSimpleName());
			this.localPlayListCopy = pl;
		}

		public void run() {
			println(DisplayAsConnectionToFireFox.class, " -- " + hashCode() + browserInstance.getInstance()
					+ " : starting to play a list of length " + localPlayListCopy.getChannels().size());

			// NOTE : This implementation does not support lists of lists. And this does not make logical sense if we assume
			// that a list will play forever.

			int count = 0;
			while (!stopMe) {
				if (!showingEmergencyMessage) {
					localSetContent_notLists();
					catchSleep(localPlayListCopy.getTime());
					localPlayListCopy.advanceChannel();
					println(DisplayAsConnectionToFireFox.class,
							" -- " + hashCode() + browserInstance.getInstance() + " : List play continues with channel=["
									+ localPlayListCopy.getDescription() + ", " + localPlayListCopy.getTime() + "msec] " + count++);
				} else
					catchSleep(Math.min(localPlayListCopy.getTime(), ONE_MINUTE));
			}
			println(DisplayAsConnectionToFireFox.class,
					" -- " + hashCode() + browserInstance.getInstance() + " : Exiting the list player. " + count);
		}
	}

	/**
	 * @param ipName
	 * @param vNumber
	 * @param dbNumber
	 * @param screenNumber
	 * @param showNumber
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayControllerMessagingAbstract(final String ipName, final int vNumber, final int dbNumber, final int screenNumber,
			final boolean showNumber, final String location, final Color color, final SignageType type) {
		super(ipName, vNumber, dbNumber, screenNumber, location, color, type);

		if (getContent() == null)
			throw new IllegalArgumentException("No content defined!");

		this.showNumber = showNumber;
		mySubject = myName = ipName + ":" + screenNumber + " (" + getVirtualDisplayNumber() + ")";
		messagingClient = new MessagingClientLocal(getMessagingServerName(), MESSAGING_SERVER_PORT, myName);

		cpuUsage = PerformanceMonitor.getCpuUsage();
	}

	protected abstract void endAllConnections();

	protected abstract String getStatusString();

	protected abstract void setWrapperType(WrapperType wrapperType);

	protected abstract boolean localSetContent_notLists();

	/**
	 * Must be called to start all the threads in this class instance!
	 */
	public void initiate() {
		if (!messagingClient.start()) {
			new Thread("WaitForServerToAppear") {
				public void run() {
					messagingClient.retryConnection();
				}
			}.start();
		}

		// Must be called by concrete class-->
		// contInitialization(portNumber);

	}

	public String getMessagingName() {
		return myName;
	}

	/**
	 * This method must be called by concrete class. Start various threads necessary to maintain this job.
	 */
	protected final void contInitialization() {
		browserLauncher.startBrowser(getContent().getURI().toASCIIString());

		Timer timer = new Timer("DisplayDaemons");

		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				try {
					if (statusUpdatePeriod-- <= 0) {
						updateMyStatus();
						statusUpdatePeriod = STATUS_UPDATE_PERIOD;
					}
				} catch (Exception e) {
					println(DisplayControllerMessagingAbstract.class,
							" !! Exception caught in status update thread, " + e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		}, 20000L, 1000L);

		timer.scheduleAtFixedRate(new TimerTask() {
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
					println(DisplayControllerMessagingAbstract.this.getClass(),
							" !! Exception caught in diagnostic thread, " + e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		}, FIFTEEN_MINUTES, ONE_HOUR);

		Runtime.getRuntime().addShutdownHook(new Thread("ConnectionShutdownHook") {
			public void run() {
				println(DisplayControllerMessagingAbstract.this.getClass(), "Exit hook called.");
				keepGoing = false;
				offLine = true;
				updateMyStatus();

				endAllConnections();
			}
		});
	}

	/**
	 * Update my status to the database
	 */
	protected final synchronized void updateMyStatus() {
		Connection connection;
		cpuUsage = PerformanceMonitor.getCpuUsage();
		DecimalFormat dd = new DecimalFormat("#.##");

		try {
			connection = ConnectionToDatabase.getDbConnection();

			String statementString = "";
			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {
					Date dNow = new Date();
					SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String statusString = getStatusString();
					if (offLine)
						statusString = OFF_LINE;
					statusString = "V" + versionInfo.getVersionString() + " L" + dd.format(cpuUsage) + " " + statusString;
					statusString = statusString.replace("'", "").replace("\"", "").replace(";", "").replace("\\", "");
					if (statusString.length() > 255)
						// The Content field is limited to 255 characters.
						statusString = statusString.substring(0, 249) + " ...";

					String contentName = getContent().getName().replace("'", "").replace("\"", "").replace(";", "").replace("\\",
							"");

					// Create the stream of the current content object
					String blob = convertObjectToHexBlob(getContent());

					statementString = "UPDATE DisplayStatus set Time='" + ft.format(dNow) + "',Content='" + statusString
							+ "',ContentName='" + contentName + "', SignageContent=x'" + blob + "' where DisplayID="
							+ getDBDisplayNumber();
					String succinctString = "Time='" + ft.format(dNow) + "',Content='" + statusString + "',ContentName='"
							+ contentName + "', SignageContent=x'...' where DisplayID=" + getDBDisplayNumber();

					// System.out.println(getClass().getSimpleName()+ ".updateMyStatus(): query=" + statementString);
					int numRows = stmt.executeUpdate(statementString);
					if (numRows == 0 || numRows > 1) {
						println(getClass(),
								" screen " + screenNumber
										+ ": Problem while updating status of Display: Expected to modify exactly one row, but  modified "
										+ numRows + " rows instead. SQL='" + statementString + "'");
					}
					stmt.close();
					if (statusUpdatePeriod > 0) {
						println(getClass(),
								".updateMyStatus() screen " + screenNumber + " Status: \n            " + succinctString);
						statusUpdatePeriod = STATUS_UPDATE_PERIOD;
					}
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

	@Override
	protected void respondToContentChange(boolean b, String why) {
		// generate a reply message to the sender
		println(getClass(), " -- Responding to channel change with " + (b ? "SUCCESS" : "failure"));
		MessageCarrier msg = null;
		if (b) {
			ChangeChannelReply replyMessage = new ChangeChannelReply();
			ChannelSpec channelSpec = new ChannelSpec(getContent());
			replyMessage.setChannelSpec(channelSpec);
			replyMessage.setDate("" + new Date());
			replyMessage.setDisplayNum(getDBDisplayNumber());

			try {
				String xmlMessage = MyXMLMarshaller.getXML(replyMessage);
				msg = MessageCarrier.getReplyMessage(messagingClient.getName(), messagingClient.getLastFrom(), xmlMessage);
			} catch (JAXBException e) {
				e.printStackTrace();
			}
		} else {
			msg = MessageCarrier.getErrorMessage(messagingClient.getName(), messagingClient.getLastFrom(),
					"Channel change not successful: " + why);
		}

		messagingClient.sendMessage(msg);

	}

	/**
	 * Read the database to figure out what this display is supposed to do
	 * 
	 * @param clazz
	 *            The type of object to instantiate here
	 * @return The object that is the display controller.
	 */
	protected static DisplayControllerMessagingAbstract makeTheDisplays(Class<?> clazz) {
		dynamic = true;
		DisplayControllerMessagingAbstract d = null;
		String myNode = "localhost";
		try {
			myNode = InetAddress.getLocalHost().getCanonicalHostName().replace(".dhcp.", ".");
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
		}

		String query = "SELECT * FROM Display where IPName='" + myNode + "'";

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

								int tickerCode = rs.getInt("LocationCode");
								String t = rs.getString("Type");
								SignageType type = SignageType.valueOf(t);
								String location = rs.getString("Location");
								String colorString = rs.getString("ColorCode");
								Color color = new Color(Integer.parseInt(colorString, 16));

								boolean showNumber = rs.getInt("Port") == 0;
								int screenNumber = rs.getInt("ScreenNumber");
								int channelNumber = rs.getInt("Content");
								SignageContent cont = getChannelFromNumber(channelNumber);

								boolean badNUC = rs.getInt("BadNUC") == 1;

								// stmt.close();
								// rs.close();

								// Now create the class object

								Constructor<?> cons;
								try {
									cons = clazz.getConstructor(String.class, int.class, int.class, int.class, boolean.class,
											String.class, Color.class, SignageType.class);
									d = (DisplayControllerMessagingAbstract) cons.newInstance(new Object[] { myName, vNumber,
											dbNumber, screenNumber, showNumber, location, color, type });
									d.setContentBypass(cont);
									d.setWrapperType(WrapperType.getWrapperType(tickerCode));
									d.setBadNuc(badNUC);
									d.initiate();
									// return d;
								} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InstantiationException
										| IllegalArgumentException | InvocationTargetException e) {
									e.printStackTrace();
									System.err.println("Here are the arguments: " + myName + ", " + vNumber + ", " + dbNumber + ", "
											+ screenNumber + ", " + (showNumber ? "ShowNum" : "HideNum") + ", " + location + ", "
											+ color + ", " + type);
								}
								// Allow it to loop over multiple displays
								// return null;
							} catch (Exception e) {
								e.printStackTrace();
							}

							rs.next();
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

	private void setBadNuc(boolean badNUC) {
		this.badNUC = badNUC;
	}

	private static DisplayControllerMessagingAbstract failSafeVersion(Class<?> clazz) {
		Constructor<?> cons;
		try {
			cons = clazz.getConstructor(String.class, int.class, int.class, int.class, String.class, Color.class,
					SignageType.class);
			DisplayControllerMessagingAbstract d = (DisplayControllerMessagingAbstract) cons
					.newInstance(new Object[] { "Failsafe Display", 99, 0, 0, "Failsafe location", Color.red, SignageType.XOC });
			d.initiate();
			d.setContentBypass(makeEmptyChannel(null));

			// TODO -- Change this default content to be able to handle a default LIST of channels.
			return d;
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InstantiationException
				| IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Ask for a status update to the database
	 */
	public void resetStatusUpdatePeriod() {
		statusUpdatePeriod = 0;
	}

	protected class MessagingClientLocal extends MessagingClient {
		private boolean		debug	= true;
		private DCProtocol	dcp		= new DCProtocol();
		private Thread		myShutdownHook;
		private String		lastFrom;

		// private int myDisplayNumber, myScreenNumber;

		public MessagingClientLocal(String server, int port, String username) { // , int myDisplayNumber, int myScreenNumber) {
			super(server, port, username);
			dcp.addListener(DisplayControllerMessagingAbstract.this);
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
		public void displayLogMessage(final String msg) {
			System.out.println(new Date() + " (" + getName() + "): " + msg);
		}

		@Override
		public void connectionAccepted() {
			displayLogMessage("Connection accepted at " + (new Date()));
		}

		@Override
		public void receiveIncomingMessage(MessageCarrier msg) throws ErrorProcessingMessage {
			// TODO -- Should this method be synchronized? Does it make sense to have two messages processed at the same time, or
			// not?

			// if (debug && msg.getType() != MessageType.ISALIVE)
			println(this.getClass(), screenNumber + ":" + MessagingClientLocal.class.getSimpleName()
					+ ".displayIncomingMessage(): Got this message:\n[" + msg + "]");
			lastFrom = msg.getFrom();
			if (msg.getTo().equals(getName())) { // || msg.getTo().startsWith(getName())) {
				if (!dcp.processInput(msg)) {
					sendMessage(MessageCarrier.getErrorMessage(msg.getTo(), msg.getFrom(), dcp.getErrorMessageText()));
				}
			} else if (debug)
				println(this.getClass(), screenNumber + ": Ignoring a message of type " + msg.getType() + ", sent to ["
						+ msg.getTo() + "] because I am [" + getName() + "]");
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

					println(DisplayAsConnectionToFireFox.class, ": Retrieved a channel from the file system: [" + content + "]");

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
					+ "' There is no recovery channel information available in this file");
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		(new File(filename)).delete(); // Make sure this file is deleted now that the object stream has been used.

		ExitHandler.removeFinalCommand(lastCommand);
		lastCommand = ExitHandler.addFinalCommand(new Command() {

			@Override
			public void execute() {
				try {
					SignageContent c = getContent();
					if (specialURI(c))
						c = previousChannel;
					if (c == null)
						return;
					try (FileOutputStream fout = new FileOutputStream(filename)) {
						try (ObjectOutputStream oos = new ObjectOutputStream(fout)) {
							oos.writeObject(c);
						}
					}
					println(DisplayAsConnectionToFireFox.class, ": Saved a channel to the file system: [" + getContent() + "]");

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

}
