/*
 * GlobalVariables
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd;

import gov.fnal.ppd.dd.signage.Display;

import java.awt.Image;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

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
	 * Does the user want to have the database index for the display shown (default) or the virtual display numbers?
	 */
	public static boolean		SHOW_VIRTUAL_DISPLAY_NUMS	= Boolean.getBoolean("ddisplay.virtualdisplaynumbers");

	/**
	 * How long since last user activity?
	 */
	public static long			lastDisplayChange			= 0L;

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
	}

	/**
	 * 
	 */
	public static final String	THIS_IP_NAME_INSTANCE	= System.getProperty("ddisplay.selectorinstance", "00");

	/**
	 * Where is the private key stored on the local file system.
	 * 
	 * On Linux (& MAC) the default will be $HOME/keystore/private\<ipname\> selector 00.key
	 * 
	 * On Windows the default will be %HOMEDIR%\\keystore\\private\<ipname\> selector 00.key
	 * 
	 */
	public static final String	PRIVATE_KEY_LOCATION;

	static {
		String def = ("/keystore/private" + THIS_IP_NAME + " selector " + THIS_IP_NAME_INSTANCE + ".key").toLowerCase();
		if (System.getenv("HOME") != null) // Linux and MAC
			def = System.getenv("HOME") + def;
		else if (System.getenv("UserProfile") != null) // Windows
			def = System.getenv("UserProfile") + def;
		PRIVATE_KEY_LOCATION = System.getProperty("ddisplay.privatekeyfilename", def);
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
	public static final String[]	imageNames				= { "bin/gov/fnal/ppd/dd/images/fermilab3.jpg",
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
															};

	/**
	 * The images corresponding to the image names specified above
	 */
	public final static Image[]		bgImage					= new Image[imageNames.length];
	/**
	 * How wide is each image, nominally?
	 */
	public final static int[]		imageWidth				= new int[imageNames.length];
	/**
	 * how tall is each image, nominally?
	 */
	public final static int[]		imageHeight				= new int[imageNames.length];
	/**
	 * The offset from the top of the screen to put the text
	 */
	public final static int[]		offsets					= new int[imageNames.length];

	static {
		for (int i = 0; i < imageNames.length; i++) {
			ImageIcon icon = new ImageIcon(imageNames[i]);
			bgImage[i] = icon.getImage();
			imageWidth[i] = icon.getIconWidth();
			imageHeight[i] = icon.getIconHeight();
			offsets[i] = (int) (50.0 + 500.0 * Math.random());
		}
	}
	/**
	 * 
	 */
	public static int				INSET_SIZE				= 4;
	/**
	 * 
	 */
	public static float				FONT_SIZE				= 40.0f;

	// /**
	// * Where is the messaging server? Controlled by system constant, ddisplay.messagingserver
	// */
	// public static final String MESSAGING_SERVER_NAME = System.getProperty("ddisplay.messagingserver", DEFAULT_SERVER);

	/**
	 * What port is the Messaging Server listing on? This is an easy to remember (I hope) prime number in the range of unassigned
	 * port number (49152 - 65535) Controlled by system constant ddisplay.messagingserver
	 */
	public static final int			MESSAGING_SERVER_PORT	= Integer.getInteger("ddisplay.messagingport", 49999);
	/**
	 * Where is the Web server? Controlled by system constant ddisplay.webserver
	 */
	public static final String		WEB_SERVER_NAME			= System.getProperty("ddisplay.webserver", "dynamicdisplays.fnal.gov");
	/**
	 * Where is the Web server? Controlled by system constant ddisplay.webserver
	 */
	private static final String		WEB_SERVER_FOLDER		= System.getProperty("ddisplay.webfolder", "XOC");

	/**
	 * @return The web server prefix, dealing with whether or not there is a folder in there, too.
	 */
	public static final String getFullURLPrefix() {
		if (WEB_SERVER_FOLDER.length() > 0)
			return "http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER;
		return "http://" + WEB_SERVER_NAME;
	}

	/**
	 * Where is the Database server? Controlled by system constant ddisplay.dbserver
	 */
	public static final String	DATABASE_SERVER_NAME	= System.getProperty("ddisplay.dbserver", "fnalmysqldev.fnal.gov:3311");
	/**
	 * The database name, as in "USE " + DATABASE_NAME. Controlled by system constant ddisplay.dbname
	 */
	public static final String	DATABASE_NAME			= System.getProperty("ddisplay.dbname", "xoc_dev");
	/**
	 * The username for accessing the database
	 */
	public static final String	DATABASE_USER_NAME		= System.getProperty("ddisplay.dbusername", "no included here");
	/**
	 * the password corresponding to the username that accesses the database. Note that this MUST be entered by hand for each time
	 * one runs an application. (This is not the actual password.)
	 */
	public static final String	DATABASE_PASSWORD		= System.getProperty("ddisplay.dbpassword", "I'm not telling :-)");
	/**
	 * Where is the XML server? This is the place where the XML schema is stored (8/2014: The only usage of this constant)
	 * Controlled by system constant ddisplay.xmlserver
	 */
	public static final String	XML_SERVER_NAME			= System.getProperty("ddisplay.xmlserver", "dynamicdisplays.fnal.gov");
	/**
	 * One second, expressed in milliseconds (e.g., 1000L)
	 */
	public static final long	ONE_SECOND				= 1000L;
	/**
	 * One Minute, expressed in milliseconds (e.g., 60000L)
	 */
	public static final long	ONE_MINUTE				= 60L * ONE_SECOND;
	/**
	 * 15 minutes, expressed in milliseconds (e.g., 900000L)
	 */
	public static final long	FIFTEEN_MINUTES			= 15L * ONE_MINUTE;
	/**
	 * One hour, expressed in milliseconds (e.g., 3600000L)
	 */
	public static final long	ONE_HOUR				= 60L * ONE_MINUTE;
	/**
	 * One day (24 hours), expressed in milliseconds (e.g., 86400000L)
	 */
	public static final long	ONE_DAY					= 24L * ONE_HOUR;
	/**
	 * Used in ChannelSelector to go to the splash screen
	 */
	public static final long	INACTIVITY_TIMEOUT		= 120L * ONE_SECOND;
	/**
	 * Used in ChannelSelector
	 */
	public static final long	PING_INTERVAL			= 5L * ONE_SECOND;

	/**
	 * An identifier for the display facades
	 */
	public static String		PROGRAM_NAME			= "ChannelSelector";

	/**
	 * The string that means "show me your colors"
	 */
	public static final String	SELF_IDENTIFY			= "http://identify";

	/**
	 * The list of Displays in this instance of whatever program you are running. This is used in a couple of places.
	 */
	public static List<Display>	displayList;

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
	public static final long	DEFAULT_DWELL_TIME		= 2 * ONE_HOUR;

	/**
	 * Which set of Displays are we controlling here? Controlled by system constant, ddisplay.selector.location
	 */
	public static int			locationCode			= Integer.getInteger("ddisplay.selector.location", 0);

	/**
	 * Short name for the location of the displays
	 */
	static String		locationName;
	// {
	// "ROC-West",
	// "ROC-East",
	// "Elliott's Office Test",
	// "WH Second Floor",
	// "Accelerator Division",
	// "FESS Operations",
	// "Fermilab"
	// };

	/**
	 * Long name for the location of the displays
	 */
	static String		locationDescription;

	// { "Fermilab Experiments' Remote Operations Center, West Side",
	// "Fermilab CMS/LHC Remote Operations Center, East Side", //
	// "Fermilab Transfer Gallery", //
	// "Second Floor of Wilson Hall", //
	// "Accelerator Division Computer Room Displays", //
	// "FESS Operations at Site 38", //
	// "All Dynamic Displays at Fermilab", //
	// };

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

	public static String getMessagingServerName() {
		checkName(); 
		return messagingServerName;
	}

	private GlobalVariables() {
	}
}
