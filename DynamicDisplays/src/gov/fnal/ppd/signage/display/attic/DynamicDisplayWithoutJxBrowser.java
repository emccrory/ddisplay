package gov.fnal.ppd.signage.display.attic;

import gov.fnal.ppd.signage.DatabaseNotVisibleException;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.signage.comm.attic.DCMulitServerThread;
import gov.fnal.ppd.signage.display.DisplayImpl;
import gov.fnal.ppd.signage.display.ScreenLayoutInterpreter;
import gov.fnal.ppd.signage.display.client.BrowserLauncher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Dynamic Display server task for listening to a port for the URI (really, a URL) to display on a free-standing browser.
 * 
 * ** This needs more work (mostly simplification!!) **
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2013-14
 * 
 */
public class DynamicDisplayWithoutJxBrowser extends DisplayImpl {

	protected static final String	OFF_LINE				= "Off Line";
	private static final String		SELF_IDENTIFY			= "http://identify";
	protected static final long		STATUS_UPDATE_PERIOD	= 60000l;
	private static int				number;
	private static Connection		connection;
	private boolean					keepGoing				= true;
	private AtomicInteger			borderCountdown			= new AtomicInteger(0);
	private AtomicInteger			splashCountdown			= new AtomicInteger(0);
	private Thread					borderThread;

	private JPanel					mainPanel				= new JPanel(new BorderLayout());
	private String					lastURL;
	private String					nowShowing				= "Startup Splash Screen";
	private BrowserLauncher			browserLauncher			= null;
	private static boolean			dynamic					= false;

	/**
	 * @param location
	 * @param content
	 * @param typ
	 * @param screenNum
	 * @param colorCode
	 * @param port
	 * @return The Display
	 */
	public static DynamicDisplayWithoutJxBrowser makeADynamicDisplay(final String location, final int content, final String typ,
			final int screenNum, final String colorCode, final int port) {

		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		} catch (DatabaseNotVisibleException e1) {
			e1.printStackTrace();
		}
		DynamicDisplayWithoutJxBrowser.dynamic = true;

		// This display is not registered. Let's register it!
		try {
			final Statement stmt = connection.createStatement();
			@SuppressWarnings("unused")
			ResultSet result = stmt.executeQuery("USE xoc");

			String ipAddress = InetAddress.getLocalHost().getHostAddress();

			stmt.executeUpdate("INSERT INTO Display VALUES ('" + location + "', " + content + ", '" + typ + "', NULL, '"
					+ ipAddress + "', '" + screenNum + "', '" + colorCode + "', '" + port + "')");

			// Retrieve the display number that I am
			ResultSet rs = stmt.executeQuery("SELECT * from Display where IPName='" + ipAddress + "'");

			rs.first(); // Move to first returned row (there should only be one)
			DynamicDisplayWithoutJxBrowser.number = rs.getInt("DisplayID");
			stmt.executeUpdate("INSERT INTO DisplayStatus VALUES ('Startup', " + DynamicDisplayWithoutJxBrowser.number
					+ ", NULL, '2013-01-01 00:00:00')");

			stmt.close();
			rs.close();
			return new DynamicDisplayWithoutJxBrowser(port, true, ipAddress, screenNum, number, location, new Color(
					Integer.parseInt(colorCode, 16)), SignageType.valueOf(typ));

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			System.out.println("SQL Error code: " + e.getErrorCode());
			e.printStackTrace();

		}

		// TODO -- Maybe we should remove this display from the database when the program exits?

