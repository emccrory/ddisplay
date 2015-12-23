package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.getMessagingServerName;
import static gov.fnal.ppd.dd.util.Util.makeEmptyChannel;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.chat.DCProtocol;
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.chat.MessageType;
import gov.fnal.ppd.dd.chat.MessagingClient;
import gov.fnal.ppd.dd.display.DisplayImpl;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.util.version.VersionInformation;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implement a Dynamic Display using an underlying browser through a simple messaging server
 * 
 * @author Elliott McCrory, Fermilab (2014)
 */
public abstract class DisplayControllerMessagingAbstract extends DisplayImpl {

	protected static final int		STATUS_UPDATE_PERIOD	= 30;
	protected static final long		SOCKET_ALIVE_INTERVAL	= 2500l;
	protected static final long		SHOW_SPLASH_SCREEN_TIME	= 15000l;

	private boolean					offLine					= false;

	protected static final String	OFF_LINE				= "Off Line";

	protected BrowserLauncher		browserLauncher;

	// protected static int number;
	protected boolean				keepGoing				= true;
	protected SignageContent		lastChannel				= null;
	protected SignageContent		revertToThisChannel		= null;
	protected static boolean		dynamic					= false;

	protected MessagingClient		messagingClient;
	private String					myName;
	private int						statusUpdatePeriod		= 10;
	protected boolean				showNumber				= true;
	protected VersionInformation	versionInfo				= VersionInformation.getVersionInformation();

