/*
 * GlobalVariables
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.changer.ListOfExistingContent;
import gov.fnal.ppd.dd.display.client.ConnectionToFirefoxInstance;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;
import gov.fnal.ppd.dd.util.version.VersionInformationComparison;

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
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

/**
 * This is where all the global constant in the Dynamic Displays system are held.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class GlobalVariables {
	/**
	 * Do we show in full screen or in a window? Controlled by system constant, ddisplay.selector.inwindow
	 */
	public static boolean		SHOW_IN_WINDOW				= Boolean.getBoolean("ddisplay.selector.inwindow");
	/**
	 * Is this a PUBLIC controller? Controlled by system constant, ddisplay.selector.public
	 */
	public static boolean		IS_PUBLIC_CONTROLLER		= Boolean.getBoolean("ddisplay.selector.public");

	/**
	 * Is this a controller that is thought to be used by a docent?
	 */
	public static boolean		IS_DOCENT_CONTROLLER		= Boolean.getBoolean("ddisplay.selector.docent");

	/**
	 * Does the user want to have the database index for the display shown (default) or the virtual display numbers?
	 */
	public static boolean		SHOW_VIRTUAL_DISPLAY_NUMS	= Boolean.getBoolean("ddisplay.virtualdisplaynumbers");

	/**
	 * Does the user want to show and extended display name on the display buttons?
	 */
	public static boolean		SHOW_EXTENDED_DISPLAY_NAMES	= Boolean.getBoolean("ddisplay.extendeddisplaynnames");

	/**
	 * How long since last user activity?
	 */
	private static long			lastUserActivity			= 0L;

	/**
	 * String that says, "Do not check message signing". This is the only word that will turn off checking. All other words will
	 * result in checking.
	 */
	public static final String	NOCHECK_SIGNED_MESSAGE		= "nocheck";

	/**
	 * String that says, "Check message signing". This is the default.
	 */
	public static final String	CHECK_SIGNED_MESSAGE		= "check";

	/**
     */
	public static final boolean	RUN_RAISE_SELECTOR_BUTTON	= Boolean.getBoolean("ddisplay.selector.showraisebutton");

	/**
	 * <p>
	 * How should we deal with signed messages? The options are
	 * <ol>
	 * <li>nocheck -- do not check the signature on any signed object</li>
	 * <li>check -- Check the signature and complain if the signature is bad, but use the message no matter what the check says</li>
	 * </ol>
	 * </p>
	 */

	private static String		checkSignedMessage			= System.getProperty("ddisplay.checksignedmessage",
																	CHECK_SIGNED_MESSAGE);

	/**
	 * @return Do we need to check the signature on messages?
	 */
	public static boolean checkSignedMessages() {
		return !NOCHECK_SIGNED_MESSAGE.equals(checkSignedMessage);
	}

	/**
	 * My IP Name
	 */
	public static String		THIS_IP_NAME;
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
	public static final String	THIS_IP_NAME_INSTANCE	= System.getProperty("ddisplay.selectorinstance", "00");

	/**
	 * @return The messaging name for this selector client.
	 */
	public static final String getFullSelectorName() {
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
	public static final String[]	imageNames	= { "bin/gov/fnal/ppd/dd/images/fermilab3.jpg",
			"bin/gov/fnal/ppd/dd/images/fermilab1.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab2.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab4.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab5.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab6.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab7.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab8.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab9.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab10.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab11.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab12.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab13.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab14.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab15.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab16.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab17.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab18.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab19.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab20.jpg", //
			"bin/gov/fnal/ppd/dd/images/fermilab21.jpg", //
			"bin/gov/fnal/ppd/dd/images/american-lotus-2.jpg", //
			"bin/gov/fnal/ppd/dd/images/auditorium-stairs.jpg", //
			"bin/gov/fnal/ppd/dd/images/behind-tall-grass.jpg", //
			"bin/gov/fnal/ppd/dd/images/bright-tevatron-tunnel.jpg", //
			"bin/gov/fnal/ppd/dd/images/coyote-wilson-hall.jpg", //
			"bin/gov/fnal/ppd/dd/images/egret-spots.jpg", //
			"bin/gov/fnal/ppd/dd/images/feynman-fountain.jpg", //
			"bin/gov/fnal/ppd/dd/images/iarc-angles.jpg", //
			"bin/gov/fnal/ppd/dd/images/july-fourth-coyote.jpg", //
			"bin/gov/fnal/ppd/dd/images/lightning-storm-over-fermilab.jpg", //
			"bin/gov/fnal/ppd/dd/images/main-ring-magnets.jpg", //
			"bin/gov/fnal/ppd/dd/images/pom-pom.jpg", //
			"bin/gov/fnal/ppd/dd/images/ramsey-wilson.jpg", //
			"bin/gov/fnal/ppd/dd/images/sunset-at-caseys-pond.jpg", //
			"bin/gov/fnal/ppd/dd/images/water-moon-venus.jpg", //
			"bin/gov/fnal/ppd/dd/images/wilson-hall-moon-airplane.jpg", //
			"bin/gov/fnal/ppd/dd/images/yellow-flowers.jpg", //
			"bin/gov/fnal/ppd/dd/images/yellowwildflowers.jpg", //
												};

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
	public static final void prepareSaverImages() {
		for (int i = 0; i < imageNames.length; i++) {
			ImageIcon icon = new ImageIcon(imageNames[i]);
			bgImage[i] = icon.getImage();
			imageWidth[i] = icon.getIconWidth();
			imageHeight[i] = icon.getIconHeight();
			offsets[i] = (int) (50.0 + 100.0 * Math.random());
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
	// public static final String MESSAGING_SERVER_NAME = System.getProperty("ddisplay.messagingserver", DEFAULT_SERVER);

	/**
	 * What port is the Messaging Server listing on? This is an easy to remember (I hope) prime number in the range of unassigned
	 * port number (49152 - 65535) Controlled by system constant ddisplay.messagingserver
	 */
	public static final int		MESSAGING_SERVER_PORT	= Integer.getInteger("ddisplay.messagingport", 49999);
	/**
	 * Where is the Web server? Controlled by system constant ddisplay.webserver
	 */
	public static final String	WEB_SERVER_NAME			= System.getProperty("ddisplay.webserver", "dynamicdisplays.fnal.gov");
	/**
	 * Where is the Web server? Controlled by system constant ddisplay.webserver
	 */
	private static final String	WEB_SERVER_FOLDER		= System.getProperty("ddisplay.webfolder", "");

	/**
	 * @return The web server prefix, dealing with whether or not there is a folder in there, too.
	 */
	public static final String getFullURLPrefix() {
		if (WEB_SERVER_FOLDER.length() > 0)
			return "https://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER;
		return "https://" + WEB_SERVER_NAME;
	}

	/**
	 * Where is the Database server? Controlled by system constant ddisplay.dbserver
	 */
	public static String			DATABASE_SERVER_NAME			= System.getProperty("ddisplay.dbserver",
																			"fnalmysqldev.fnal.gov:3311");
	/**
	 * The database name, as in "USE " + DATABASE_NAME. Controlled by system constant ddisplay.dbname
	 */
	public static String			DATABASE_NAME					= System.getProperty("ddisplay.dbname", "xoc_dev");
	/**
	 * The username for accessing the database
	 */
	public static String			DATABASE_USER_NAME				= System.getProperty("ddisplay.dbusername", "no included here");
	/**
	 * the password corresponding to the username that accesses the database. Note that this MUST be entered by hand for each time
	 * one runs an application. (This is not the actual password.)
	 */
	public static String			DATABASE_PASSWORD				= System.getProperty("ddisplay.dbpassword",
																			"I'm not telling :-)");
	/**
	 * Where is the XML server? This is the place where the XML schema is stored (8/2014: The only usage of this constant)
	 * Controlled by system constant ddisplay.xmlserver
	 */
	public static final String		XML_SERVER_NAME					= System.getProperty("ddisplay.xmlserver",
																			"dynamicdisplays.fnal.gov");

	/**
	 * The URL that is the single image display web page. This is a bit of a kludge!
	 */
	public static final String		SINGLE_IMAGE_DISPLAY			= System.getProperty("ddisplay.singleimagedisplay",
																			"https://dynamicdisplays.fnal.gov/portfolioOneSlide.php?photo=");

	/**
	 * What is the signature of a URL that can lead to a "Bad NUC" showing a bad web page?
	 * 
	 */
	public static final String		URL_REQUIRING_LOTS_OF_GRAPHICS	= System.getProperty("ddisplay.animationurl",
																			"^\\S+dynamicdisplays.fnal.gov/kenburns/portfolioDisplayChoice.php\\S+$");
	private static final Pattern	urlMatchingPattern				= Pattern.compile(URL_REQUIRING_LOTS_OF_GRAPHICS);

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
	public static final int			ONE_BILLION				= 1000000000;

	/**
	 * One second, expressed in milliseconds (e.g., 1000L)
	 */
	public static final long		ONE_SECOND				= 1000L;
	/**
	 * One Minute, expressed in milliseconds (e.g., 60000L)
	 */
	public static final long		ONE_MINUTE				= 60L * ONE_SECOND;
	/**
	 * 15 minutes, expressed in milliseconds (e.g., 900000L)
	 */
	public static final long		FIFTEEN_MINUTES			= 15L * ONE_MINUTE;
	/**
	 * One hour, expressed in milliseconds (e.g., 3600000L)
	 */
	public static final long		ONE_HOUR				= 60L * ONE_MINUTE;
	/**
	 * One day (24 hours), expressed in milliseconds (e.g., 86400000L)
	 */
	public static final long		ONE_DAY					= 24L * ONE_HOUR;
	/**
	 * Used in ChannelSelector to go to the splash screen
	 */
	public static final long		INACTIVITY_TIMEOUT		= 3L * ONE_MINUTE;
	/**
	 * Used in ChannelSelector
	 */
	public static final long		PING_INTERVAL			= 5L * ONE_SECOND;

	/**
	 * How long should the client wait between attempts to reconnect to the server. For displays, ONE_MINUTE is good. For
	 * Controllers, the user is going to be less patient!
	 */
	public static long				WAIT_FOR_SERVER_TIME	= ONE_MINUTE;
	/**
	 * An identifier for the display facades
	 */
	public static String			PROGRAM_NAME			= "ChannelSelector";

	/**
	 * The string that means "show me your colors"
	 */
	public static final String		SELF_IDENTIFY			= "http://identify";

	/**
	 * The string of a URL that means, perform a full and complete refresh of the page you are shwowing now
	 */
	public static final String		FORCE_REFRESH			= "http://refresh";

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
	public static final long		DEFAULT_DWELL_TIME		= 2 * ONE_HOUR;

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
		} else {
			if (locationCodes.contains(lc) || locationCodes.contains(-1))
				return false;
			return locationCodes.add(lc);
		}
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

	static String	messagingServerName	= null;

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

	// ----------------------------------------------------------------------------------------------------------------
	// A class and a method for checking to see if there is update software available

	private static class SampleTask extends TimerTask {
		Thread	myThreadObj;

		SampleTask(Thread t) {
			this.myThreadObj = t;
		}

		public void run() {
			myThreadObj.start();
		}
	}

	/**
	 * Start a thread that runs every week (say, Tuesday mornings at 0400, plus a random offset) to see if there is a new version of
	 * the code ready to be downloaded
	 * 
	 * @param isMessagingServer
	 *            Set to true if this is the messaging server. It will look for the updates a little earlier than all the other bits
	 */
	public static void prepareUpdateWatcher(boolean isMessagingServer) {

		Timer timer = new Timer();
		Thread myThread = new Thread("CheckForSoftwareUpdate") {

			@Override
			public void run() {
				println(GlobalVariables.class, "Checking to see if there is an update for the software");
				// 1. See if an update is available
				double days = VersionInformationComparison.lookup(FLAVOR.PRODUCTION, false);
				if (days > 0) {
					// 2. If so, download it. Then exit the entire process with System.exit(-1) and we will restart.
					println(GlobalVariables.class, "There is a version of the software that is " + days
							+ " days newer than the code we are running.");

					// Download and install the new code
					downloadNewSoftware();
					// System.exit(-1); - This will signal the controlling shell script to restart the program.
				} else {
					println(GlobalVariables.class, "No new software is present.  Will check again next week at this time.");
				}
			}
		};
		Calendar date = Calendar.getInstance();
		date.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
		if (isMessagingServer) {
			// The messaging server needs to get the software first
			date.set(Calendar.HOUR_OF_DAY, 3);
			date.set(Calendar.MINUTE, 0);
		} else {
			date.set(Calendar.HOUR_OF_DAY, 4);
			date.set(Calendar.MINUTE, (int) (Math.random() * 30.0)); // Randomly in the first half of the 4AM hour
		}
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 0);

		date.add(Calendar.HOUR, 168); // Delay the first one to next week.

		println(GlobalVariables.class, "Will check for updates every week starting at " + date.getTime());

		// Run myThread at 04:xx and then repeat every week at that time
		timer.schedule(new SampleTask(myThread), date.getTime(), 1000 * 60 * 60 * 24 * 7);
	}

	// ----------------------------------------------------------------------------------------------------------------

	protected static void downloadNewSoftware() {
		println(GlobalVariables.class, " Experimental implementatiion of refresh-&-restart code.");

		// This code assumes a very specific location for the refresh script, and this is in a Linux environment. This will be
		// different on Windows PCs.

		try {
			ProcessBuilder pb = new ProcessBuilder("refreshSoftware.sh");
			pb.directory(new File("../.."));
			@SuppressWarnings("unused")
			Process p = pb.start();
			// This command *should* work on Linux, and it *should* throw an exception on Windows

			// Save the current channel and exit (so it can be restarted)
			ConnectionToFirefoxInstance.saveAndExit();
		} catch (Exception e) {
			e.printStackTrace();
		}

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
			Scanner scanner = new Scanner(file);
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
			}/*
			 * else{ result_line=""; }
			 */
		}

		return result_line;
	}

	private static ListOfExistingContent	contentOnDisplays	= new ListOfExistingContent();

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
