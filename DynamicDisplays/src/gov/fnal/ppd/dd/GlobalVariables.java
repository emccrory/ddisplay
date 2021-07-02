/*
 * GlobalVariables
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;
import static gov.fnal.ppd.dd.db.DisplayUtilDatabase.getFlavorFromDatabase;

import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import gov.fnal.ppd.dd.changer.ListOfExistingContent;
import gov.fnal.ppd.dd.interfaces.NotificationClient;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.guiUtils.NotificationClientText;
import gov.fnal.ppd.dd.util.nonguiUtils.CheckForUpdatesTimerTask;
import gov.fnal.ppd.dd.util.nonguiUtils.CredentialsNotFoundException;
import gov.fnal.ppd.dd.util.nonguiUtils.PropertiesFile;
import gov.fnal.ppd.dd.util.specific.NotificationClientGUI;
import gov.fnal.ppd.dd.util.version.VersionInformation;
import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;

/**
 * This is where all the global constant in the Dynamic Displays system are held.
 * 
 * TODO - Rationalize the place where global constants are defined. Most of them are supposed to be here, but how are they
 * initialized?
 * 
 * The categories of constants, as I see it now, are:
 * 
 * 1. Things that never, ever will change (e.g., the definition of the constant ONE_BILLION) - This will be defined here. To change
 * one of these requires a re-compile.
 * 
 * 2. Things that only change on a system-wide basis (e.g., the default web server URL) - These constants should be defined in the
 * Properties file (see class PropertiesFile).
 * 
 * 3. Things that change depending on how the program is being run (e.g., SHOW_IN_WINDOW) - These are changed by a system constant,
 * set when the JVM is launched.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class GlobalVariables {

	private GlobalVariables() {
		// Do not instantiate
	}

	/** Do we show in full screen or in a window? Controlled by system constant, ddisplay.selector.inwindow */
	public final static boolean			SHOW_IN_WINDOW				= Boolean.getBoolean("ddisplay.selector.inwindow");

	/** Does the user want to have the database index for the display shown (default) or the virtual display numbers? */
	public final static boolean			SHOW_VIRTUAL_DISPLAY_NUMS	= Boolean.getBoolean("ddisplay.virtualdisplaynumbers");

	/** Does the user want to show and extended display name on the display buttons? */
	public static boolean				SHOW_EXTENDED_DISPLAY_NAMES	= Boolean.getBoolean("ddisplay.extendeddisplaynnames");

	/** How long since last user activity? */
	private static long					lastUserActivity			= 0L;
	private static VersionInformation	versionInfo					= null;

	/**
	 * What is the version of the suite this instance is running?
	 * 
	 * @return the 3-field version number of the software suite right now.
	 */
	public static String getSoftwareVersion() {
		if (versionInfo == null) {
			versionInfo = VersionInformation.getVersionInformation();
		}

		return versionInfo.getVersionString();
	}

	/**
	 * The IP Name that this instance is running on.
	 */
	public static String THIS_IP_NAME;
	static {
		try {
			String s = InetAddress.getLocalHost().getHostName().toLowerCase();
			int index = s.indexOf('.');
			if (index > 0)
				THIS_IP_NAME = s.substring(0, index);
			else
				THIS_IP_NAME = s;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * To allow for multiple Channel Selectors in one node, it will be differentiated with this tag. This has not been used except
	 * in testing.
	 */
	public final static String THIS_IP_NAME_INSTANCE = System.getProperty("ddisplay.selectorinstance", "00");

	/**
	 * @return The messaging name for this selector client.
	 */
	public final static String getFullSelectorName() {
		return THIS_IP_NAME + " selector " + THIS_IP_NAME_INSTANCE;
	}

	/**
		 * 
		 */
	public static int			INSET_SIZE				= 10;
	/**
	 * 
	 */
	public static float			FONT_SIZE				= 40.0f;

	// /**
	// * Where is the messaging server? Controlled by system constant, ddisplay.messagingserver
	// */
	// public final static String MESSAGING_SERVER_NAME = System.getProperty("ddisplay.messagingserver", DEFAULT_SERVER);

	/**
	 * What port is the Messaging Server listing on? This is an easy to remember (I hope) in the range of unassigned port number
	 * (49152 - 65535) Controlled by system constant ddisplay.messagingserver. I like primes.
	 */
	public final static int		MESSAGING_SERVER_PORT	= PropertiesFile.getIntProperty("messagingPort", 49999);
	/** Where is the Web server? Controlled by system constant ddisplay.webserver */
	public final static String	WEB_SERVER_NAME			= PropertiesFile.getProperty("webServer", "dynamicdisplays.fnal.gov");
	/** Where is the Web server? Controlled by system constant ddisplay.webserver */
	private final static String	WEB_SERVER_FOLDER		= PropertiesFile.getProperty("webFolder", "");

	/** Do we use http or https? */
	public final static String	WEB_PROTOCOL			= PropertiesFile.getProperty("defaultWebProtocol", "https");

	/**
	 * @return The web server prefix, dealing with whether or not there is a folder in there, too.
	 */
	public final static String getFullURLPrefix() {
		if (WEB_SERVER_FOLDER.length() > 0)
			return WEB_PROTOCOL + "://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER;

		return WEB_PROTOCOL + "://" + WEB_SERVER_NAME;
	}

	/** The name of the ZIP file that will contain a software update */
	public static final String	SOFTWARE_FILE_ZIP			= PropertiesFile.getProperty("SoftwareFileZip");
	/** The location of the ZIP file that will contain a software update */
	public static final String	UNZIP_PROGRAM_LOCATION		= PropertiesFile.getProperty("UnzipLocation");
	/** Where is the Database server? Controlled by system constant ddisplay.dbserver */
	public static String		DATABASE_SERVER_NAME		= System.getProperty("ddisplay.dbserver");
	/** The database name, as in "USE " + DATABASE_NAME. Controlled by system constant ddisplay.dbname */
	public static String		DATABASE_NAME				= System.getProperty("ddisplay.dbname");
	/** The username for accessing the database */
	public static String		DATABASE_USER_NAME			= System.getProperty("ddisplay.dbusername");
	/**
	 * the password corresponding to the username that accesses the database. Note that this MUST be entered by hand for each time
	 * one runs an application. (This is not the actual password.)
	 */
	public static String		DATABASE_PASSWORD			= System.getProperty("ddisplay.dbpassword");
	/**
	 * Where is the XML server? This is the place where the XML schema is stored (8/2014: The only usage of this constant)
	 * Controlled by system constant ddisplay.xmlserver
	 */
	public final static String	XML_SERVER_NAME				= System.getProperty("ddisplay.xmlserver");

	/** The URL that is the single image display web page. This is a bit of a kludge! */
	public final static String	SINGLE_IMAGE_DISPLAY		= PropertiesFile.getProperty("singleImageDisplay",
			getFullURLPrefix() + "/portfolioOneSlide.php?photo=");

	/** A symbol for 1,000,000,000. */
	public final static int		ONE_BILLION					= 1000000000;

	/** One second, expressed in milliseconds (e.g., 1000L) */
	public final static long	ONE_SECOND					= 1000L;
	/** One Minute, expressed in milliseconds (e.g., 60000L) */
	public final static long	ONE_MINUTE					= 60L * ONE_SECOND;
	/** 15 minutes, expressed in milliseconds (e.g., 900000L) */
	public final static long	FIFTEEN_MINUTES				= 15L * ONE_MINUTE;
	/** One hour, expressed in milliseconds (e.g., 3600000L) */
	public final static long	ONE_HOUR					= 60L * ONE_MINUTE;
	/** One day (24 hours), expressed in milliseconds (e.g., 86400000L) */
	public final static long	ONE_DAY						= 24L * ONE_HOUR;
	/** Used in ChannelSelector to go to the splash screen */
	public final static long	INACTIVITY_TIMEOUT			= 3L * ONE_MINUTE;
	/** Used in ChannelSelector */
	public final static long	PING_INTERVAL				= 5L * ONE_SECOND;

	/**
	 * How long should the client wait between attempts to reconnect to the server. For displays, ONE_MINUTE is good. For
	 * Controllers, the user is going to be less patient!
	 */
	public final static long	WAIT_FOR_SERVER_TIME		= ONE_MINUTE;

	/** The string that means "show me your colors" */
	public final static String	SELF_IDENTIFY				= "http://identify";
	/** The string of a URL that means, perform a full and complete refresh of the page you are showing now */
	public final static String	FORCE_REFRESH				= "http://refresh";
	/** Expected exit codes: This signifies a normal exit, and we should be able to recover from it */
	public final static int		PURE_EXIT_OF_JVM			= 0;
	/** Expected exit codes: This signifies an exit we should be able to recover from */
	public final static int		SIMPLE_RECOVERABLE_ERROR	= -1;
	/** Expected exit codes: This signifies an error we might be able to recover from */
	public final static int		POSSIBLE_RECOVERABLE_ERROR	= -2;
	/** Expected exit codes: This signifies an error we cannot recover from */
	public final static int		UNRECOVERABLE_ERROR			= -999;
	/** The name that is associated with the default URL for a Display */
	public static final String	STANDBY_URL_NAME			= "unspecified content";
	/** The default URL for a Display */
	public static final String	STANDBY_URL					= WEB_PROTOCOL + "://" + WEB_SERVER_NAME + "/standby.html";

	/**
	 * The list of Displays in this instance of whatever program you are running. This is used in a couple of places.
	 */
	public static List<Display>	displayList					= null;

	private static Logger		logger						= null;

	private final static long	DEFAULT_DWELL_TIME			= 2 * ONE_HOUR;

	/**
	 * <p>
	 * The default dwell time if the dwell time is zero. In other words, how long do we want to wait before the FireFox instance
	 * asks to refresh a web page. It has been my observation that this is needed for these reasons:
	 * 
	 * <ol>
	 * <li>Many pages really do need refreshing because they "end", like YouTube videos and PowerPoint presentations</li>
	 * <li>Many web pages just fail from time to time. For example, the webcam pages and the "Channel 13" accelerator display.</li>
	 * <ol>
	 * </p>
	 */
	public final static long getDefaultDwellTime() {
		// This random 5-second variation is to put default refreshes for different channels and different displays at different
		// times which I hope will even out the load a little bit. One common scenario is that all the displays start at the
		// same time, so if they all have the same default timeout, these will collide.

		// It might not be a big deal, but it makes me feel better.
		return (long) (10000L * (0.5 - Math.random())) + DEFAULT_DWELL_TIME;
	}

	private static List<Integer> locationCodes = new ArrayList<Integer>();

	/**
	 * Which set of Displays are we controlling here? Controlled by system constant, ddisplay.selector.location
	 * 
	 * @return The location code we have
	 */
	public static int getLocationCode() {
		if (locationCodes.size() > 0)
			return locationCodes.get(0);
		return 0;
	}

	/**
	 * @param lc
	 *            the location code to add to the list
	 * @return After Collections.add(), returns true if this operation changed the list of location codes.
	 */
	public static boolean addLocationCode(final int lc) {
		assert (lc >= -1);
		if (lc == -1) {
			locationCodes.clear();
			return locationCodes.add(-1);
		}
		if (locationCodes.contains(lc) || locationCodes.contains(-1))
			return false;
		return locationCodes.add(lc);
	}

	/**
	 * @return The number of location codes relevant here
	 */
	public static int getNumberOfLocations() {
		return locationCodes.size();
	}

	/**
	 * @param index
	 *            Which location code to return
	 * @return the index'th location code
	 */
	public static int getLocationCode(final int index) {
		if (locationCodes.size() > index)
			return locationCodes.get(index);
		return getLocationCode();
	}

	/**
	 * Short name for the location of the displays
	 */
	static String	locationName;

	/**
	 * Long name for the location of the displays
	 */
	static String	locationDescription;
	static String	messagingServerName	= null;

	/**
	 * @return the location (short) name corresponding to this index value
	 */
	public static String getLocationName() {
		checkName(); // Be sure to read the database
		return locationName;
	}

	/**
	 * @return The location (long) description corresponding to this index value
	 */
	public static String getLocationDescription() {
		checkName(); // Be sure to read the database
		return locationDescription;
	}

	private static void checkName() {
		if (messagingServerName == null) {
			System.err.println("ERROR in construction of your program!  You have to run "
					+ GetMessagingServer.class.getCanonicalName() + ".getMessagingServerName*() (where * is either "
					+ "Selector or Display, depending on what you are) first");
			new Exception("ERROR in construction of your program!").printStackTrace();
			System.exit(-1);
		}

	}

	/**
	 * @return The name of our messaging server, e.g., roc-w-11.fnal.gov
	 */
	public static String getMessagingServerName() {
		checkName(); // Be sure to read the database
		return messagingServerName;
	}

	/**
	 * Where is the private key stored on the local file system.
	 * 
	 * On Linux (& MAC) the default will be $HOME/keystore/private\<ipname\> selector 00.key
	 * 
	 * On Windows the default is expected to be C:\\keystore\\\<ipname\> selector 00.key
	 * 
	 */
	public static String	PRIVATE_KEY_LOCATION;

	/**
	 * This is the name of the "Docent" in the central database.
	 */
	public static String	docentName		= null;

	private static String[]	credentialsPath	= { "/keystore/", System.getenv("HOME") + "/keystore/",
			System.getenv("HOMEPATH") + "/keystore/" };

	/**
	 * Every machine EXCEPT places where the development happens will be eligible to update their code.
	 * 
	 * @return Return if this is NOT a development machine
	 */
	public static boolean okToUpdateSoftware() {
		for (String test : PropertiesFile.getProperty("DevelopmentMachine").split(":"))
			if (THIS_IP_NAME.equals(test)) {
				println(GlobalVariables.class,
						"This machine, " + THIS_IP_NAME + ", is a development machine -- Skipping automatic updates."
								+ "\n\t\t\t\t\t\tThe development machines are: "
								+ Arrays.toString(PropertiesFile.getProperty("DevelopmentMachine").split(":")));
				return false;
			}
		return true;
	}

	/**
	 * Start a thread that runs every week (at, say, 0400, plus a random offset) to see if there is a new version of the code
	 * 
	 * @param isMessagingServer
	 *            Set to true if this is the messaging server. It will look for the updates a little earlier than all the other bits
	 */
	public static void prepareUpdateWatcher(final boolean isMessagingServer) {
		// Skip update for the development machine! This can have horrible consequences if the place where the development is
		// happening all of the sudden replaces the code! Now, it probably won't be all THAT horrible since (a) the update does not
		// delete anything, and (b) the development machine will always have the latest version. But we don't want any accidents.

		if (!okToUpdateSoftware())
			return;

		Calendar date = Calendar.getInstance();
		if (PropertiesFile.getProperty("FirstUpdateWait") == null) {
			date.set(Calendar.MINUTE, 0);
			date.set(Calendar.SECOND, 0);
			date.set(Calendar.MILLISECOND, 0);
			if (isMessagingServer) {
				// Let the messaging servers get the software first
				date.set(Calendar.HOUR_OF_DAY, 3);
				date.add(Calendar.SECOND, (int) (Math.random() * 5.0 * 60.0)); // Choose a random second between 3 and 3:05
			} else {
				// Let the displays and the controllers get their updates a little later
				date.set(Calendar.HOUR_OF_DAY, 4);
				date.add(Calendar.SECOND, (int) (Math.random() * 45.0 * 60.0)); // Choose a random second between 4 and 4:45
			}
			date.add(Calendar.HOUR, 24); // Do the first one tomorrow
		} else {
			// User overrides the first look for the update (testing, we presume)
			long wait = Long.parseLong(PropertiesFile.getProperty("FirstUpdateWait"));
			date.add(Calendar.SECOND, (int) wait);
		}
		// Run myTimerTask at the same time every day
		long period = 1000 * 60 * 60 * 24L;
		if (PropertiesFile.getProperty("LookForUpdatesPeriod") != null)
			// User overrides the default 24 hour period for looking for updates (testing, we presume)
			period = 1000 * Long.parseLong(PropertiesFile.getProperty("LookForUpdatesPeriod"));

		NotificationClient client = null;
		if (isMessagingServer)
			client = new NotificationClientText();
		else
			client = new NotificationClientGUI();

		Timer timer = new Timer();
		TimerTask myTimerTask = new CheckForUpdatesTimerTask(client);
		timer.schedule(myTimerTask, date.getTime(), period);

		// Maybe we should run this update task really frequently so the user can change the update period. Nah.

		println(GlobalVariables.class, "------------------------- Versioning ------------------------------");
		println(GlobalVariables.class, "-- This is software version " + getSoftwareVersion());
		println(GlobalVariables.class, "-- The desired flavor of software is will be determined when we check for updates.");
		if (period < 2 * ONE_HOUR)
			println(GlobalVariables.class,
					"-- Will check for updates every " + period / 1000 + " seconds starting at " + date.getTime());
		else
			println(GlobalVariables.class,
					"-- Will check for updates every " + (period / 1000 / 60 / 60) + " hours starting at " + date.getTime());

		println(GlobalVariables.class, "-------------------------------------------------------------------");
	}

	// ----------------------------------------------------------------------------------------------------------------

	private static String DISPLAY_NODE_NAME = null;

	/**
	 * Set the location this node gets the default FLAVOR of the software to be from the database. If this is not called, it will
	 * get that FLAVOR from the Properties file.
	 * 
	 * @param f
	 *            The name of this node
	 */
	public static void setFlavorFromDatabase(String f) {
		DISPLAY_NODE_NAME = f;
		println(GlobalVariables.class, "Will be getting the update FLAVOR for this node, " + f + ", from the database.");
	}

	/**
	 * What FLAVOR of the software should this instance be looking for.
	 * 
	 * @param reset
	 *            If true, then positively re-read the Properties file. This has no effect if the FLAVOR is being read from the
	 *            database.
	 * @return The FLAVOR of the software should this instance be looking for.
	 */
	public static FLAVOR getFlavor(boolean reset) {
		FLAVOR flavor = FLAVOR.PRODUCTION;

		if (DISPLAY_NODE_NAME != null) {
			return getFlavorFromDatabase(DISPLAY_NODE_NAME);
		} else {
			if (reset)
				PropertiesFile.reset();
			if (PropertiesFile.getProperty("UpdateFlavor") != null) {
				try {
					flavor = FLAVOR.valueOf(PropertiesFile.getProperty("UpdateFlavor"));
				} catch (Exception e) {
					printlnErr(GlobalVariables.class, "Unrecognized software flavor in the configuration file: "
							+ PropertiesFile.getProperty("UpdateFlavor") + ". Will use " + FLAVOR.PRODUCTION);
					flavor = FLAVOR.PRODUCTION;
				}
			}
		}
		return flavor;
	}

	/**
	 * Read the file, credentials.txt, for establishing connection to the database
	 */
	public static void credentialsSetup() throws CredentialsNotFoundException {
		String fileName = "credentials.txt";
		String key_loc = ("/private" + getFullSelectorName() + ".key").toLowerCase();

		File file = null;

		for (String P : credentialsPath) {
			File f = new File(P + fileName);
			if (f.exists() && !f.isDirectory()) {
				file = f;
				PRIVATE_KEY_LOCATION = P + key_loc;
				break;
			}
		}

		if (file == null) {
			System.err.println("Cannot find credentials file.  Looked in these folders: " + Arrays.toString(credentialsPath));
			throw new CredentialsNotFoundException(
					"Cannot find credentials file.  Looked in these folders: " + Arrays.toString(credentialsPath));
		}

		try {
			int lineNumber = 0;
			String dbServer = "";
			try (Scanner scanner = new Scanner(file)) {
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					String newline = ignoreComments(line);
					if (newline != null) {
						switch (lineNumber) {
						case 0:
							// MySQL Server name
							dbServer = newline;
							break;
						case 1:
							// MySQL server port
							dbServer += ":" + newline;
							DATABASE_SERVER_NAME = dbServer;
							break;
						case 2:
							// Database name
							DATABASE_NAME = newline;
							break;
						case 3:
							// Database user
							DATABASE_USER_NAME = newline;
							break;
						case 4:
							// database password
							DATABASE_PASSWORD = newline;
							break;
						}
						lineNumber++;
					}
					// System.out.println(newline);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		println(GlobalVariables.class, ": Have read database credentials from [" + file.getAbsolutePath() + "]");
	}

	private static String ignoreComments(String line) {
		String result_line = null;
		int upto = line.indexOf('#');
		if (upto != 0 && upto > 0) {
			result_line = line.substring(0, upto);
		} else {
			if (upto < 0) {
				result_line = line;
			} /*
				 * else{ result_line=""; }
				 */
		}

		return result_line;
	}

	private static ListOfExistingContent contentOnDisplays = new ListOfExistingContent();

	/**
	 * @return the the list of content that can be shown
	 */
	public static ListOfExistingContent getContentOnDisplays() {
		return contentOnDisplays;
	}

	/**
	 * @return Has the user demonstrated their presence recently?
	 */
	public static boolean userIsInactive() {
		return System.currentTimeMillis() > INACTIVITY_TIMEOUT + lastUserActivity;
	}

	/**
	 * @return The number of milliseconds since the last, detected user activity.
	 */
	public static long timeSinceLastUserActivity() {
		return System.currentTimeMillis() - lastUserActivity;
	}

	/**
	 * Call this method when the user does something, proving that they are at the console, manipulating the GUI
	 */
	public static void userHasDoneSomething() {
		GlobalVariables.lastUserActivity = System.currentTimeMillis();
	}

	/**
	 * 
	 * @return The logger that this instance should be using
	 */
	public static Logger getLogger() {
		return logger;
	}

	/**
	 * 
	 * @param l
	 *            The logger that this instance should be using
	 */
	public static void setLogger(Logger l) {
		logger = l;
	}
}
