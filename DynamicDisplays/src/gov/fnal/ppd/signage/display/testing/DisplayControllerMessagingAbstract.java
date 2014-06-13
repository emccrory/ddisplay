package gov.fnal.ppd.signage.display.testing;

import gov.fnal.ppd.chat.MessagingClient;
import gov.fnal.ppd.signage.SignageDatabaseNotVisibleException;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.signage.comm.DCProtocol;
import gov.fnal.ppd.signage.comm.DDMessage;
import gov.fnal.ppd.signage.display.DisplayImpl;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
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
	private static String			webServerNode			= System.getProperty("signage.webserver", "mccrory.fnal.gov");
	private static String			messagingServerNode		= webServerNode;
	private static int				messagingServerPort		= 1500;

	protected static final long		STATUS_UPDATE_PERIOD	= 60000l;
	protected static final long		SOCKET_ALIVE_INTERVAL	= 2500l;
	protected static final long		SHOW_SPLASH_SCREEN_TIME	= 15000l;

	private String					nowShowing				= "Startup Splash Screen";

	protected static final String	OFF_LINE				= "Off Line";
	protected static final String	IDENTIFY_URL			= "http://" + webServerNode + "/XOC/displaySplash.php?id=";
	protected static final String	SELF_IDENTIFY			= "http://identify";

	protected static Connection		connection;

	protected BrowserLauncher		browserLauncher;

	protected static int			number;
	protected boolean				keepGoing				= true;
	protected String				lastURL					= "http://xoc.fnal.gov";
	protected static boolean		dynamic					= false;

	private MessagingClient			messagingClient;
	private String					myName;

	// / Use messaging to get change requests from the changers -->
	// private boolean actAsServerNoMessages = true;

	/**
	 * @param portNumber
	 * @param ipName
	 * @param displayID
	 * @param screenNumber
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayControllerMessagingAbstract(String ipName, int displayID, int screenNumber, int portNumber, String location,
			Color color, SignageType type) {
		super(ipName, displayID, screenNumber, location, color, type);

		if (getContent() == null)
			throw new IllegalArgumentException("No content defined!");

		myName = ipName + ":" + screenNumber + " (" + number + ")";
		messagingClient = new MessagingClientLocal(messagingServerNode, messagingServerPort, myName);
		messagingClient.start();

		// Must be called by concrete class-->
		// contInitialization(portNumber);
	}

	protected abstract void endAllConnections();

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
					try {
						sleep(STATUS_UPDATE_PERIOD);
						updateMyStatus();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.err.println("Exit hook called.");
				keepGoing = false;
				nowShowing = OFF_LINE;
				// updateMyStatus(); This is odd--it was deleting the rows for this Display in Display and DisplayStatus tables.
				// (5/24/14)

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
		Statement stmt = null;
		// ResultSet rs = null;

		try {
			stmt = connection.createStatement();
			stmt.executeQuery("USE xoc");
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		}

		try {
			Date dNow = new Date();
			SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			// String statementString = "UPDATE DisplayStatus set Time='" + ft.format(dNow) + "',Content='" + getStatus()
			// + "',ContentName='" + getContent().getName() + "' where DisplayID=" + number;
			// TODO Give xocuser the rights to change ContentName in this table!
			String statementString = "UPDATE DisplayStatus set Time='" + ft.format(dNow) + "',Content='" + getStatus()
					+ "' where DisplayID=" + number;
			int numRows = stmt.executeUpdate(statementString);
			if (numRows == 0 || numRows > 1) {
				System.err.println("Problem while updating status of Display: Expected to modify exactly one row, but  modified "
						+ numRows + " rows instead. SQL='" + statementString + "'");
			}

			if (dynamic && OFF_LINE.equalsIgnoreCase(nowShowing)) {
				// When we are all done, remove this new record from the Display and the DisplayStatus tables
				stmt.executeUpdate("DELETE FROM Display WHERE DisplayID=" + number);
				stmt.executeUpdate("DELETE FROM DisplayStatus WHERE DisplayID=" + number);
			}
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		} catch (SignageDatabaseNotVisibleException e1) {
			e1.printStackTrace();
			System.err.println("\nNo connection to the Signage/Displays database.");
			System.exit(-1);
		}

		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = connection.createStatement();
			rs = stmt.executeQuery("USE xoc");
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		}

		try {
			rs = stmt.executeQuery(query);

			if (rs.first()) { // Move to first returned row (there should only be one)
				String myName = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("IPName"));

				if (!myName.equals(myNode)) {
					// TODO This will not work if the IPName in the database is an IP address.
					System.out.println("The node name of this display (no. " + number
							+ "), according to the database, is supposed to be '" + myName
							+ "', but InetAddress.getLocalHost().getCanonicalHostName() says it is '" + myNode
							+ "'\n\t** We'll try to run anyway, but this should be fixed **");
					// System.exit(-1);
				}
				number = rs.getInt("DisplayID");

				System.out.println("The node name of this display (no. " + number + ") is '" + myNode + "'");

				String t = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Type"));
				SignageType type = SignageType.valueOf(t);
				String location = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Location"));
				String colorString = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("ColorCode"));
				Color color = new Color(Integer.parseInt(colorString, 16));
				int portNumber = rs.getInt("Port");
				int screenNumber = rs.getInt("ScreenNumber");
				// String positionString = rs.getString("Position");
				// if (positionString == null)

				stmt.close();
				rs.close();

				// Now create the class object

				Constructor<?> cons;
				try {
					cons = clazz.getConstructor(String.class, int.class, int.class, int.class, String.class, Color.class,
							SignageType.class);
					return (DisplayControllerMessagingAbstract) cons.newInstance(new Object[] { myName, number, screenNumber,
							portNumber, location, color, type });
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InstantiationException
						| IllegalArgumentException | InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			} else {
				new Exception("No database rows returned for query, '" + query + "'").printStackTrace();
				System.err.println("\n** Cannot continue! **\nExit");
				connection.close();
				System.exit(-1);
			}

			System.err.println("Cannot create an instance of DisplayAsStandaloneBrowser!");
			if (connection != null)
				connection.close();
			System.exit(-1);

		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.err.println("Control passed to a place that is impossible to reach!");
		return null;
	}

	private class MessagingClientLocal extends MessagingClient {
		private boolean		debug	= false;
		private DCProtocol	dcp		= new DCProtocol();

		public MessagingClientLocal(String server, int port, String username) {
			super(server, port, username);
			dcp.addListener(DisplayControllerMessagingAbstract.this);
		}

		@Override
		public void displayIncomingMessage(String msg) {
			if (debug)
				System.out.println("Got this message: [" + msg + "]");
			if (msg.startsWith(myName)) {
				if (debug)
					System.out.println("This message is for me!");

				// TODO Interpret the XML document I just got and then set the content, appropriately.
				String xmlDocument = msg.substring(msg.indexOf("<?xml"));
				DDMessage myMessage = new DDMessage(xmlDocument);
				// TODO Make this message affect the URL that is being shown
				// I think we need to use the DCProtocol class here.
				dcp.processInput(myMessage);
			}
		}
	}
}