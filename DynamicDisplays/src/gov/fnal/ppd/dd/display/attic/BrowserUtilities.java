package gov.fnal.ppd.dd.display.attic;

/**
 * Some utility functions for the operating system
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class BrowserUtilities {
	/**
	 * The operating system as determined by the propery, os.name
	 */
	public static final String	OS	= System.getProperty("os.name").toLowerCase();

	/**
	 * @return Is this a Microsoft Windows system?
	 */
	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	/**
	 * @return Is this an Apple MacIntosh system?
	 */
	public static boolean isMac() {
		return (OS.indexOf("mac") >= 0);
	}

	/**
	 * @return Is this some flavor of Unix?
	 */
	public static boolean isUnix() {
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
	}

	/**
	 * @return Is this a Solaris/SunOS system?
	 */
	public static boolean isSolaris() {
		return (OS.indexOf("sunos") >= 0);
	}

	// public static BrowserType getBrowserType() {
	// BrowserType bt = BrowserType.Mozilla;
	//
	// if ( isMac() )
	// bt = BrowserType.Safari;
	//
	// // if (isUnix())
	// // bt = BrowserType.Mozilla;
	// // else if (isWindows())
	// // bt = BrowserType.Mozilla;
	// // else if (isMac())
	// // bt = BrowserType.Safari;
	// // else if (isSolaris())
	// // bt = BrowserType.Mozilla;
	// return bt;
	// }
}
