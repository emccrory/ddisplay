/*
 * Util
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_BILLION;
import static gov.fnal.ppd.dd.GlobalVariables.SINGLE_IMAGE_DISPLAY;

import java.awt.event.ActionEvent;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import gov.fnal.ppd.dd.GlobalVariables;
import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.display.client.DisplayControllerMessagingAbstract;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.xml.ChangeChannelList;
import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;

/**
 * Some general utilities in the DD system
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class Util {
	private static final String[]	days	= { "", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

	/**
	 * The default URL for a Display
	 */
	public static final String		MY_URL	= "https://dynamicdisplays.fnal.gov/standby.html";
	/**
	 * The name that is associated with the default URL for a Display
	 */
	public static final String		MY_NAME	= "unspecified content";

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

	public static void launchMemoryWatcher() {
		launchMemoryWatcher(null);
	}

	/**
	 * Prints a short memory reckoning every 5 minutes
	 */
	public static void launchMemoryWatcher(final Logger logger) {
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
					String msg = "Threads: " + tCount + " (" + activeTCount + "), mem: " + total + "M " + free + "M " + max
							+ "M at " + (new Date()) + " (Sleep " + (time / 1000) + " sec.)";
					if (logger != null) {
						logger.fine(msg);
					} else {
						System.out.println(msg);
					}
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
			return new ChannelImpl(MY_NAME, ChannelClassification.MISCELLANEOUS, "This is a default channel",
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
		//
		// Here is a nice idea: Suppress repeated messages
		//
		if (GlobalVariables.getLogger() != null) {
			GlobalVariables.getLogger().fine(message);
		} else {
			String className = getClassName(clazz);
			if (message.startsWith(":") || message.startsWith(".") || message.startsWith(" -"))
				System.out.println(new Date() + " - " + className + message);
			else
				System.out.println(new Date() + " - " + className + ": " + message);
		}
	}

	/**
	 * @param clazz
	 *            The type of the caller
	 * @param message
	 *            the message to print
	 */
	public static void printlnErr(Class<?> clazz, String message) {
		//
		// Here is a nice idea: Suppress repeated messages
		//
		if (GlobalVariables.getLogger() != null) {
			GlobalVariables.getLogger().warning(message);
		} else {
			String className = getClassName(clazz);
			if (message.startsWith(":") || message.startsWith("."))
				System.err.println(new Date() + " - " + className + message);
			else
				System.err.println(new Date() + " - " + className + ": " + message);
		}
	}

	/**
	 * Return a relevant string for this class. If this is an anonymous class of some sort, it tries first to figure out the name of
	 * the enclosing class and then returns it with an appended caret, "^". Otherwise, it returns the name of the super class with a
	 * prepended caret. E.g., for an enclosing class of name EnclosingClass, the string it would return is EnclosingClass^. If there
	 * is no enclosing class, it will return ^BaseClassName.
	 * 
	 * @param clazz
	 *            The Class object to name
	 * @return The best possible name for this class object.
	 */
	private static String getClassName(Class<?> clazz) {
		String className = clazz.getSimpleName();
		if (className == null || className.length() == 0) {
			className += "(" + clazz.getSuperclass().getSimpleName();
			if (clazz.getEnclosingClass() != null)
				className += " in " + clazz.getEnclosingClass().getSimpleName();
			className += ")";
		}
		return className;
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
	 *            The Content object to be turned into a string
	 * @return the content converted to a DB-ready string
	 */
	public static String convertContentToDBReadyString(final SignageContent theContent) {
		if (theContent == null)
			return null;

		String blob = null;

		String xmlDocument = "";
		try {
			if (theContent instanceof ChannelPlayList) {
				ChangeChannelList ccl = new ChangeChannelList(); // This is the XML class
				ccl.setContent((ChannelPlayList) theContent);
				xmlDocument = MyXMLMarshaller.getXML(ccl);
			} else {
				ChannelSpec cs = new ChannelSpec(theContent); // This is the XML class
				xmlDocument = MyXMLMarshaller.getXML(cs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// blob = xmlDocument.replace("'", "").replace("\"", "");
		// I think we need to remove all the tick marks and the quote marks from the body of the XML specification of the
		// channel.
		// This global removal of them breaks things downstream.
		blob = xmlDocument;

		return blob;
	}

	/**
	 * @param bytes
	 *            The array to convert
	 * @param readable
	 *            Put readability spaces, after every 4 bytes, into the returned string
	 * @return A hexidecimal string of the byte array.
	 */
	public static String bytesToString(final byte[] bytes, boolean readable) {
		String retval = "";
		for (int i = 0; i < bytes.length; i++) {
			if ((0x000000ff & bytes[i]) < 16)
				retval += "0";
			retval += Integer.toHexString(0x000000ff & bytes[i]);
			if (readable && i % 4 == 3)
				retval += " ";
		}
		return retval;
	}

	private final static float[] bestFonts = { 48.0f, 36.0f, 32.0F, 24.0F, 20.0F, 18.0F, 17.0F, 16.0F, 15.1F, 14.0F, 13.0F, 12.0F,
			11.5F, 11.0F, 10.5F, 10.0F, 9.5F, 9.0F, 8.5F, 8.0F };

	/**
	 * @param suggestedSize
	 *            The size you want your font to be
	 * @param minimum
	 *            The smallest size font you will accept
	 * @param maximum
	 *            The largest size font you will accept
	 * @return A reasonable font size, with some consistency overall.
	 */
	public static float fixFontSize(final float suggestedSize, final float minimum, final float maximum) {
		if (suggestedSize < minimum)
			return minimum;
		if (suggestedSize > maximum)
			return maximum;
		for (float f : bestFonts) {
			if (suggestedSize > f)
				return f;
		}
		return minimum;
	}

	private static HashMap<Integer, Channel> alreadyRetrieved = new HashMap<Integer, Channel>();

	/**
	 * @param channelNumber
	 *            The channel number to look up
	 * @return The channel that is specified by this channel number
	 */
	public static SignageContent getChannelFromNumber(int channelNumber) {
		if (alreadyRetrieved.containsKey(channelNumber))
			return new ChannelImpl(alreadyRetrieved.get(channelNumber));

		Channel retval = null;
		try {
			retval = new ChannelImpl("Fermilab", ChannelClassification.MISCELLANEOUS, "Fermilab", new URI("https://www.fnal.gov"),
					0, 360000L);
		} catch (URISyntaxException e2) {
			e2.printStackTrace();
		}

		if (channelNumber == 0) {
			// Using the default, Fermilab home page!
			println(Util.class, " Using the Fermilab home page as the default channel!");
			return retval;
		} else if (channelNumber < 0) {
			// The default channel is a list of channels. Build it!

			String query = "select Channel.Number as Number,Dwell,Name,Description,URL,SequenceNumber,Sound FROM Channel,ChannelList where ListNumber="
					+ (-channelNumber) + " and Channel.Number=ChannelList.Number ORDER BY SequenceNumber";

			println(DisplayControllerMessagingAbstract.class, " -- Getting default channel list: [" + query + "]");
			Connection connection;
			try {
				connection = ConnectionToDatabase.getDbConnection();
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
							int specialCode = rs.getInt("Sound");
							int seq = rs.getInt("SequenceNumber");

							ChannelInList c = new ChannelInList(name, ChannelClassification.MISCELLANEOUS, desc, new URI(url),
									chanNum, dwell);
							c.setSequenceNumber(seq);
							c.setCode(specialCode);
							channelList.add(c);

						} while (rs.next());

						retval = new ChannelPlayList("List " + (-channelNumber), channelList, 60000L);
						alreadyRetrieved.put(channelNumber, retval);
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
			String query = "SELECT URL,DwellTime,Description,Name,Sound FROM Channel WHERE Number=" + channelNumber;
			// println(DisplayControllerMessagingAbstract.class, " -- Getting default channel: [" + query + "]");
			Connection connection;
			try {
				connection = ConnectionToDatabase.getDbConnection();
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
							int specialCode = rs.getInt("Sound");

							retval = new ChannelImpl(name, ChannelClassification.MISCELLANEOUS, desc, new URI(url), channelNumber,
									dwell);
							retval.setCode(specialCode);

							alreadyRetrieved.put(channelNumber, retval);

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
			// This is an image. Look up that image number and then make the URL that shows this image.
			int portfolioID = channelNumber - ONE_BILLION;
			String query = "SELECT Filename,Description FROM Portfolio where PortfolioID=" + portfolioID
					+ " AND Approval='Approved'";
			// println(DisplayControllerMessagingAbstract.class, " -- Getting a portfolio image: [" + query + "]");
			Connection connection;
			try {
				connection = ConnectionToDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				return null;
			}

			synchronized (connection) {
				try (Statement stmt = connection.createStatement();) {
					try (ResultSet rs = stmt.executeQuery(query);) {
						if (rs.first()) { // Move to first returned row (there should only be one)
							String desc = rs.getString("Description");
							String filename = rs.getString("Filename");

							String url = SINGLE_IMAGE_DISPLAY + URLEncoder.encode(filename, "UTF-8") + "&caption="
									+ URLEncoder.encode(desc, "UTF-8");

							String[] array = filename.split("/", -1);
							retval = new ChannelImpl(array[array.length - 1], ChannelClassification.IMAGE, desc, new URI(url),
									channelNumber, 0L);
							alreadyRetrieved.put(channelNumber, retval);

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

	/**
	 * @param count
	 *            The number to make into an ordinal
	 * @return the suffix to make this count an ordinal, e.g. "st" for count=1
	 */
	public static String getOrdinalSuffix(final int count) {
		String ordinal = "th";
		if (count != 11 && (count % 10) == 1)
			ordinal = "st";
		if (count != 12 && (count % 10) == 2)
			ordinal = "nd";
		if (count != 13 && (count % 10) == 3)
			ordinal = "rd";
		return ordinal;
	}

	/**
	 * Stream an XML document to an output stream.
	 * 
	 * @param doc
	 *            - the XML document to stream
	 * @param os
	 *            - The output stream
	 * @throws TransformerException
	 */
	public static void streamDocument(Document doc, OutputStream os) throws TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = tf.newTransformer();
		trans.transform(new DOMSource(doc), new StreamResult(os));
	}

	private static long	_entropy		= 0;
	private static long	_lastEntropy	= 0;
	private static long	_myIPNumber		= 0;

	/// Function to get nextPrimeNumber
	static long nextPrime(long n) {
		BigInteger b = new BigInteger(String.valueOf(n));
		return Long.parseLong(b.nextProbablePrime().toString());
	}

	/// return a large, random, prime number
	public static long getEntropy() {
		// There are probably better ways to get a high-entropy value, but this one should be sufficient

		if (_myIPNumber == 0) {
			try {
				Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				while (interfaces.hasMoreElements()) {
					NetworkInterface iface = interfaces.nextElement();
					// filters out 127.0.0.1 and inactive interfaces
					if (iface.isLoopback() || !iface.isUp())
						continue;

					Enumeration<InetAddress> addresses = iface.getInetAddresses();
					while (addresses.hasMoreElements()) {
						InetAddress addr = addresses.nextElement();
						String ip = addr.getHostAddress();
						if (ip.contains(":"))
							continue;
						// System.out.print("--> " + ip);
						String reverse = "";
						for (int i = ip.length() - 1; i >= 0; i--) {
							if (ip.charAt(i) == '.')
								reverse += "1";
							else
								reverse += ip.charAt(i);
						}
						// System.out.println(" .. " + reverse + " ... " + System.nanoTime());
						_myIPNumber = Long.parseLong(reverse);
						break;
					}
				}
			} catch (SocketException e) {
				throw new RuntimeException(e);
			}
			_entropy = nextPrime(System.nanoTime() ^ _myIPNumber);
		}
		return _entropy;
	}

	public static long getNextEntropy() {
		if (_lastEntropy == 0)
			_lastEntropy = getEntropy();
		return _lastEntropy = new Random(_lastEntropy).nextLong();
	}

	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			System.out.println(getNextEntropy());
			// _myIPNumber = 0;
			catchSleep(555);
		}
	}
}