		return null;
	}

	/**
	 * Look up all the information from the database using the number as the DisplayID
	 */
	public static DynamicDisplayWithoutJxBrowser makeADynamicDisplay(final int number) {

		DynamicDisplayWithoutJxBrowser.number = number;
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		} catch (DatabaseNotVisibleException e1) {
			e1.printStackTrace();
			System.err.println("\nNo connection to the Signage/Displays database.  We're making stuff up now.");
			// Make something up--this is a test (obviously)
			return new DynamicDisplayWithoutJxBrowser(99999, true, "Fake Display", 0, number, "Fake location", Color.blue,
					SignageType.XOC);
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

//		try {
//			rs = stmt.executeQuery("SELECT * FROM Display where DisplayID=" + number);
//			rs.first(); // Move to first returned row (there should only be one)
//			String myName = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("IPName"));
//
//			String node = InetAddress.getLocalHost().getCanonicalHostName();
//			if (!myName.equals(node)) {
//				// TODO This will not work if the IPName in the database is an IP address.
//				System.out.println("The node name of display no. " + number + " is supposed to be '" + myName
//						+ "', but this node is called '" + node + "'\n\t** We'll try to run anyway, but this should be fixed **");
//				// System.exit(-1);
//			}
//
//			String t = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Type"));
//			SignageType type = SignageType.valueOf(t);
//			String location = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Location"));
//			String colorString = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("ColorCode"));
//			Color color = new Color(Integer.parseInt(colorString, 16));
//			int portNumber = rs.getInt("Port");
//			int screenNumber = rs.getInt("ScreenNumber");
//			// String positionString = rs.getString("Position");
//			// if (positionString == null)
//
//			stmt.close();
//			rs.close();
//			return new DynamicDisplayWithoutJxBrowser(portNumber, true, myName, screenNumber, number, location, color, type);
//			// else {
//			// String[] tokens = positionString.split("[+-]");
//			// if (tokens.length == 3) {
//			// int x = Integer.parseInt(tokens[1]);
//			// if (positionString.startsWith("-"))
//			// x = -x;
//			// int y = Integer.parseInt(tokens[2]);
//			// return new ADynamicDisplay(portNumber, true, myName, x, y, number, location, color, type);
//			// } else {
//			// System.err.println("Oops!  There are " + tokens.length + " tokens in '" + positionString + "': "
//			// + Arrays.toString(tokens));
//			// System.exit(-1);
//			// }
//			// }
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		}

		System.err.println("Cannot create an instance of ADynamicDisplay!");
		System.exit(-1);
		return null;
	}

	public DynamicDisplayWithoutJxBrowser(final int portNumber, final boolean maximized, String ipName, int screenNumber,
			int number, String location, Color color, SignageType type) {
		super(ipName, screenNumber, number, location, color, type);

		lastURL = "http://xoc.fnal.gov";
		changeURL(lastURL);
		identifyMe();

		// GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[screenNumber]
		// .getDefaultConfiguration();
		Rectangle bounds = ScreenLayoutInterpreter.getBounds(screenNumber);

		JFrame frame = new JFrame("Dynamic Display no. " + this.getNumber());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		mainPanel.add(new JLabel("Wait"), BorderLayout.CENTER);
		frame.getContentPane().add(mainPanel, BorderLayout.CENTER);

		frame.pack();
		frame.setVisible(true);

		contInitialization(portNumber);
	}

	private void contInitialization(final int portNumber) {

		new Thread("ADisplayServerDaemon") {
			public void run() {
				DCMulitServerThread server = null;
				while (keepGoing) {
					if (server == null || !server.isAlive())
						try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
							server = null;
							System.gc();
							server = new DCMulitServerThread(serverSocket.accept());
							server.addListener(DynamicDisplayWithoutJxBrowser.this);
							server.start();
						} catch (IOException e) {
							System.err.println("Could not listen on port " + portNumber);
							System.exit(-1);
						}
					try {
						// Make sure the serverSocket is alive, continually
						sleep(2500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

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
				keepGoing = false;
				nowShowing = OFF_LINE;
				updateMyStatus();
			}
		});
	}

	@Override
	protected void localSetContent() {
		String url = getContent().getURI().toASCIIString();
		if (url.equalsIgnoreCase(SELF_IDENTIFY)) {
			identifyMe();
			nowShowing = url; // Do not change the value of lastURL
		} else {
			changeURL(url);
			nowShowing = lastURL = url;
		}
		displayHasChanged();
	}

	/**
	 * Put up a colorful splash screen that identifies this Display
	 */
	private void identifyMe() {
		String colorString = Integer.toHexString(getPreferredHighlightColor().getRGB()).substring(2);
		String style1 = "font-size:500%;font-weight:bold;background-color:white;border-style:solid;border-width:25px;border-color:white;";
		String style2 = "font-size:350%;background-color:white;border-style:solid;border-width:25px;border-color:white;";
		String style3 = "font-size:150%;background-color:white;border-style:solid;border-width:25px;border-color:white;";
		final String content = "<html><body style='margin:200;background-color:#" + colorString + ";'>\n" + "<p align='center' "
				+ "style='" + style1 + "'>Display Number " + number + "</p>\n" + "<p align='center' style='" + style2
				+ "'>This is " + (getCategory() == SignageType.Experiment ? "an Experiment" : "a " + getCategory())
				+ " Display<br>\n<p align='center' style='" + style3 + "'>" + "Screen number " + getScreenNumber() + "<br>\n"
				+ "Location = '" + getLocation() + "'<br>\nColor code is #" + colorString + "</p>" + "<p><em>The web site '"
				+ lastURL + "' will be shown ";

		final String endContent = "</p></body></html>";

		nowShowing = "Self identification splash screen";

		System.out.println(content);
		splashCountdown.set(10);
		borderThread = new Thread() {
			public void run() {
				// Turn off the border in a while
				try {
					while (splashCountdown.decrementAndGet() > 0) {
						if (splashCountdown.get() != 1)
							showHTML(content + "in about " + splashCountdown + " seconds.</em>" + endContent);
						else
							showHTML(content + "in a second.</em>" + endContent);
						sleep(999);
					}
					showHTML(content + "NOW!</em>" + endContent);
					sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				showHTML((String) null);
				nowShowing = lastURL;
				displayHasChanged();
			}

		};
		borderThread.start();
		updateMyStatus();
	}

	protected void showHTML(String html) {

	}

	private void displayHasChanged() {
		String url = getContent().getURI().toASCIIString();
		if (!url.equalsIgnoreCase(SELF_IDENTIFY))
			splashCountdown.set(0);

		if (borderCountdown.get() == 0) {
			mainPanel.setBorder(BorderFactory.createLineBorder(getPreferredHighlightColor(), 15));
			borderCountdown.set(60); // Keep the border on there for a minute
			borderThread = new Thread() {
				public void run() {
					// Turn off the border in a while
					try {
						while (borderCountdown.getAndDecrement() > 0) {
							// This cute line causes the border to shrink slowly as the time counts to zero
							mainPanel.setBorder(BorderFactory.createLineBorder(getPreferredHighlightColor(),
									borderCountdown.get() / 4));
							sleep(1000);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					mainPanel.setBorder(BorderFactory.createEmptyBorder());
					borderCountdown.set(0);
				}
			};
			borderThread.start();
		}
		updateMyStatus();
	}

	/**
	 * Update my status to the database
	 */
	private synchronized void updateMyStatus() {
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
			e.printStackTrace();
		}

	}

	@Override
	public String getStatus() {
		return nowShowing;
	}

	private void changeURL(String url) {
		browserLauncher.changeURL(url);
	}

	public static void main(String[] args) {

		/*
		 * TODO --- Certificates can be added, awkwardly (https://sites.google.com/a/teamdev.com/jxbrowser-support/home, search for
		 * "certificate"):
		 * 
		 * it's possible to use SSL certificate. While there is no direct way, but you can copy the profile files (cert8.db, key3.db
		 * and cert_override.txt) from the firefox profile (with SSL certificate) folder into "jxbrowser firefox profile folder" .
		 * Then JxBrowser has the certificates and use them.
		 */

		@SuppressWarnings("unused")
		DynamicDisplayWithoutJxBrowser add;

		if (args.length == 0) {
			System.err.println("Usage: " + DynamicDisplayWithoutJxBrowser.class.getCanonicalName() + " <displayIDNumber>");
			System.err.println("       -- or --");
			System.err.println("       " + DynamicDisplayWithoutJxBrowser.class.getCanonicalName()
					+ " \"Location string\" displayType colorCode portNumber");
			System.exit(-1);
		}

		if (args.length == 4) {
			String location = args[0];
			String displayType = args[1];
			String colorCode = args[2];
			int port = Integer.parseInt(args[3]);

			add = makeADynamicDisplay(location, 0, displayType, 0, colorCode, port);

		} else {
			int number = Integer.parseInt(args[0]);

			try {
				add = makeADynamicDisplay(number);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}