	// Use messaging to get change requests from the changers -->
	// private boolean actAsServerNoMessages = true;

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
		myName = ipName + ":" + screenNumber + " (" + getVirtualDisplayNumber() + ")";
		messagingClient = new MessagingClientLocal(getMessagingServerName(), MESSAGING_SERVER_PORT, myName);
		// , getVirtualDisplayNumber(), getScreenNumber());
	}

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

	protected abstract void endAllConnections();

	public String getMessagingName() {
		return myName;
	}

	/**
	 * This method must be called by concrete class. Start various threads necessary to maintain this job.
	 */
	protected final void contInitialization() {
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
							+ new Date(((MessagingClientLocal) messagingClient).getServerTimeStamp());
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
				System.err.println("Exit hook called."); // This does not actually get printed.
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
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();

			String statementString = "";
			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {
					Date dNow = new Date();
					SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String statusString = getStatusString();
					if (offLine)
						statusString = "Off Line";
					statusString = "V" + versionInfo.getVersionString() + " " + statusString;

					statementString = "UPDATE DisplayStatus set Time='" + ft.format(dNow) + "',Content='" + statusString
							+ "' where DisplayID=" + getDBDisplayNumber();

					// System.out.println(getClass().getSimpleName()+ ".updateMyStatus(): query=" + statementString);
					int numRows = stmt.executeUpdate(statementString);
					if (numRows == 0 || numRows > 1) {
						println(getClass(), " screen " + screenNumber
								+ ": Problem while updating status of Display: Expected to modify exactly one row, but  modified "
								+ numRows + " rows instead. SQL='" + statementString + "'");
					}
					stmt.close();
					if (statusUpdatePeriod > 0) {
						println(getClass(), ".updateMyStatus() screen " + screenNumber + " Status: \n            "
								+ statementString.substring("UPDATE DisplayStatus set ".length()));
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

	protected abstract String getStatusString();

	public final void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		localSetContent();
	}

	static DisplayControllerMessagingAbstract makeTheDisplays(Class<?> clazz) {
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
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
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

								System.out.println("\n********** Display number " + (displayCount++) + ", name=" + myName
										+ " **********\n");

								if (!myName.equals(myNode)) {
									// TODO This will not work if the IPName in the database is an IP address.
									System.out
											.println("The node name of this display, according to the database, is supposed to be '"
													+ myName
													+ "', but InetAddress.getLocalHost().getCanonicalHostName() says it is '"
													+ myNode + "'\n\t** We'll try to run anyway, but this should be fixed **");
									// System.exit(-1);
								}
								int dbNumber = rs.getInt("DisplayID");
								int vNumber = rs.getInt("VirtualDisplayNumber");

								System.out.println("The node name of this display (no. " + vNumber + "/" + dbNumber + ") is '"
										+ myNode + "'");

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
									d.initiate();
									// return d;
								} catch (NoSuchMethodException | SecurityException | IllegalAccessException
										| InstantiationException | IllegalArgumentException | InvocationTargetException e) {
									e.printStackTrace();
									System.err.println("Here are the arguments: " + myName + ", " + vNumber + ", " + dbNumber
											+ ", " + screenNumber + ", " + (showNumber ? "ShowNum" : "HideNum") + ", " + location
											+ ", " + color + ", " + type);
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

	protected abstract void setWrapperType(WrapperType wrapperType);

	private static DisplayControllerMessagingAbstract failSafeVersion(Class<?> clazz) {
		Constructor<?> cons;
		try {
			cons = clazz
					.getConstructor(String.class, int.class, int.class, int.class, String.class, Color.class, SignageType.class);
			DisplayControllerMessagingAbstract d = (DisplayControllerMessagingAbstract) cons.newInstance(new Object[] {
					"Failsafe Display", 99, 0, 0, "Failsafe location", Color.red, SignageType.XOC });
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
	 * @param channelNumber
	 *            The channel number to look up
	 * @return The channel that is specified by this channel number
	 */
	public static SignageContent getChannelFromNumber(int channelNumber) {
		Channel retval = null;
		try {
			retval = new ChannelImpl("Fermilab", ChannelCategory.PUBLIC, "Fermilab", new URI("http://www.fnal.gov"), 0, 360000L);
		} catch (URISyntaxException e2) {
			e2.printStackTrace();
		}
		;

		if (channelNumber == 0) {
			return retval;
		} else if (channelNumber < 0) {
			// This is a list of channels

			// FIXME -- This does NOT work because the management of traversing a channel list resides in the chat client
			// that receives the channel list. Either we have to give this list to the chat client (not sure how THAT would work)
			// or implement the traversing of the channel list lower down (which is a suggestion that can be seen elsewhere
			// in the code, DCProtocol.informListenersForever()).

			String query = "select Channel.Number as Number,Dwell,Name,Description,URL from Channel,ChannelList where ListNumber="
					+ (-channelNumber) + " and Channel.Number=ChannelList.Number ORDER BY SequenceNumber";

			println(DisplayControllerMessagingAbstract.class, " -- Getting default channel list: [" + query + "]");
			Connection connection;
			try {
				connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				return null;
			}

			List<SignageContent> channelList = new ArrayList<SignageContent>();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement();) {
					try (ResultSet rs = stmt.executeQuery(query);) {
						if (!rs.first()) {
							return retval;
						}
						do {
							int chanNum = rs.getInt("Number");
							int dwell = rs.getInt("Dwell");
							String name = rs.getString("Name");
							String desc = rs.getString("Description");
							String url = rs.getString("URL");

							Channel c = new ChannelImpl(name, ChannelCategory.PUBLIC, desc, new URI(url), chanNum, dwell);
							channelList.add(c);

						} while (rs.next());

						retval = new ChannelPlayList(channelList, 60000L);
						stmt.close();
						rs.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (SQLException ex) {
					System.err.println("It is likely that the DB server is down.  We'll try again later.");
					ex.printStackTrace();
				}
			}
		} else {
			// Channel number is positive!
			// String query = "SELECT * from ChannelList where ListNumber=" + (-channelNumber) + " ORDER BY SequenceNumber";
			String query = "SELECT * from Channel where Number=" + channelNumber;
			println(DisplayControllerMessagingAbstract.class, " -- Getting default channel list: [" + query + "]");
			Connection connection;
			try {
				connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				return null;
			}

			synchronized (connection) {
				try (Statement stmt = connection.createStatement();) {
					try (ResultSet rs = stmt.executeQuery(query);) {
						if (rs.first()) { // Move to first returned row (there should only be one)
							String url = rs.getString("URL");
							int dwell = rs.getInt("DwellTime");
							String desc = rs.getString("Description");
							String name = rs.getString("Name");

							retval = new ChannelImpl(name, ChannelCategory.PUBLIC, desc, new URI(url), channelNumber, dwell);

							stmt.close();
							rs.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (SQLException ex) {
					System.err.println("It is likely that the DB server is down.  We'll try again later.");
					ex.printStackTrace();
				}
			}
		}

		return retval;
	}

	/**
	 * Ask for a status update to the database
	 */
	public void resetStatusUpdatePeriod() {
		statusUpdatePeriod = 0;
	}

	private class MessagingClientLocal extends MessagingClient {
		private boolean		debug	= true;
		private DCProtocol	dcp		= new DCProtocol();
		private Thread		myShutdownHook;

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

		public long getServerTimeStamp() {
			return lastMessageReceived;
		}

		@Override
		public void connectionAccepted() {
			displayLogMessage("Connection accepted at " + (new Date()));
		}

		@Override
		public void receiveIncomingMessage(MessageCarrier msg) {
			if (debug && msg.getType() != MessageType.ISALIVE)
				println(this.getClass(), screenNumber + ":" + MessagingClientLocal.class.getSimpleName()
						+ ".displayIncomingMessage(): Got this message:\n[" + msg + "]");
			if (msg.getTo().equals(getName())) { // || msg.getTo().startsWith(getName())) {
				dcp.processInput(msg);
			} else if (debug)
				println(this.getClass(),
						screenNumber + ": Ignoring a message of type " + msg.getType() + ", sent to [" + msg.getTo()
								+ "] because I am [" + getName() + "]");
		}
	}
}
