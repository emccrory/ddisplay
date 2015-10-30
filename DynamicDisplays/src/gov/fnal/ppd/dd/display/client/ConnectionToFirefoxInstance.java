/*
 * ConnectionToFirefoxInstance
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_NAME;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.display.ScreenLayoutInterpreter;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connects to an instance of Firefox browser and sends JavaScript instructions to it. This is the "lowest" level class in the
 * suite.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ConnectionToFirefoxInstance {
	private static final int						DEFAULT_BUFFER_SIZE				= 300;
	private static final String						LOCALHOST						= "localhost";
	private static final int						PORT							= 32000;

	// private static final String FullScreenExecute = "var elem = document.body; elem.requestFullScreen();";

	private static int								numberOfScreens					= 0;
	private String									instance						= "";

	private boolean									connected						= false;
	private BufferedReader							in;
	private Socket									kkSocket;
	private String									lastReplyLine;
	private PrintWriter								out;
	private final int								port;

	private boolean									debug							= true;
	private int										virtualID, dbID;

	private String									colorCode;
	private AtomicBoolean							showingCanonicalSite			= new AtomicBoolean(false);
	private Rectangle								bounds;
	private boolean									showNumber						= true;
	private String									lastURL							= null;

	private static final HashMap<String, String>	colorNames						= GetColorsFromDatabase.get();

	private static final String						BASE_WEB_PAGE					= "http://" + WEB_SERVER_NAME + "/border.php";
	private static final String						TICKERTAPE_WEB_PAGE				= "http://" + WEB_SERVER_NAME + "/border6.php";
	private static final String						EXTRAFRAME_WEB_PAGE				= "http://" + WEB_SERVER_NAME + "/border2.php";
	private static final String						EXTRAFRAMESNOTICKER_WEB_PAGE	= "http://" + WEB_SERVER_NAME + "/border3.php";
	private static final String						EXTRAFRAMENOTICKER_WEB_PAGE		= "http://" + WEB_SERVER_NAME + "/border4.php";

	// private static final String FERMI_TICKERTAPE_WEB_PAGE = "http://" + WEB_SERVER_NAME + "/border5.php";

	/**
	 * Create a connection to the instance of FireFox that is being targeted here
	 * 
	 * @param screenNumber
	 * @param virtualID
	 * @param dbID
	 * @param color
	 * @param showNumber
	 *            Should the display number be shown on the screen?
	 * 
	 */
	public ConnectionToFirefoxInstance(final int screenNumber, final int virtualID, final int dbID, final Color color,
			final boolean showNumber) {
		// Create a connection to the instance of FireFox that is being targeted here

		bounds = ScreenLayoutInterpreter.getBounds(screenNumber);
		println(getClass(), " -- Screen size is " + bounds.width + " x " + bounds.height + ", vDisplay=" + virtualID + ", dbID="
				+ dbID);

		this.virtualID = virtualID;
		this.dbID = dbID;
		this.showNumber = showNumber;
		numberOfScreens++;

		port = PORT + screenNumber;
		Timer timer = new Timer();

		if (numberOfScreens > 1)
			instance = " (port #" + port + ")";
		else {
			TimerTask g = new TimerTask() {
				public void run() {
					for (int i = 0; i < 100; i++) {
						if (numberOfScreens > 1) {
							instance = " (port #" + port + ")";
							return;
						}
						// System.out.println("              Number of screens = " + numberOfScreens);
						catchSleep(100);
					}
				}
			};
			timer.schedule(g, 500); // Run it once, after a short delay.
		}

		colorCode = Integer.toHexString(color.getRGB() & 0x00ffffff);
		colorCode = "000000".substring(colorCode.length()) + colorCode;

		openConnection();

		String moveTo = "window.moveTo(" + ((int) bounds.getX()) + "," + ((int) bounds.getY()) + ");";
		send(moveTo);

		String resizeTo = "window.resizeTo(" + ((int) bounds.getWidth()) + "," + ((int) bounds.getHeight()) + ");";
		send(resizeTo);

		// Perform a full reset of the browser every now and then.
		TimerTask tt = new TimerTask() {
			public void run() {
				try {
					// The next time there is a channel change (even for a refresh), it will do a full reset.
					showingCanonicalSite.set(false);
					println(ConnectionToFirefoxInstance.class, ": next refresh of URL will be a FULL reset");
				} catch (Exception e) {
					println(ConnectionToFirefoxInstance.class, "Exception caught in diagnostic thread, " + e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		};
		timer.scheduleAtFixedRate(tt, ONE_HOUR, ONE_HOUR);
	}

	/**
	 * Change the URL that is showing now
	 * 
	 * @param urlString
	 *            The URL that this instance should show now.
	 * @param theWrapper
	 *            What kind of wrapper page shall this be? Normal, ticker-tape or none ("none" is really not going to work)
	 * @param frameNumber
	 *            The frame number to which this content is targeted
	 * @return Was the change successful?
	 * @throws UnsupportedEncodingException
	 *             -- if the url we have been given is bogus
	 */
	public synchronized boolean changeURL(final String urlString, final WrapperType theWrapper, final int frameNumber)
			throws UnsupportedEncodingException {
		if (debug)
			println(getClass(), instance + " New URL: " + urlString);

		lastURL = urlString;

		String frameName = "iframe";
		if (frameNumber > 0)
			frameName = "frame" + frameNumber;

		String s = "document.getElementById('" + frameName + "').src = '" + urlString + "';\n";
		if (frameNumber > 0)
			s += "document.getElementById('" + frameName + "').style.visibility='visible';\n";

		// "FastBugs" frowns on using an internally-synchronized object for synchronization. The intent is to prevent
		// multiple instances of this method from running at the same time, so we put the "synchronized" on the method, where it
		// should be.
		// synchronized (showingCanonicalSite) {
		switch (theWrapper) {

		case NORMAL:
			if (!showingCanonicalSite.get()) {
				println(getClass(), instance + " Sending full, new URL to browser, " + BASE_WEB_PAGE);
				showingCanonicalSite.set(true);
				s = "window.location=\"" + BASE_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8") + "&display="
						+ virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;

				if (isNumberHidden())
					s += "&shownumber=0";
				s += "\";\n";
			}
			break;

		case TICKER:
		case FERMITICKER:
			if (!showingCanonicalSite.get()) {
				println(getClass(), instance + " Sending full, new URL to browser " + TICKERTAPE_WEB_PAGE);
				showingCanonicalSite.set(true);
				s = "window.location=\"" + TICKERTAPE_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8") + "&display="
						+ virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height + "&feed="
						+ theWrapper.getTickerName();

				if (isNumberHidden())
					s += "&shownumber=0";
				s += "\";\n";
			}
			break;

		// if (!showingCanonicalSite.get()) {
		// println(getClass(), instance + " Sending full, new URL to browser" + instance + ", " + FERMI_TICKERTAPE_WEB_PAGE);
		// showingCanonicalSite.set(true);
		// s = "window.location=\"" + FERMI_TICKERTAPE_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8")
		// + "&display=" + virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height="
		// + bounds.height;
		//
		// if (isNumberHidden())
		// s += "&shownumber=0";
		// s += "\";\n";
		// }
		// break;

		case TICKERANDFRAME:
			if (!showingCanonicalSite.get()) {
				println(getClass(), instance + " Sending full, new URL to browser " + EXTRAFRAME_WEB_PAGE);
				showingCanonicalSite.set(true);
				s = "window.location=\"" + EXTRAFRAME_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8") + "&display="
						+ virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;

				if (isNumberHidden())
					s += "&shownumber=0";
				s += "\";\n";
			}
			break;

		case FRAMENOTICKER:
			if (!showingCanonicalSite.get()) {
				println(getClass(), instance + " Sending full, new URL to browser " + EXTRAFRAMENOTICKER_WEB_PAGE);
				showingCanonicalSite.set(true);
				s = "window.location=\"" + EXTRAFRAMENOTICKER_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8")
						+ "&display=" + virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;

				if (isNumberHidden())
					s += "&shownumber=0";
				s += "\";\n";
			}
			break;

		case FRAMESNOTICKER:
			if (!showingCanonicalSite.get()) {
				println(getClass(), instance + " Sending full, new URL to browser " + EXTRAFRAMESNOTICKER_WEB_PAGE);
				showingCanonicalSite.set(true);
				s = "window.location=\"" + EXTRAFRAMESNOTICKER_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8")
						+ "&display=" + virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;

				if (isNumberHidden())
					s += "&shownumber=0";
				s += "\";\n";
			}
			break;

		case NONE:
			s = "window.location=\"" + urlString + "\";\n";
			println(getClass(), instance + " Wrapper not used, new URL is " + urlString + instance);
			showingCanonicalSite.set(false);
			break;
		}

		send(s);

		// I have tried to do this in a local file, in order to remove some load from the web server. But
		// this has not worked for me. (10/2014) First, it seems that the "window.location" Javascript command
		// does not work for local files. Second, if I load the local file by hand, the Javascript timeout
		// function never gets called.

		// try {
		// FileOutputStream out = new FileOutputStream(LOCAL_FILE_NAME);
		// out.write(htmlTemplate.replace(URL_MARKER, urlString).getBytes());
		// out.close();
		//
		// String sendString = "window.location=\"file://localhost" + LOCAL_FILE_NAME + "\";\n";
		// System.out.println("changeURL(): " + sendString);
		// send(sendString);
		// //
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		// An experiment: Can I turn off the scroll bars? The answer is no (it seems)
		// send("document.documentElement.style.overflow = 'hidden';\n");
		// send("document.body.scroll='no';\n");
		// send(FullScreenExecute);

		try {
			return waitForServer();
		} catch (IOException e) {
			if (numberOfScreens > 1)
				System.err.println("Exception on port #" + port);
			e.printStackTrace();
			connected = false;
			return false;
		}
		// }
		// return true;
	}

	private boolean isNumberHidden() {
		return !showNumber;
		// return (dbID == 13 || dbID == 14 || dbID == 15 || dbID == 16);
	}

	/**
	 * Ask the browser to completely refresh itself
	 * 
	 * @param frameNumber
	 */
	public void forceRefresh(int frameNumber) {
		// Send the refresh

		String s = "window.location.reload();";

		send(s);

		try {
			waitForServer();
			if (!connected)
				System.err.println(getClass().getName() + ".forceRefresh()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			connected = false;
		}

//		if (lastURL == null)
//			return;
//		// Send the last URL back to the display
//
//		String frameName = "iframe";
//		if (frameNumber > 0)
//			frameName = "frame" + frameNumber;
//
//		s = "document.getElementById('" + frameName + "').src = '" + lastURL + "';\n";
//		if (frameNumber > 0)
//			s += "document.getElementById('" + frameName + "').style.visibility='visible';\n";
//
//		send(s);
//
//		try {
//			waitForServer();
//			if (!connected)
//				System.err.println(getClass().getName() + ".forceRefresh()" + instance + " -- error from Display server!");
//		} catch (IOException e) {
//			e.printStackTrace();
//			connected = false;
//		}
	}

	/**
	 * Modify the actual display to self-identify, showing the Display number and the highlight color.
	 */
	public void showIdentity() {
		String s = "document.getElementById('numeral').style.opacity=0.8;\n";
		s += "document.getElementById('numeral').style.font='bold 750px sans-serif';\n";
		s += "document.getElementById('numeral').style.textShadow='0 0 18px black, 0 0 18px black, 0 0 18px black, 0 0 18px black, 0 0 18px black, 16px 16px 2px #"
				+ colorCode + "';\n";
		s += "document.getElementsByTagName('body')[0].setAttribute('style', 'background-color: #" + colorCode
				+ "; padding:0; margin: 100;' );\n";
		// s += "document.getElementById('iframe').style.width=1700;\n";
		// s += "document.getElementById('iframe').style.height=872;\n";
		s += "document.getElementById('iframe').style.width=" + (bounds.width - 220) + ";\n";
		s += "document.getElementById('iframe').style.height=" + (bounds.height - 208) + ";\n";

		String displayID = "(" + dbID + ") ";
		if (this.dbID == this.virtualID)
			displayID = "";
		if (colorNames.containsKey(colorCode))
			s += "document.getElementById('colorName').innerHTML = '" + displayID + colorNames.get(colorCode) + "';\n";
		else
			s += "document.getElementById('colorName').innerHTML = '" + displayID + "#" + colorCode + "';\n";

		send(s);

		try {
			waitForServer();
			if (!connected)
				System.err.println(getClass().getName() + ".showIdentity()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			connected = false;
		}

		// TODO -- Add the PNG image of the QR code here. Generate it with this:
		// try {
		// BufferedImage image = gov.fnal.ppd.dd.util.CrunchifyQRCode.createQRCodeImage(lastURLString);
		// } catch (WriterException e) {
		// e.printStackTrace();
		// }
	}

	/**
	 * Remove the self-identify dressings on the Display
	 */
	public void removeIdentify() {
		String s = "document.getElementById('numeral').style.opacity=" + (isNumberHidden() ? "0.0" : "0.4") + ";\n";
		s += "document.getElementById('numeral').style.font='bold 120px sans-serif';\n";
		s += "document.getElementsByTagName('body')[0].setAttribute('style', 'padding:0; margin: 0;');\n";
		s += "document.getElementById('numeral').style.textShadow='0 0 4px black, 0 0 4px black, 0 0 4px black, 0 0 4px black, 0 0 4px black, 6px 6px 2px #"
				+ colorCode + "';\n";
		// s += "document.getElementById('iframe').style.width=1916;\n";
		// s += "document.getElementById('iframe').style.height=1074;\n";
		s += "document.getElementById('iframe').style.width=" + (bounds.width - 4) + ";\n";
		s += "document.getElementById('iframe').style.height=" + (bounds.height - 6) + ";\n";

		s += "document.getElementById('colorName').innerHTML = '';\n";

		send(s);

		try {
			waitForServer();
			if (!connected)
				System.err.println(getClass().getName() + ".removeIdentity()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			connected = false;
		}
	}

	/**
	 * @param frameNumber
	 *            the frame number to show in the HTML file
	 */
	public void showFrame(final int frameNumber) {
		String s = "document.getElementById('frame" + frameNumber + "').style.visibility=visible;\n";
		send(s);

		try {
			waitForServer();
			if (!connected)
				System.err.println(getClass().getName() + ".showFrame()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			connected = false;
		}
	}

	/**
	 * @param frameNumber
	 *            the frame number to hide in the HTML file
	 */
	public void hideFrame(final int frameNumber) {
		String s = "document.getElementById('frame" + frameNumber + "').style.visibility=hidden;\n";
		send(s);

		try {
			waitForServer();
			if (!connected)
				System.err.println(getClass().getName() + ".hideFrame()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			connected = false;
		}
	}

	/**
	 * Remove the FireFox window that was created earlier.
	 */
	public void exit() {
		// Remove the FireFox window that was created earlier.
		try {
			kkSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return The last reply from the socket connection
	 */
	public Object getTheReply() {
		return lastReplyLine;
	}

	/**
	 * @return Best guess as to whether or not we are connected to the FireFox back end
	 */
	public boolean isConnected() {
		return connected;
	}

	private void openConnection() {
		new Thread("OpenConToFirefox") {
			public void run() {
				if (connected)
					println(getClass(), " -- already connected!");
				long delay = 1000L;
				while (!connected)
					try {
						println(getClass(), ":\n\tOpening connection to FireFox instance to " + LOCALHOST + ":" + port + " ... ");
						kkSocket = new Socket(LOCALHOST, port);
						println(getClass(), "\tSocket connection to FF created");
						out = new PrintWriter(kkSocket.getOutputStream(), true);
						println(getClass(), "\tOutput stream established");
						in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
						println(getClass(), "\tInput stream established");
						connected = true;
						println(getClass(), " ** Connected to FireFox instance on " + LOCALHOST + " through port number " + port
								+ " ** -- " + new Date());
					} catch (UnknownHostException e) {
						System.err.println("Don't know about host " + LOCALHOST + " --  ignoring.");
					} catch (IOException e) {
						if ("Connection refused".equals(e.getMessage()) || "Connection timed out".equals(e.getMessage()))
							System.err.println("Couldn't get I/O for the connection to " + LOCALHOST + ":" + port + " -- ("
									+ e.getMessage() + ").");
						else {
							System.err.println(instance + "Unrecognized error!");
							e.printStackTrace();
						}
						catchSleep(delay);
						delay += 1000L;
					}
			}
		}.start();
	}

	private void send(String s) {
		long sleepTime = 1000L;
		while (!connected && out == null) {
			println(getClass(), " -- Not connected to FireFox instance at " + LOCALHOST + ":" + port + ".  Will try again in "
					+ sleepTime + " milliseconds");
			catchSleep(sleepTime);
			sleepTime += 1000L;
		}
		synchronized (out) {
			if (debug)
				println(getClass(), instance + " sending [" + s + "]");
			out.println(s);
		}
	}

	private boolean waitForServer() throws IOException {
		if (in != null) {
			char[] cbuf = new char[DEFAULT_BUFFER_SIZE];
			int numRead = in.read(cbuf, 0, DEFAULT_BUFFER_SIZE);
			lastReplyLine = new String(cbuf).substring(0, numRead - 1);
			if (debug)
				println(getClass(), ".waitForServer()" + instance + ": " + numRead + " chars from server: [" + lastReplyLine + "]");

			/*
			 * It looks like the reply expected here is something like this:
			 * 
			 * {"result":"<whatever we just sent>"}
			 * 
			 * If you send more than one line, it sometimes sends more than one {"result":"<value>"} pair, ending in a new line.
			 */

			/*
			 * Generally, when the return value contains "error", there was a problem.
			 */
			connected = numRead > 0 && !lastReplyLine.toUpperCase().contains("\"ERROR\"");
		}
		println(getClass(), instance + " Returning from waitForServer(), connected=" + connected);
		return connected;
	}

	/**
	 * @param frameNumber
	 *            The frame number to turn off
	 */
	public void turnOffFrame(final int frameNumber) {
		String s = "document.getElementById('frame" + frameNumber + "').style.visibility='hidden';\n";

		send(s);

		try {
			waitForServer();
			if (!connected)
				System.err.println(getClass().getName() + ".removeIdentity()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			connected = false;
		}
	}

	/**
	 * Force to refresh the controlling URL.
	 */
	public void resetURL() {
		showingCanonicalSite.set(false);
	}

	/**
	 * @return a string that tells which connection instance this is (e.g., " (port #32000)")
	 */
	String getInstance() {
		return instance;
	}
}
