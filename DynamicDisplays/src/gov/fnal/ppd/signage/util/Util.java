package gov.fnal.ppd.signage.util;

import static gov.fnal.ppd.GlobalVariables.FIFTEEN_MINUTES;

import java.lang.management.ManagementFactory;
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

	private static final String[]	days					= { "", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

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
					try {
						sleep(time);
					} catch (InterruptedException e) {
					}
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
}
