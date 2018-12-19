/*
 * GlobalVariables
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import gov.fnal.ppd.dd.changer.ListOfExistingContent;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.PropertiesFile;
import gov.fnal.ppd.dd.util.version.VersionInformation;
import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;

/**
 * This is where all the global constant in the Dynamic Displays system are held.
 * 
 * TODO - Rationalize the place where global constants are defined. Most of them are supposed to be here, but bow are they
 * initialize?
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
	/**
	 * Do we show in full screen or in a window? Controlled by system constant, ddisplay.selector.inwindow
	 */
	public final static boolean			SHOW_IN_WINDOW				= Boolean.getBoolean("ddisplay.selector.inwindow");
	/**
	 * Is this a PUBLIC controller? Controlled by system constant, ddisplay.selector.public
	 */
	public final static boolean			IS_PUBLIC_CONTROLLER		= Boolean.getBoolean("ddisplay.selector.public");

	/**
	 * Is this a controller that is thought to be used by a docent?
	 */
	public final static boolean			IS_DOCENT_CONTROLLER		= Boolean.getBoolean("ddisplay.selector.docent");

	/**
	 * Does the user want to have the database index for the display shown (default) or the virtual display numbers?
	 */
	public final static boolean			SHOW_VIRTUAL_DISPLAY_NUMS	= Boolean.getBoolean("ddisplay.virtualdisplaynumbers");

	/**
	 * Does the user want to show and extended display name on the display buttons?
	 */
	public static boolean				SHOW_EXTENDED_DISPLAY_NAMES	= Boolean.getBoolean("ddisplay.extendeddisplaynnames");

	/**
	 * How long since last user activity?
	 */
	private static long					lastUserActivity			= 0L;
	private static VersionInformation	versionInfo					= null;

	/**
	 * String that says, "Do not check message signing". This is the only word that will turn off checking. All other words will
	 * result in checking.
	 */
	public final static String			NOCHECK_SIGNED_MESSAGE		= "nocheck";

	/**
	 * String that says, "Check message signing". This is the default.
	 */
	public final static String			CHECK_SIGNED_MESSAGE		= "check";

	/**
	 * See comment in MakeChannelSelector - this has not been successfully implemented.
	 */
	public final static boolean			RUN_RAISE_SELECTOR_BUTTON	= Boolean.getBoolean("ddisplay.selector.showraisebutton");

	/**
	 * <p>
	 * How should we deal with signed messages? The options are
	 * <ol>
	 * <li>nocheck -- do not check the signature on any signed object</li>
	 * <li>check -- Check the signature and complain if the signature is bad, but use the message no matter what the check says</li>
	 * </ol>
	 * </p>
	 */

	private final static String			checkSignedMessage			= System.getProperty("ddisplay.checksignedmessage",
			CHECK_SIGNED_MESSAGE);

	/**
	 * Do we check for signatures? This seems inappropriate now - it should probably be removed (2-Aug-2018)
	 * 
	 * @return Do we need to check the signature on messages?
	 */
	public static boolean checkSignedMessages() {
		return !NOCHECK_SIGNED_MESSAGE.equals(checkSignedMessage);
	}

	public static String getSoftwareVersion() {
		if (versionInfo == null) {
			versionInfo = VersionInformation.getVersionInformation();
		}

		return versionInfo.getVersionString();
	}

	/**
	 * My IP Name
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

		if (IS_PUBLIC_CONTROLLER)
			SHOW_EXTENDED_DISPLAY_NAMES = true;
	}

	/**
	 * 
	 */
	public final static String THIS_IP_NAME_INSTANCE = System.getProperty("ddisplay.selectorinstance", "00");

	/**
	 * @return The messaging name for this selector client.
	 */
	public final static String getFullSelectorName() {
		return THIS_IP_NAME + " selector " + THIS_IP_NAME_INSTANCE;
	}

	/**
	 * @return Should the display show its number (all the time) on itself?
	 */
	public static boolean showNumberOnDisplay() {
		return locationCode != 3;
	}

	/**
	 * The names of the image files used in the screen saver
	 */
	public final static String[]	imageNames	= { "fermilab3.jpg", "fermilab1.jpg", "fermilab2.jpg", "fermilab4.jpg",
			"fermilab5.jpg", "fermilab6.jpg", "fermilab7.jpg", "fermilab8.jpg", "fermilab9.jpg", "fermilab10.jpg", "fermilab11.jpg",
			"fermilab12.jpg", "fermilab13.jpg", "fermilab14.jpg", "fermilab15.jpg", "fermilab16.jpg", "fermilab17.jpg",
			"fermilab18.jpg", "fermilab19.jpg", "fermilab20.jpg", "fermilab21.jpg", "Baginski_1319.jpg", "Baginski_1649.jpg",
			"Biron_9077.jpg", "Brown_5472.jpg", "Chapman_1066.jpg", "Chapman_2860.jpg", "Chapman_3859.jpg", "Chapman_4721.jpg",
			"Chapman_7196.jpg", "Chapman_8814.jpg", "Dyer_3235.jpg", "Ferguson_2539.jpg", "Flores_9904.jpg", "Hahn_4109.jpg",
			"Higgins_7961.jpg", "Iraci_6742.jpg", "Kroc_9559.jpg", "Limberg_9772.jpg", "McCrory_2555.jpg", "McCrory_3368.jpg",
			"McCrory_3902.jpg", "McCrory_7002.jpg", "McCrory_8865.jpg", "Murphy_2507.jpg", "Murphy_3460.jpg", "Murphy_4658.jpg",
			"Nicol_1322.jpg", "Nicol_2066.jpg", "Nicol_7967.jpg", "Olsen_6219.jpg", "Paterno_6012.jpg", "Pygott_6098.jpg",
			"Robertson_7776.jpg", "Santucci_3708.jpg", "Schwender_7961.jpg", "Scroggins_1293.jpg", "Scroggins_9472.jpg",
			"Shaddix_1745.jpg", "Shaddix_2506.jpg", "Shaddix_3843.jpg", "Shaddix_5138.jpg", "american-lotus-2.jpg",
			"auditorium-stairs.jpg", "behind-tall-grass.jpg", "bright-tevatron-tunnel.jpg", "coyote-wilson-hall.jpg",
			"egret-spots.jpg", "feynman-fountain.jpg", "iarc-angles.jpg", "july-fourth-coyote.jpg",
			"lightning-storm-over-fermilab.jpg", "main-ring-magnets.jpg", "pom-pom.jpg", "ramsey-wilson.jpg",
			"sunset-at-caseys-pond.jpg", "water-moon-venus.jpg", "wilson-hall-moon-airplane.jpg", "yellow-flowers.jpg",
			"yellowwildflowers.jpg", };

	/**
	 * The images corresponding to the image names specified above
	 */
	public final static Image[]		bgImage		= new Image[imageNames.length];
	/**
	 * How wide is each image, nominally?
	 */
	public final static int[]		imageWidth	= new int[imageNames.length];
	/**
	 * how tall is each image, nominally?
	 */
	public final static int[]		imageHeight	= new int[imageNames.length];
	/**
	 * The offset from the top of the screen to put the text
	 */
	public final static int[]		offsets		= new int[imageNames.length];

	/**
	 * Must be called by the ChannelSelector prior to startup! This was simply a "static" block, but this is entirely unnecessary
	 * for the Displays
	 */
	public final static void prepareSaverImages() {
		List<String> a = Arrays.asList(imageNames);
		Collections.shuffle(a); // Randomize the presentation
		int i = 0;
		for (String name : a) {
			ImageIcon icon = new ImageIcon("bin/gov/fnal/ppd/dd/images/" + name);
			bgImage[i] = icon.getImage();
			imageWidth[i] = icon.getIconWidth();
			imageHeight[i] = icon.getIconHeight();
			offsets[i] = (int) (50.0 + 150.0 * Math.random());
			i++;
		}
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
	// public final static int MESSAGING_SERVER_PORT = Integer.getInteger("ddisplay.messagingport", 49999);
	/**
	 * Where is the Web server? Controlled by system constant ddisplay.webserver
	 */
	public final static String	WEB_SERVER_NAME			= PropertiesFile.getProperty("webServer", "dynamicdisplays.fnal.gov");
	/**
	 * Where is the Web server? Controlled by system constant ddisplay.webserver
	 */
	private final static String	WEB_SERVER_FOLDER		= PropertiesFile.getProperty("webFolder", "");

	/**
	 * Do we use http or https?
	 */
	public final static String	WEB_PROTOCOL			= PropertiesFile.getProperty("defaultWebProtocol", "http");

	/**
	 * @return The web server prefix, dealing with whether or not there is a folder in there, too.
	 */
	public final static String getFullURLPrefix() {
		if (WEB_SERVER_FOLDER.length() > 0)
			return WEB_PROTOCOL + "://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER;

		return WEB_PROTOCOL + "://" + WEB_SERVER_NAME;
	}

	public static final String		SOFTWARE_FILE_ZIP				= PropertiesFile.getProperty("SoftwareFileZip");
	public static final String		UNZIP_PROGRAM_LOCATION			= PropertiesFile.getProperty("UnzipLocation");
	/**
	 * Where is the Database server? Controlled by system constant ddisplay.dbserver
	 */
	public static String			DATABASE_SERVER_NAME			= System.getProperty("ddisplay.dbserver");
	/**
	 * The database name, as in "USE " + DATABASE_NAME. Controlled by system constant ddisplay.dbname
	 */
	public static String			DATABASE_NAME					= System.getProperty("ddisplay.dbname");
	/**
	 * The username for accessing the database
	 */
	public static String			DATABASE_USER_NAME				= System.getProperty("ddisplay.dbusername");
	/**
	 * the password corresponding to the username that accesses the database. Note that this MUST be entered by hand for each time
	 * one runs an application. (This is not the actual password.)
	 */
	public static String			DATABASE_PASSWORD				= System.getProperty("ddisplay.dbpassword");
	/**
	 * Where is the XML server? This is the place where the XML schema is stored (8/2014: The only usage of this constant)
	 * Controlled by system constant ddisplay.xmlserver
	 */
	public final static String		XML_SERVER_NAME					= System.getProperty("ddisplay.xmlserver");

	/**
	 * The URL that is the single image display web page. This is a bit of a kludge!
	 */
	public final static String		SINGLE_IMAGE_DISPLAY			= PropertiesFile.getProperty("singleImageDisplay",
			getFullURLPrefix() + "/portfolioOneSlide.php?photo=");

	/**
	 * What is the signature of a URL that can lead to a "Bad NUC" showing a bad web page?
	 * 
	 */
	public final static String		URL_REQUIRING_LOTS_OF_GRAPHICS	= System.getProperty("ddisplay.animationurl",
			"^\\S+dynamicdisplays.fnal.gov/kenburns/portfolioDisplayChoice.php\\S+$");
	private final static Pattern	urlMatchingPattern				= Pattern.compile(URL_REQUIRING_LOTS_OF_GRAPHICS);

	/**
	 * 
	 * @param testURL
	 *            the URL to test
	 * @return Do we think that this URL is one of the ones that will cause a "bad NUC" to mess up?
	 */
	public static boolean isThisURLNeedAnimation(final String testURL) {
		System.out.println("\n\nREGEX url: '" + URL_REQUIRING_LOTS_OF_GRAPHICS + "'");
		System.out.println("URL to match: '" + testURL + "'");
		return urlMatchingPattern.matcher(testURL).matches();
	}

	/**
	 * A symbol for 1,000,000,000.
	 */
	public final static int			ONE_BILLION				= 1000000000;

	/**
	 * One second, expressed in milliseconds (e.g., 1000L)
	 */
	public final static long		ONE_SECOND				= 1000L;
	/**
	 * One Minute, expressed in milliseconds (e.g., 60000L)
	 */
	public final static long		ONE_MINUTE				= 60L * ONE_SECOND;
	/**
	 * 15 minutes, expressed in milliseconds (e.g., 900000L)
	 */
	public final static long		FIFTEEN_MINUTES			= 15L * ONE_MINUTE;
	/**
	 * One hour, expressed in milliseconds (e.g., 3600000L)
	 */
	public final static long		ONE_HOUR				= 60L * ONE_MINUTE;
	/**
	 * One day (24 hours), expressed in milliseconds (e.g., 86400000L)
	 */
	public final static long		ONE_DAY					= 24L * ONE_HOUR;
	/**
	 * Used in ChannelSelector to go to the splash screen
	 */
	public final static long		INACTIVITY_TIMEOUT		= 3L * ONE_MINUTE;
	/**
	 * Used in ChannelSelector
	 */
	public final static long		PING_INTERVAL			= 5L * ONE_SECOND;

	/**
	 * How long should the client wait between attempts to reconnect to the server. For displays, ONE_MINUTE is good. For
	 * Controllers, the user is going to be less patient!
	 */
	public static long				WAIT_FOR_SERVER_TIME	= ONE_MINUTE;

	/**
	 * The string that means "show me your colors"
	 */
	public final static String		SELF_IDENTIFY			= "http://identify";

	/**
	 * The string of a URL that means, perform a full and complete refresh of the page you are showing now
	 */
	public final static String		FORCE_REFRESH			= "http://refresh";

	/**
	 * The list of Displays in this instance of whatever program you are running. This is used in a couple of places.
	 */
	public static List<Display>		displayList				= null;

	/**
	 * <p>
	 * The default dwell time if the dwell time is zero. In other words, how long do we want to wait before the FireFox instance
	 * asks to refresh a web page. It has been my observation that this is needed for these reasons:
	 * 
	 * <ol>
	 * <li>Many pages really do need refreshing because they "end", like YouTube videos and PowerPoint presentations</li>
	 * <li>Many we pages just fail from time to time. For example, the webcam pages and the "Channel 13" accelerator display.</li>
	 * <ol>
	 * </p>
	 */
	public final static long		DEFAULT_DWELL_TIME		= 2 * ONE_HOUR;

	private static int				locationCode;
	private static List<Integer>	locationCodes			= new ArrayList<Integer>();

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

	/**
	 * @return the location (short) name corresponding to this index value
	 */
	public static String getLocationName() {
		checkName();
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

	static String messagingServerName = null;

	/**
	 * @return The name of our messaging server, e.g., roc-w-11.fnal.gov
	 */
	public static String getMessagingServerName() {
		checkName();
		return messagingServerName;
	}

	/**
	 * Where is the private key stored on the local file system.
	 * 
	 * On Linux (& MAC) the default will be $HOME/keystore/private\<ipname\> selector 00.key
	 * 
	 * On Windows the default will be %HOMEDIR%\\keystore\\private\<ipname\> selector 00.key
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
	 * Start a thread that runs every week (at, say, 0400, plus a random offset) to see if there is a new version of the code
	 * 
	 * @param isMessagingServer
	 *            Set to true if this is the messaging server. It will look for the updates a little earlier than all the other bits
	 */
	public static void prepareUpdateWatcher(boolean isMessagingServer) {
		// Skip update for the development machine! This can have horrible consequences if the place where the development is
		// happening all of the sudden replaces the code! Now, it probably won't be all THAT horrible since (a) the update does not
		// delete anything, and (b) the development machine will always have the latest version. But we don't want any accidents.
		
		for (String test : PropertiesFile.getProperty("DevelopmentMachine").split(":"))
			if (THIS_IP_NAME.equals(test)) {
				println(GlobalVariables.class,
						"This machine, " + THIS_IP_NAME + ", is a development machine -- Skipping automatic updates."
								+ "\n\t\t\t\t\t\tThe development machines are: "
								+ Arrays.toString(PropertiesFile.getProperty("DevelopmentMachine").split(":")));
				return;
			}
		
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

		Timer timer = new Timer();
		TimerTask myTimerTask = new CheckForUpdatesTimerTask();
		timer.schedule(myTimerTask, date.getTime(), period);
		
		// Maybe we should run this update task really frequently so the user can change the update period.  Nah.

		println(GlobalVariables.class, "------------------------- Versioning ------------------------------");
		println(GlobalVariables.class, "-- This is software version " + getSoftwareVersion());
		println(GlobalVariables.class, "-- The desired flavor of software is " + getFlavor(false) + ", but this can change.");
		if (period < 2 * ONE_HOUR)
			println(GlobalVariables.class,
					"-- Will check for updates every " + period / 1000 + " seconds starting at " + date.getTime());
		else
			println(GlobalVariables.class,
					"-- Will check for updates every " + (period / 1000 / 60 / 60) + " hours starting at " + date.getTime());

		println(GlobalVariables.class, "-------------------------------------------------------------------");
	}

	// ----------------------------------------------------------------------------------------------------------------

	public static FLAVOR getFlavor(boolean reset) {
		if ( reset ) PropertiesFile.reset();
		FLAVOR flavor = FLAVOR.PRODUCTION;
		if (PropertiesFile.getProperty("UpdateFlavor") != null) {
			try {
				flavor = FLAVOR.valueOf(PropertiesFile.getProperty("UpdateFlavor"));
			} catch (Exception e) {
				printlnErr(GlobalVariables.class, "Unrecognized software flavor in the configuration file: "
						+ PropertiesFile.getProperty("UpdateFlavor") + ". Will use " + FLAVOR.PRODUCTION);
				flavor = FLAVOR.PRODUCTION;
			}
		}
		return flavor;
	}

	/**
	 * Read the file, credentials.txt, for establishing connection to the database
	 */
	public static void credentialsSetup() {
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
			System.exit(-1);
			return;
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
	 * @return the contentOnDisplays
	 */
	public static ListOfExistingContent getContentOnDisplays() {
		return contentOnDisplays;
	}

	private GlobalVariables() {
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
}
