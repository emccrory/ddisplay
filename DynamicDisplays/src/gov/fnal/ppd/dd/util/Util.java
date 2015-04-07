package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_FOLDER;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_NAME;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Some general utilities in the DD system
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class Util {
	// Some globals for identifying the servers in this system

	private static final String[]	days			= { "", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

	// TODO Replace this random selection of default page to show with the page that is specified in the database.

	/**
	 * The list of default URLs
	 */
	public static final String		DEFAULT_URLS[]	= {
			"http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/kenburns/portfolioDisplay.php?exp=MINOS",
			"http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/kenburns/portfolioDisplay.php?exp=MINERvA",
			"http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/kenburns/portfolioDisplay.php?exp=MiniBooNE",
			"http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/kenburns/portfolioDisplay.php?exp=MicroBooNE",
			"http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/kenburns/portfolioDisplay.php?exp=Mu2e",
			"http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/kenburns/portfolioDisplay.php?exp=gMinus2",
			"http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/kenburns/portfolioDisplay.php?exp=SeaQuest",
			"http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/kenburns/portfolioDisplay.php?exp=NOvA",
			"http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/kenburns/portfolioDisplay.php?exp=DUNE",
			"http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/kenburns/portfolioDisplay.php?exp=NuMI", //
			"http://www-bd.fnal.gov/notifyservlet/www?project=HD&refresh=on&infolinks=none", //
			"http://elliottmccrory.com/clock/five.html", };

	/**
	 * The list of default names for URLS
	 */
	public static final String		DEFAULT_NAMES[]	= { "MINOS Pictures", "MINERvA Pictures", "MiniBooNE Pictures",
			"MicroBooNE Pictures", "Mu2e Pictures", "g-2 Pictures", "SeaQuest Pictures", "NOvA Pictures", "DUNE Pictures",
			"NuMI Pictures", "Accelerator Status", "Clocks", };

	/**
	 * The default URL for a Display
	 */
	public static final String		MY_URL;
	/**
	 * The name that is associated with the default URL for a Display
	 */
	public static final String		MY_NAME;
	static {
		int index = (int) (DEFAULT_URLS.length * Math.random());
		MY_URL = DEFAULT_URLS[index];
		MY_NAME = DEFAULT_NAMES[index];
	}

	private Util() {
		// Not implement--cannot be instantiated.
	}

	/**
	 * Truncate a long string. Add the ellipsis if the string is too long
	 * 
	 * @param name
	 *            The string to shorten
	 * @param chars
	 *            The number of characters to shorten it to
	 * @return the truncated string
	 */
	public static final String truncate(final String name, final int chars) {
		if (name.length() <= chars)
			return name;
		return name.substring(0, chars - 3) + " \u2026 " + name.substring(name.length() - 3);
	}

	/**
	 * Truncate a string to 30 characters, adding an ellipsis if it is too long
	 * 
	 * @param name
	 *            The string to truncate
	 * @return The truncated string
	 */
	public static final String truncate(final String name) {
		return truncate(name, 30);
	}

	/**
	 * return a two-digit representation of a number, prepending a 0 for numbers less than 10
	 * 
	 * @param v
	 *            A number to encode, less than 100
	 * @return The encoded two-character string
	 */
	public static final String twoDigits(final int v) {
		assert (v < 100);
		if (v < 10)
			return "0" + v;
		return "" + v;
	}

	/**
	 * @return The current time stamp as "Mon 12:43:17"
	 */
	public static final String shortDate() {
		Calendar c = new GregorianCalendar();
		return days[c.get(Calendar.DAY_OF_WEEK)] + " " + twoDigits(c.get(Calendar.HOUR_OF_DAY)) + ":"
				+ twoDigits(c.get(Calendar.MINUTE)) + ":" + twoDigits(c.get(Calendar.SECOND));
	}

	/**
	 * Prints a short memory reckoning every 5 minutes
	 */
	public static void launchMemoryWatcher() {
		new Thread("MemoryWatcher.DEBUG") {
			public void run() {
				long time = FIFTEEN_MINUTES / 512;
				double sq2 = Math.sqrt(2.0);
				while (true) {
					catchSleep(time);
					if (time < FIFTEEN_MINUTES) {
						time *= sq2;
						if (time >= FIFTEEN_MINUTES) {
							time = FIFTEEN_MINUTES;
						}
					}
					int tCount = java.lang.Thread.activeCount();
					int activeTCount = ManagementFactory.getThreadMXBean().getThreadCount();
					long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;
					long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
					long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
					System.out.println("Threads: " + tCount + " (" + activeTCount + "), mem: " + total + "M " + free + "M " + max
							+ "M at " + (new Date()) + " (Sleep " + (time / 1000) + " sec.)");
				}
			}
		}.start();
	}

	/**
	 * @return An empty channel
	 */
	public static SignageContent makeEmptyChannel() {
		try {
			return new ChannelImpl(MY_NAME, ChannelCategory.PUBLIC, "This is a default channel", new URI(MY_URL), 0, 0);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * A helper function to call Thread.sleep() and not throw an exception.
	 * 
	 * @param time
	 */
	public static void catchSleep(final long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param clazz
	 *            The type of the caller
	 * @param message
	 *            the message to print
	 */
	public static void println(Class<?> clazz, String message) {
		System.out.println(new Date() + " -- " + clazz.getSimpleName() + message);
	}

	/**
	 * @param d
	 *            The display to identify
	 * @return A unique string to identify this display.
	 */
	public static String getDisplayID(final Display d) {
		return "Display " + d.getVirtualDisplayNumber() + "/" + d.getDBDisplayNumber();

	}
}
