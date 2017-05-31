/*
 * Util
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.display.client.DisplayControllerMessagingAbstract;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * Some general utilities in the DD system
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class Util {
	// Some globals for identifying the servers in this system

	private static final String[]	days			= { "", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

	// TODO Replace this random selection of default page to show with the page that is specified in the database.

	/**
	 * The list of default URLs
	 */
	public static final String		DEFAULT_URLS[]	= { getFullURLPrefix() + "/kenburns/portfolioDisplay.php?exp=MINOS",
			getFullURLPrefix() + "/kenburns/portfolioDisplay.php?exp=MINERvA",
			getFullURLPrefix() + "/kenburns/portfolioDisplay.php?exp=MiniBooNE",
			getFullURLPrefix() + "/kenburns/portfolioDisplay.php?exp=MicroBooNE",
			getFullURLPrefix() + "/kenburns/portfolioDisplay.php?exp=Mu2e",
			getFullURLPrefix() + "/kenburns/portfolioDisplay.php?exp=gMinus2",
			getFullURLPrefix() + "/kenburns/portfolioDisplay.php?exp=SeaQuest",
			getFullURLPrefix() + "/kenburns/portfolioDisplay.php?exp=NOvA",
			getFullURLPrefix() + "/kenburns/portfolioDisplay.php?exp=DUNE-LBNF",
			getFullURLPrefix() + "/kenburns/portfolioDisplay.php?exp=NuMI", //
			"http://www-bd.fnal.gov/notifyservlet/www?project=HD&refresh=on&infolinks=none", //
			"http://elliottmccrory.com/clock/five.html", };

	/**
	 * The list of default names for URLS
	 */
	public static final String		DEFAULT_NAMES[]	= { "MINOS Pictures", "MINERvA Pictures", "MiniBooNE Pictures",
			"MicroBooNE Pictures", "Mu2e Pictures", "g-2 Pictures", "SeaQuest Pictures", "NOvA Pictures", "DUNE/LBNF Pictures",
			"NuMI Pictures", "Accelerator Status", "Clocks", };

	/**
	 * The default URL for a Display
	 */
	public static final String		MY_URL;
	/**
	 * The name that is associated with the default URL for a Display
	 */
	public static final String		MY_NAME;

	private static final int		ONE_BILLION		= 1000000000;
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
	 * @param url
	 *            The URL to show in this empty channel (which can be null)
	 * @return An empty channel
	 */
	public static SignageContent makeEmptyChannel(String url) {
		try {
			return new ChannelImpl(MY_NAME, ChannelCategory.PUBLIC, "This is a default channel",
					new URI(url == null ? MY_URL : url), 0, 0);
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

	/**
	 * @param content
	 *            The blob to be turned into a string
	 * @return the blob as a string
	 */
	public static String convertObjectToHexBlob(final Serializable content) {
		if (content == null)
			return null;
		String blob = "";
		try {
			ByteArrayOutputStream fout = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(content);

			byte[] bytes = fout.toByteArray();

			for (int i = 0; i < bytes.length; i++) {
				if ((0x000000ff & bytes[i]) < 16)
					blob += "0";
				blob += Integer.toHexString(0x000000ff & bytes[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return blob;
	}

	/**
	 * @param channelNumber
	 *            The channel number to look up
	 * @return The channel that is specified by this channel number
	 */
	public static SignageContent getChannelFromNumber(int channelNumber) {
		Channel retval = null;
		try {
			retval = new ChannelImpl("Fermilab", ChannelCategory.PUBLIC, "Fermilab", new URI("http://www.fnal.gov"), 0, 360000L);
		} catch (URISyntaxException e2) {
			e2.printStackTrace();
		}

		if (channelNumber == 0) {
			return retval;
		} else if (channelNumber < 0) {
			// The default channel is a list of channels. Build it!

			String query = "select Channel.Number as Number,Dwell,Name,Description,URL from Channel,ChannelList where ListNumber="
					+ (-channelNumber) + " and Channel.Number=ChannelList.Number ORDER BY SequenceNumber";

			println(DisplayControllerMessagingAbstract.class, " -- Getting default channel list: [" + query + "]");
			Connection connection;
			try {
				connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				return null;
			}

			List<SignageContent> channelList = new ArrayList<SignageContent>();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement();) {
					try (ResultSet rs = stmt.executeQuery(query);) {
						if (!rs.first()) {
							return retval;
						}
						do {
							int chanNum = rs.getInt("Number");
							int dwell = rs.getInt("Dwell");
							String name = rs.getString("Name");
							String desc = rs.getString("Description");
							String url = rs.getString("URL");

							Channel c = new ChannelImpl(name, ChannelCategory.PUBLIC, desc, new URI(url), chanNum, dwell);
							channelList.add(c);

						} while (rs.next());

						retval = new ChannelPlayList(channelList, 60000L);
						stmt.close();
						rs.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (SQLException ex) {
					System.err.println("It is likely that the DB server is down.  We'll try again later.");
					ex.printStackTrace();
				}
			}
		} else if (channelNumber < ONE_BILLION) {
			// Channel number is positive and "small": this is a single channel.
			//
			// WARNING -- We assume here that the automatically-generated "channelNumber" in the database is incremented, as
			// opposed to chosen randomly. It is possible that some future MySQL implementation will do random index number
			// generation, although this is unlikely. Something to be aware of!
			//
			String query = "SELECT * from Channel where Number=" + channelNumber;
			println(DisplayControllerMessagingAbstract.class, " -- Getting default channel: [" + query + "]");
			Connection connection;
			try {
				connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				return null;
			}

			synchronized (connection) {
				try (Statement stmt = connection.createStatement();) {
					try (ResultSet rs = stmt.executeQuery(query);) {
						if (rs.first()) { // Move to first returned row (there should only be one)
							String url = rs.getString("URL");
							int dwell = rs.getInt("DwellTime");
							String desc = rs.getString("Description");
							String name = rs.getString("Name");

							retval = new ChannelImpl(name, ChannelCategory.PUBLIC, desc, new URI(url), channelNumber, dwell);

							stmt.close();
							rs.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (SQLException ex) {
					System.err.println("It is likely that the DB server is down.  We'll try again later.");
					ex.printStackTrace();
				}
			}
		} else {
			// TODO This is an image. Look up that image number and then make the URL that shows this image.
			int portfolioID = ONE_BILLION - channelNumber;
			String query = "SELECT Filename,Description from Portfolio where PortfolioID=" + portfolioID + " AND Approval='Approved'";
			println(DisplayControllerMessagingAbstract.class, " -- Getting a portfolio image: [" + query + "]");
			Connection connection;
			try {
				connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				return null;
			}

			synchronized (connection) {
				try (Statement stmt = connection.createStatement();) {
					try (ResultSet rs = stmt.executeQuery(query);) {
						if (rs.first()) { // Move to first returned row (there should only be one)
							String url = getFullURLPrefix() + "/" + rs.getString("Filename");
							String desc = rs.getString("Description");

							retval = new ChannelImpl("PortfolioImage", ChannelCategory.PUBLIC, desc, new URI(url), channelNumber, 2*60*60*1000L);

							stmt.close();
							rs.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (SQLException ex) {
					System.err.println("It is likely that the DB server is down.  We'll try again later.");
					ex.printStackTrace();
				}
			}
		}

		return retval;
	}

	private static Thread	errorMessageThread	= null;
	private static boolean	threadIsMade		= false;

	/**
	 * Utility to launch one, and only one, error message
	 * 
	 * @param e
	 */
	public static void launchErrorMessage(final ActionEvent e) {
		if (threadIsMade)
			return;
		errorMessageThread = new Thread("ErrorMessagePopup") {
			public void run() {
				threadIsMade = true;
				int g = e.paramString().indexOf("ERROR") + 9;
				int h = e.paramString().indexOf(",when");
				String m = e.paramString().substring(g, h);
				if (m.length() == 0)
					JOptionPane.showMessageDialog(null, e, "Error in communications", JOptionPane.ERROR_MESSAGE);
				else
					JOptionPane.showMessageDialog(null, e.paramString().substring(g, h), "Error in communications",
							JOptionPane.ERROR_MESSAGE);
				// catchSleep(3000L);
				errorMessageThread = null;
				threadIsMade = false;
			}
		};
		errorMessageThread.start();
	}

}
