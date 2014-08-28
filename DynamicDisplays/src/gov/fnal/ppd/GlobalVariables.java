package gov.fnal.ppd;

public class GlobalVariables {
	/**
	 * Do we show in full screen or in a window?
	 */
	public static boolean	SHOW_IN_WINDOW			= Boolean.getBoolean("signage.selector.inwindow");
	/**
	 * Is this a PUBLIC controller?
	 */
	public static boolean	IS_PUBLIC_CONTROLLER	= Boolean.getBoolean("signage.selector.public");

	/**
	 * How long since last user activity?
	 */
	public static long		lastDisplayChange		= System.currentTimeMillis();

	/**
	 * 
	 */
	public static int		INSET_SIZE				= 32;
	/**
	 * 
	 */
	public static float		FONT_SIZE				= 60.0f;

	private GlobalVariables() {
	}
}
