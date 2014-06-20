package gov.fnal.ppd.signage.display.attic.jxbrowser;


/**
 * Dynamic Display server task for listening to a port for the URI (really, a URL) to display on the JxBrowser.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2013
 * @deprecated
 * 
 */
public class ADynamicDisplay { // extends DisplayImpl {
//
//	protected static final String	OFF_LINE				= "Off Line";
//	private static final String		SELF_IDENTIFY			= "http://identify";
//	protected static final long		STATUS_UPDATE_PERIOD	= 60000l;
//	private static int				number;
//	private static Connection		connection;
//	private Browser					browser;
//	private boolean					keepGoing				= true;
//	private AtomicInteger			borderCountdown			= new AtomicInteger(0);
//	private AtomicInteger			splashCountdown			= new AtomicInteger(0);
//	private Thread					borderThread;
//
//	private JPanel					mainPanel				= new JPanel(new BorderLayout());
//	private String					lastURL;
//	private String					nowShowing				= "Startup Splash Screen";
//	private static boolean			dynamic					= false;
//
//	static {
//		// Locate the plugins for the various browsers
//
//		// For SLF-64 & Mozilla:
//		System.setProperty("jxbrowser.plugin.dir", "/usr/lib64/mozilla/plugins:/usr/lib64/mozilla/plugins-wrapped");
//
//	}
//
//	public static ADynamicDisplay makeADynamicDisplay(final String location, final int content, final String typ,
//			final int screenNum, final String colorCode, final int port) {
//
//		try {
//			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
//		} catch (SignageDatabaseNotVisibleException e1) {
//			e1.printStackTrace();
//		}
//		ADynamicDisplay.dynamic = true;
//
//		// This display is not registered. Let's register it!
//		try {
//			final Statement stmt = connection.createStatement();
//			stmt.executeQuery("USE xoc");
//
//			String ipAddress = InetAddress.getLocalHost().getHostAddress();
//
//			stmt.executeUpdate("INSERT INTO Display VALUES ('" + location + "', " + content + ", '" + typ + "', NULL, '"
//					+ ipAddress + "', '" + screenNum + "', '" + colorCode + "', '" + port + "')");
//
//			// Retrieve the display number that I am
//			ResultSet rs = stmt.executeQuery("SELECT * from Display where IPName='" + ipAddress + "'");
//
//			rs.first(); // Move to first returned row (there should only be one)
//			ADynamicDisplay.number = rs.getInt("DisplayID");
//			stmt.executeUpdate("INSERT INTO DisplayStatus VALUES ('Startup', " + ADynamicDisplay.number
//					+ ", NULL, '2013-01-01 00:00:00')");
//
//			stmt.close();
//			rs.close();
//			return new ADynamicDisplay(port, true, ipAddress, screenNum, number, location, new Color(
//					Integer.parseInt(colorCode, 16)), SignageType.valueOf(typ));
//
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		} catch (SQLException e) {
//			System.out.println("SQL Error code: " + e.getErrorCode());
//			e.printStackTrace();
//
//		}
//
//		//  -- Maybe we should remove this display from the database when the program exits?
//
//		return null;
//	}
//
//	/**
//	 * Look up all the information from the database using the number as the DisplayID
//	 */
//	public static ADynamicDisplay makeADynamicDisplay(final int number) {
//
//		// -- It might be appropriate to have a display add itself to the Database when it starts.
//
//		ADynamicDisplay.number = number;
//		try {
//			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
//		} catch (SignageDatabaseNotVisibleException e1) {
//			e1.printStackTrace();
//			System.err.println("\nNo connection to the Signage/Displays database.  We're making stuff up now.");
//			// Make something up--this is a test (obviously)
//			return new ADynamicDisplay(99999, true, "Fake Display", 0, number, "Fake location", Color.blue, SignageType.XOC);
//		}
//
//		Statement stmt = null;
//		ResultSet rs = null;
//
//		try {
//			stmt = connection.createStatement();
//			rs = stmt.executeQuery("USE xoc");
//		} catch (SQLException ex) {
//			ex.printStackTrace();
//			System.exit(1);
//		}
//
//		try {
//			rs = stmt.executeQuery("SELECT * FROM Display where DisplayID=" + number);
//			rs.first(); // Move to first returned row (there should only be one)
//			String myName = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("IPName"));
//
//			String node = InetAddress.getLocalHost().getCanonicalHostName();
//			if (!myName.equals(node)) {
//				// -- This will not work if the IPName in the database is an IP address.
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
//			return new ADynamicDisplay(portNumber, true, myName, screenNumber, number, location, color, type);
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
//
//		System.err.println("Cannot create an instance of ADynamicDisplay!");
//		System.exit(-1);
//		return null;
//	}
//
//	// public ADynamicDisplay(final int portNumber, final boolean maximized, String ipName, int xOffset, int yOffset, int number,
//	// String location, Color color, SignageType type) {
//	// super(ipName, 0, number, location, color, type);
//	//
//	// browser = BrowserFactory.createBrowser(BrowserType.Mozilla);
//	// lastURL = "http://xoc.fnal.gov";
//	// identifyMe();
//	//
//	// GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[screenNumber]
//	// .getDefaultConfiguration();
//	//
//	// JFrame frame = new JFrame("Dynamic Display no. " + this.getNumber(), config);
//	// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//	//
//	// mainPanel.add(getComponent(), BorderLayout.CENTER);
//	// frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
//	//
//	// if (maximized) {
//	// frame.setExtendedState(Frame.MAXIMIZED_BOTH);
//	// frame.setUndecorated(true);
//	// frame.setLocationRelativeTo(null);
//	// Rectangle bounds = config.getBounds();
//	// frame.setSize(bounds.width, bounds.height);
//	// frame.setLocation(xOffset, yOffset);
//	// } else {
//	// frame.setSize(1000, 1000);
//	// }
//	//
//	// frame.setVisible(true);
//	//
//	// contInitialization(portNumber);
//	// }
//
//	public ADynamicDisplay(final int portNumber, final boolean maximized, String ipName, int screenNumber, int number,
//			String location, Color color, SignageType type) {
//		super(ipName, screenNumber, number, location, color, type);
//
//		// System.setProperty("teamdev.license.info", "true");
//		browser = BrowserFactory.create();
//		lastURL = "http://xoc.fnal.gov";
//		identifyMe();
//
////		GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[screenNumber]
////				.getDefaultConfiguration();
//		Rectangle bounds = ScreenLayoutInterpreter.getBounds(screenNumber);
//
//		JFrame frame = new JFrame("Dynamic Display no. " + this.getNumber());
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//		mainPanel.add(getComponent(), BorderLayout.CENTER);
//		frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
//
//		if (maximized) {
//			// frame.setExtendedState(Frame.MAXIMIZED_BOTH);
//			frame.setUndecorated(true);
//			frame.setLocationRelativeTo(null);
//			frame.setSize(bounds.width, bounds.height);
//			frame.setLocation((int) bounds.getX(), (int) bounds.getY());
//		} else {
//			frame.setSize(1000, 1000);
//		}
//
//		frame.setVisible(true);
//
//		contInitialization(portNumber);
//	}
//
//	private void contInitialization(final int portNumber) {
//
//		new Thread("ADisplayServerDaemon") {
//			public void run() {
//				DCMulitServerThread server = null;
//				while (keepGoing) {
//					if (server == null || !server.isAlive())
//						try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
//							server = null;
//							System.gc();
//							server = new DCMulitServerThread(serverSocket.accept());
//							server.addListener(ADynamicDisplay.this);
//							server.start();
//						} catch (IOException e) {
//							System.err.println("Could not listen on port " + portNumber);
//							System.exit(-1);
//						}
//					try {
//						// Make sure the serverSocket is alive, continually
//						sleep(2500);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		}.start();
//
//		new Thread("StatusThread") {
//			public void run() {
//				while (keepGoing) {
//					try {
//						sleep(STATUS_UPDATE_PERIOD);
//						updateMyStatus();
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		}.start();
//
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			public void run() {
//				keepGoing = false;
//				nowShowing = OFF_LINE;
//				updateMyStatus();
//			}
//		});
//	}
//
//	private Component getComponent() {
//		synchronized (browser) {
//			return browser.getView().getComponent();
//		}
//	}
//
//	// @Override
//	// public boolean changeURL(URL newURL) {
//	// if (newURL.toString().equalsIgnoreCase(SELF_IDENTIFY))
//	// identifyMe();
//	// else {
//	// synchronized (browser) {
//	// browser.navigate(newURL.toString());
//	// }
//	// nowShowing = lastURL = newURL.toString();
//	// displayHasChanged();
//	// }
//	// return true;
//	// }
//
//	@Override
//	protected void localSetContent() {
//		String url = getContent().getURI().toASCIIString();
//		synchronized (browser) {
//			if (url.equalsIgnoreCase(SELF_IDENTIFY)) {
//				identifyMe();
//				nowShowing = url; // Do not change the value of lastURL
//			} else {
//				browser.loadURL(url);
//				nowShowing = lastURL = url;
//			}
//		}
//		displayHasChanged();
//	}
//
//	/**
//	 * Put up a colorful splash screen that identifies this Display
//	 */
//	private void identifyMe() {
//		String colorString = Integer.toHexString(getPreferredHighlightColor().getRGB()).substring(2);
//		String style1 = "font-size:500%;font-weight:bold;background-color:white;border-style:solid;border-width:25px;border-color:white;";
//		String style2 = "font-size:350%;background-color:white;border-style:solid;border-width:25px;border-color:white;";
//		String style3 = "font-size:150%;background-color:white;border-style:solid;border-width:25px;border-color:white;";
//		final String content = "<html><body style='margin:200;background-color:#" + colorString + ";'>\n" + "<p align='center' "
//				+ "style='" + style1 + "'>Display Number " + number + "</p>\n" + "<p align='center' style='" + style2
//				+ "'>This is " + (getCategory() == SignageType.Experiment ? "an Experiment" : "a " + getCategory())
//				+ " Display<br>\n<p align='center' style='" + style3 + "'>" + "Screen number " + getScreenNumber() + "<br>\n"
//				+ "Location = '" + getLocation() + "'<br>\nColor code is #" + colorString + "</p>" + "<p><em>The web site '"
//				+ lastURL + "' will be shown ";
//
//		final String endContent = "</p></body></html>";
//
//		nowShowing = "Self identification splash screen";
//
//		System.out.println(content);
//		splashCountdown.set(10);
//		borderThread = new Thread() {
//			public void run() {
//				// Turn off the border in a while
//				try {
//					while (splashCountdown.decrementAndGet() > 0) {
//						synchronized (browser) {
//							if (splashCountdown.get() != 1)
//								browser.loadHTML(content + "in about " + splashCountdown + " seconds.</em>" + endContent);
//							else
//								browser.loadHTML(content + "in a second.</em>" + endContent);
//						}
//						sleep(999);
//					}
//					browser.loadHTML(content + "NOW!</em>" + endContent);
//					sleep(100);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//
//				synchronized (browser) {
//					browser.loadURL(lastURL);
//				}
//				nowShowing = lastURL;
//				displayHasChanged();
//			}
//
//		};
//		borderThread.start();
//		updateMyStatus();
//	}
//
//	private void displayHasChanged() {
//		String url = getContent().getURI().toASCIIString();
//		if (!url.equalsIgnoreCase(SELF_IDENTIFY))
//			splashCountdown.set(0);
//
//		if (borderCountdown.get() == 0) {
//			mainPanel.setBorder(BorderFactory.createLineBorder(getPreferredHighlightColor(), 15));
//			borderCountdown.set(60); // Keep the border on there for a minute
//			borderThread = new Thread() {
//				public void run() {
//					// Turn off the border in a while
//					try {
//						while (borderCountdown.getAndDecrement() > 0) {
//							// This cute line causes the border to shrink slowly as the time counts to zero
//							mainPanel.setBorder(BorderFactory.createLineBorder(getPreferredHighlightColor(),
//									borderCountdown.get() / 4));
//							sleep(1000);
//						}
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					mainPanel.setBorder(BorderFactory.createEmptyBorder());
//					borderCountdown.set(0);
//				}
//			};
//			borderThread.start();
//		}
//		updateMyStatus();
//	}
//
//	/**
//	 * Update my status to the database
//	 */
//	private synchronized void updateMyStatus() {
//		Statement stmt = null;
//		// ResultSet rs = null;
//
//		try {
//			stmt = connection.createStatement();
//			stmt.executeQuery("USE xoc");
//		} catch (SQLException ex) {
//			ex.printStackTrace();
//			System.exit(1);
//		}
//
//		try {
//			Date dNow = new Date();
//			SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//			// String statementString = "UPDATE DisplayStatus set Time='" + ft.format(dNow) + "',Content='" + getStatus()
//			// + "',ContentName='" + getContent().getName() + "' where DisplayID=" + number;
//			// -- Give xocuser the rights to change ContentName in this table!
//			String statementString = "UPDATE DisplayStatus set Time='" + ft.format(dNow) + "',Content='" + getStatus()
//					+ "' where DisplayID=" + number;
//			int numRows = stmt.executeUpdate(statementString);
//			if (numRows == 0 || numRows > 1) {
//				System.err.println("Problem while updating status of Display: Expected to modify exactly one row, but  modified "
//						+ numRows + " rows instead. SQL='" + statementString + "'");
//			}
//
//			if (dynamic && OFF_LINE.equalsIgnoreCase(nowShowing)) {
//				// When we are all done, remove this new record from the Display and the DisplayStatus tables
//				stmt.executeUpdate("DELETE FROM Display WHERE DisplayID=" + number);
//				stmt.executeUpdate("DELETE FROM DisplayStatus WHERE DisplayID=" + number);
//			}
//			stmt.close();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//
//	}
//
//	@Override
//	public String getStatus() {
//		return nowShowing;
//	}
//
//	public static void main(String[] args) {
//
//		/*
//		 *  --- Certificates can be added, awkwardly (https://sites.google.com/a/teamdev.com/jxbrowser-support/home, search for
//		 * "certificate"):
//		 * 
//		 * it's possible to use SSL certificate. While there is no direct way, but you can copy the profile files (cert8.db, key3.db
//		 * and cert_override.txt) from the firefox profile (with SSL certificate) folder into "jxbrowser firefox profile folder" .
//		 * Then JxBrowser has the certificates and use them.
//		 */
//
//		@SuppressWarnings("unused")
//		ADynamicDisplay add;
//
//		if (args.length == 0) {
//			System.err.println("Usage: " + ADynamicDisplay.class.getCanonicalName() + " <displayIDNumber>");
//			System.err.println("       -- or --");
//			System.err.println("       " + ADynamicDisplay.class.getCanonicalName()
//					+ " \"Location string\" displayType colorCode portNumber");
//			System.exit(-1);
//		}
//
//		if (args.length == 4) {
//			String location = args[0];
//			String displayType = args[1];
//			String colorCode = args[2];
//			int port = Integer.parseInt(args[3]);
//
//			add = makeADynamicDisplay(location, 0, displayType, 0, colorCode, port);
//
//		} else {
//			int number = Integer.parseInt(args[0]);
//
//			try {
//				add = makeADynamicDisplay(number);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}
//
}