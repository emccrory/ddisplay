package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.util.Util.*;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.chat.DCProtocol;
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.chat.MessagingClient;
import gov.fnal.ppd.dd.display.DisplayImpl;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Implement a Dynamic Display using an underlying browser through a simple messaging server
 * 
 * @author Elliott McCrory, Fermilab (2014)
 */
public abstract class DisplayControllerMessagingAbstract extends DisplayImpl {

	protected static final int		STATUS_UPDATE_PERIOD	= 30;
	protected static final long		SOCKET_ALIVE_INTERVAL	= 2500l;
	protected static final long		SHOW_SPLASH_SCREEN_TIME	= 15000l;

	private String					nowShowing				= "Startup Splash Screen";

	protected static final String	OFF_LINE				= "Off Line";

	protected static Connection		connection;

	protected BrowserLauncher		browserLauncher;

	// protected static int number;
	protected boolean				keepGoing				= true;
	protected SignageContent		lastChannel				= null;
	protected static boolean		dynamic					= false;

	private MessagingClient			messagingClient;
	private String					myName;
	private boolean					doTheHeartbeatThing		= false;
	private int						statusUpdatePeriod		= 10;

	// / Use messaging to get change requests from the changers -->
	// private boolean actAsServerNoMessages = true;

	/**
	 * @param portNumber
	 * @param ipName
	 * @param displayNumber
	 * @param screenNumber
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayControllerMessagingAbstract(final String ipName, final int displayNumber, final int screenNumber,
			final int portNumber, final String location, final Color color, final SignageType type) {
		super(ipName, displayNumber, screenNumber, location, color, type);

		if (getContent() == null)
			throw new IllegalArgumentException("No content defined!");

		myName = ipName + ":" + screenNumber + " (" + getNumber() + ")";
		messagingClient = new MessagingClientLocal(MESSAGING_SERVER_NAME, MESSAGING_SERVER_PORT, myName, getNumber(), getScreenNumber());
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

		if (doTheHeartbeatThing)
			new Thread("IsMyServerAlive") {
				public void run() {
					while (true) {
						catchSleep(FIFTEEN_MINUTES);
						if (((MessagingClientLocal) messagingClient).getServerTimeStamp() + 2 * FIFTEEN_MINUTES < System
								.currentTimeMillis()) {
							System.err.println("It looks like the server is down! Let's try to restart our connection to it.");
							messagingClient.disconnect();
							messagingClient = null; // Not sure about this.
							messagingClient = new MessagingClientLocal(MESSAGING_SERVER_NAME, MESSAGING_SERVER_PORT, myName,
									getNumber(), getScreenNumber());
							messagingClient.start();
						}
					}
				}
			}.start();
	}

	protected abstract void endAllConnections();

	public String getMessagingName() {
		return myName;
	}

	/**
	 * This method must be called by concrete class
	 * 
	 * @param portNumber
	 *            Port number on which to listen
	 */
	protected final void contInitialization(final int portNumber) {

		new Thread("StatusThread") {
			public void run() {
				while (keepGoing) {
					catchSleep(1000L);
					if (statusUpdatePeriod-- <= 0) {
						updateMyStatus();
						statusUpdatePeriod = STATUS_UPDATE_PERIOD;
					}
				}
			}
		}.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.err.println("Exit hook called.");
				keepGoing = false;
				nowShowing = OFF_LINE;
				updateMyStatus();

				endAllConnections();
			}
		});

		// if (actAsServerNoMessages) {
		// new Thread("ADisplayServerDaemon") {
		// public void run() {
		// DCMulitServerThread server = null;
		// while (keepGoing) {
		// if (server == null || !server.isAlive())
		// try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
		// server = null;
		// System.gc();
		// System.out.println("Listening on port " + portNumber);
		// server = new DCMulitServerThread(serverSocket.accept()); // This accept() call blocks
		// server.addListener(DisplayControllerMessagingAbstract.this);
		// server.start();
		// } catch (IOException e) {
		// System.err.println("Could not listen on port " + portNumber);
		// System.exit(-1);
		// }
		// try {
		// // Make sure the serverSocket is alive, continually
		// sleep(SOCKET_ALIVE_INTERVAL);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// }
		// }
		// }.start();
		// }
	}

	/**
	 * Update my status to the database
	 */
	protected final synchronized void updateMyStatus() {

		try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {
			Date dNow = new Date();
			SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String statementString;
			if (OFF_LINE.equalsIgnoreCase(nowShowing))
				statementString = "UPDATE DisplayStatus set Time='" + ft.format(dNow) + "',Content='Off Line' where DisplayID="
						+ getNumber();
			else
				statementString = "UPDATE DisplayStatus set Time='" + ft.format(dNow) + "',Content='" + getStatus() + " ("
						+ getContent().getURI() + ")" + "' where DisplayID=" + getNumber();
			// System.out.println(getClass().getSimpleName()+ ".updateMyStatus(): query=" + statementString);
			int numRows = stmt.executeUpdate(statementString);
			if (numRows == 0 || numRows > 1) {
				System.err.println("Problem while updating status of Display: Expected to modify exactly one row, but  modified "
						+ numRows + " rows instead. SQL='" + statementString + "'");
			}
			stmt.close();
			if (statusUpdatePeriod > 0) {
				System.out.println(getClass().getSimpleName() + ".updateMyStatus(): Status: \n            "
						+ statementString.substring("UPDATE DisplayStatus set ".length()));
				statusUpdatePeriod = STATUS_UPDATE_PERIOD;
			}
		} catch (SQLException ex) {
			System.err.println("It is likely that the DB server is down.  We'll try again later.");
			ex.printStackTrace();
			return;
		}
	}

	public final void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		localSetContent();
	}

	// Static methods to create an instance of the class

	/**
	 * Look up all the information from the database using the number as the DisplayID
	 * 
	 * @param number
	 *            The display number this instance is
	 * @param clazz
	 * @return The object that deals with this display
	 */
	public static DisplayControllerMessagingAbstract makeDynamicDisplayByNumber(final int number, Class<?> clazz) {
		try {
			String query = "SELECT * FROM Display where DisplayID=" + number;
			InetAddress ip = InetAddress.getLocalHost();

			String myNode = ip.getCanonicalHostName();

			return makeADisplay(myNode, query, clazz);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param screen
	 *            This screen number
	 * @param clazz
	 * @return The Dynamic Display object
	 */
	public static DisplayControllerMessagingAbstract makeDynamicDisplayByScreen(int screen, Class<?> clazz) {
		try {
			InetAddress ip = InetAddress.getLocalHost();

			String myNode = ip.getCanonicalHostName();

			return makeADisplay(myNode, "SELECT * FROM Display where IPName='" + myNode + "' AND ScreenNumber=" + screen, clazz);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static DisplayControllerMessagingAbstract makeADisplay(String myNode, String query, Class<?> clazz) {
		dynamic = true;

		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		} catch (DatabaseNotVisibleException e1) {
			e1.printStackTrace();
			System.err.println("\nNo connection to the Signage/Displays database.");
			return failSafeVersion(clazz);
		}

		// Use ARM to simplify these try blocks.
		try (Statement stmt = connection.createStatement();) {
			try (ResultSet rs = stmt.executeQuery("USE " + DATABASE_NAME)) {
			}

			try (ResultSet rs = stmt.executeQuery(query);) {
				if (rs.first())
					try { // Move to first returned row (there should only be one)
						String myName = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("IPName"));

						if (!myName.equals(myNode)) {
							// TODO This will not work if the IPName in the database is an IP address.
							System.out.println("The node name of this display, according to the database, is supposed to be '"
									+ myName + "', but InetAddress.getLocalHost().getCanonicalHostName() says it is '" + myNode
									+ "'\n\t** We'll try to run anyway, but this should be fixed **");
							// System.exit(-1);
						}
						int number = rs.getInt("DisplayID");

						System.out.println("The node name of this display (no. " + number + ") is '" + myNode + "'");

						String t = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Type"));
						SignageType type = SignageType.valueOf(t);
						String location = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Location"));
						String colorString = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("ColorCode"));
						Color color = new Color(Integer.parseInt(colorString, 16));
						// TODO Also get the color name from the DB
						int portNumber = rs.getInt("Port");
						int screenNumber = rs.getInt("ScreenNumber");
						int channelNumber = rs.getInt("Content");
						String url = getURLFromNumber(channelNumber);
						// String positionString = rs.getString("Position");
						// if (positionString == null)

						stmt.close();
						rs.close();

						// Now create the class object

						Constructor<?> cons;
						try {
							cons = clazz.getConstructor(String.class, int.class, int.class, int.class, String.class, Color.class,
									SignageType.class);
							DisplayControllerMessagingAbstract d = (DisplayControllerMessagingAbstract) cons
									.newInstance(new Object[] { myName, number, screenNumber, portNumber, location, color, type });
							d.initiate();
							d.setDefaultContent(url);
							return d;
						} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InstantiationException
								| IllegalArgumentException | InvocationTargetException e) {
							e.printStackTrace();
						}
						return null;
					} catch (Exception e) {
						e.printStackTrace();
					}
				else {
					new Exception("No database rows returned for query, '" + query + "'").printStackTrace();
					System.err.println("\n** Cannot continue! **\nExit");
					connection.close();
					return failSafeVersion(clazz);
				}

			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			return failSafeVersion(clazz);
		}

		System.err.println("Control passed to a place that is impossible to reach!");
		return null;
	}

	private static DisplayControllerMessagingAbstract failSafeVersion(Class<?> clazz) {
		Constructor<?> cons;
		try {
			cons = clazz
					.getConstructor(String.class, int.class, int.class, int.class, String.class, Color.class, SignageType.class);
			DisplayControllerMessagingAbstract d = (DisplayControllerMessagingAbstract) cons.newInstance(new Object[] {
					"Failsafe Display", 99, 0, 0, "Failsafe location", Color.red, SignageType.XOC });
			d.initiate();
			d.setDefaultContent(makeEmptyChannel().getURI().toURL().toString());
			return d;
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InstantiationException
				| IllegalArgumentException | InvocationTargetException | MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void setDefaultContent(String url) {
		setChannel((Channel) makeEmptyChannel());
	}

	private static String getURLFromNumber(int channelNumber) {
		String query = "SELECT URL from Channel where Number=" + channelNumber;
		String retval = "http://www.fnal.gov";
		try (Statement stmt = connection.createStatement();) {
			try (ResultSet rs = stmt.executeQuery(query);) {
				if (rs.first()) { // Move to first returned row (there should only be one)
					retval = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("URL"));

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
		private int			myDisplayNumber, myScreenNumber;

		public MessagingClientLocal(String server, int port, String username, int myDisplayNumber, int myScreenNumber) {
			super(server, port, username);
			dcp.addListener(DisplayControllerMessagingAbstract.this);
			this.myDisplayNumber = myDisplayNumber;
			this.myScreenNumber = myScreenNumber;
		}

		public long getServerTimeStamp() {
			return dcp.getLastServerHeartbeat();
		}

		@Override
		public void connectionAccepted() {
			displayLogMessage("Connection accepted at " + (new Date()));
		}

		@Override
		public void displayIncomingMessage(MessageCarrier msg) {
			if (debug)
				System.out.println(DisplayControllerMessagingAbstract.class.getSimpleName() + ":"
						+ MessagingClientLocal.class.getSimpleName() + ".displayIncomingMessage(): Got this message:\n[" + msg
						+ "]");
			if (msg.getTo().equals(getName())) {
				dcp.processInput(msg, myDisplayNumber, myScreenNumber);
			} else if (debug)
				System.out.println("Ignoring a message from [" + msg.getTo() + "] because I am [" + getName() + "]");
		}
	}
}
