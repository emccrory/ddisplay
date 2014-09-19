package gov.fnal.ppd;

import gov.fnal.ppd.signage.Display;

import java.awt.Image;
import java.util.List;

import javax.swing.ImageIcon;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class GlobalVariables {
	/**
	 * Do we show in full screen or in a window?
	 */
	public static boolean			SHOW_IN_WINDOW			= Boolean.getBoolean("ddisplay.selector.inwindow");
	/**
	 * Is this a PUBLIC controller?
	 */
	public static boolean			IS_PUBLIC_CONTROLLER	= Boolean.getBoolean("ddisplay.selector.public");

	/**
	 * How long since last user activity?
	 */
	public static long				lastDisplayChange		= 0L;

	/**
	 * Which set of Displays are we controlling here?
	 */
	public static int				locationCode			= Integer.getInteger("ddisplay.selector.location", 0);

	/**
	 * Short name for the location of the displays
	 */
	private static String[]			locationName			= { "ROC-West", "ROC-East", "Elliott's Office Test", "Fermilab" };
	
	

	/**
	 * Long name for the location of the displays
	 */
	private static String[]			locationDescription		= { "Fermilab Experiments' Remote Operations Center, West Side",
			"Fermilab CMS/LHC Remote Operations Center, East Side", //
			"Fermilab Transfer Gallery", //
			"All Dynamic Displays at Fermilab", //
															};
	public static String getLocationName(int index) {
		if ( index < 0 || index >= locationName.length )
			return locationName[locationName.length-1];
		return locationName[index];
	}
	
	public static String getLocationDescription(int index) {
		if ( index < 0 || index >= locationDescription.length )
			return locationDescription[locationDescription.length-1];
		return locationDescription[index];
	} 
	
	
	public static final String[]	imageNames				= { "src/gov/fnal/ppd/images/fermilab3.jpg",
			"src/gov/fnal/ppd/images/fermilab1.jpg", //
			"src/gov/fnal/ppd/images/fermilab2.jpg", //
			"src/gov/fnal/ppd/images/fermilab4.jpg", //
			"src/gov/fnal/ppd/images/fermilab5.jpg", //
			"src/gov/fnal/ppd/images/fermilab6.jpg", //
			"src/gov/fnal/ppd/images/fermilab7.jpg", //
			"src/gov/fnal/ppd/images/fermilab8.jpg", //
			"src/gov/fnal/ppd/images/fermilab9.jpg", //
			"src/gov/fnal/ppd/images/fermilab10.jpg", //
			"src/gov/fnal/ppd/images/fermilab11.jpg", //
			"src/gov/fnal/ppd/images/fermilab12.jpg", //
			"src/gov/fnal/ppd/images/fermilab13.jpg", //
			"src/gov/fnal/ppd/images/fermilab15.jpg", //
			"src/gov/fnal/ppd/images/fermilab17.jpg", //
															};

	public final static Image[]		bgImage					= new Image[imageNames.length];
	public final static int[]		imageWidth				= new int[imageNames.length];
	public final static int[]		imageHeight				= new int[imageNames.length];
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
	public static int				INSET_SIZE				= 32;
	/**
	 * 
	 */
	public static float				FONT_SIZE				= 60.0f;

	/**
	 * The location of the default server for everything in this system
	 */
	public static final String		DEFAULT_SERVER			= "mccrory.fnal.gov";

	/**
	 * Where is the messaging server?
	 */
	public static final String		MESSAGING_SERVER_NAME	= System.getProperty("ddisplay.messagingserver", DEFAULT_SERVER);
	/**
	 * What port is the Messaging Server listing on? This is an easy to remember (I hope) prime number in the range of unassigned
	 * port number (49152 - 65535)
	 */
	public static final int			MESSAGING_SERVER_PORT	= Integer.getInteger("ddisplay.messagingport", 49999);
	/**
	 * Where is the Web server?
	 */
	public static final String		WEB_SERVER_NAME			= System.getProperty("ddisplay.webserver", DEFAULT_SERVER);
	/**
	 * Where is the Database server?
	 */
	public static final String		DATABASE_SERVER_NAME	= System.getProperty("ddisplay.dbserver", DEFAULT_SERVER);
	/**
	 * Where is the XML server? This is the place where the XML schema is stored (8/2014: The only usage of this constant)
	 */
	public static final String		XML_SERVER_NAME			= System.getProperty("ddisplay.xmlserver", DEFAULT_SERVER);

	/**
	 * One second, expressed in milliseconds (e.g., 1000L)
	 */
	public static final long		ONE_SECOND				= 1000L;
	/**
	 * 15 minutes, expressed in milliseconds
	 */
	public static final long		FIFTEEN_MINUTES			= 15L * 60L * ONE_SECOND;
	/**
	 * Used in ChannelSelector to go to the splash screen
	 */
	public static final long		INACTIVITY_TIMEOUT		= 120L * ONE_SECOND;
	/**
	 * Used in ChannelSelector
	 */
	public static final long		PING_INTERVAL			= 5L * ONE_SECOND;

	/**
	 * An identifier for the display facades
	 */
	public static String			PROGRAM_NAME			= "ChannelSelector";

	/**
	 * The string that means "show me your colors"
	 */
	public static final String		SELF_IDENTIFY			= "http://identify";

	/**
	 * The list of Displays in this instance of whatever program you are running. This is used in a couple of places.
	 */
	public static List<Display>		displayList;

	private GlobalVariables() {
	}
}
