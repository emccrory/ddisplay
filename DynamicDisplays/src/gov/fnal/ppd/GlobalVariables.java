package gov.fnal.ppd;

import gov.fnal.ppd.signage.Display;

import java.util.List;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class GlobalVariables {
	/**
	 * Do we show in full screen or in a window?
	 */
	public static boolean		SHOW_IN_WINDOW			= Boolean.getBoolean("signage.selector.inwindow");
	/**
	 * Is this a PUBLIC controller?
	 */
	public static boolean		IS_PUBLIC_CONTROLLER	= Boolean.getBoolean("signage.selector.public");

	/**
	 * How long since last user activity?
	 */
	public static long			lastDisplayChange		= System.currentTimeMillis();

	/**
	 * 
	 */
	public static int			INSET_SIZE				= 32;
	/**
	 * 
	 */
	public static float			FONT_SIZE				= 60.0f;

	/**
	 * The location of the default server for everything in this system
	 */
	public static final String	DEFAULT_SERVER			= "mccrory.fnal.gov";

	/**
	 * Where is the messaging server?
	 */
	public static final String	MESSAGING_SERVER_NAME	= System.getProperty("signage.messagingserver",
																GlobalVariables.DEFAULT_SERVER);
	/**
	 * What port is the Messaging Server listing on?
	 */
	public static final int		MESSAGING_SERVER_PORT	= 1500;
	/**
	 * Where is the Web server?
	 */
	public static final String	WEB_SERVER_NAME			= System.getProperty("signage.webserver", GlobalVariables.DEFAULT_SERVER);
	/**
	 * Where is the Database server?
	 */
	public static final String	DATABASE_SERVER_NAME	= System.getProperty("signage.dbserver", GlobalVariables.DEFAULT_SERVER);
	/**
	 * Where is the XML server? This is the place where the XML schema is stored (8/2014: The only usage of this constant)
	 */
	public static final String	XML_SERVER_NAME			= System.getProperty("signage.xmlserver", GlobalVariables.DEFAULT_SERVER);

	/**
	 * One second, expressed in milliseconds (e.g., 1000L)
	 */
	public static final long	ONE_SECOND				= 1000L;
	/**
	 * 15 minutes, expressed in milliseconds
	 */
	public static final long	FIFTEEN_MINUTES			= 15L * 60L * ONE_SECOND;
	/**
	 * Used in ChannelSelector
	 */
	public static final long	INACTIVITY_TIMEOUT		= 60L * ONE_SECOND;
	/**
	 * Used in ChannelSelector
	 */
	public static final long	PING_INTERVAL			= 5L * ONE_SECOND;

	/**
	 * An identifier for the display facades
	 */
	public static String		PROGRAM_NAME			= "ChannelSelector";

	/**
	 * 
	 */
	public static final String	SELF_IDENTIFY			= "http://identify";

	public static List<Display>	displayList;

	private GlobalVariables() {
	}
}
