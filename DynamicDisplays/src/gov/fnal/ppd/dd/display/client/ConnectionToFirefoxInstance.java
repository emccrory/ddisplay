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
	private static final long						WAIT_FOR_CONNECTION_TIME		= 15000;

	// private static final String FullScreenExecute = "var elem = document.body; elem.requestFullScreen();";

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

	private static final HashMap<String, String>	colorNames						= GetColorsFromDatabase.get();

	private static final String						BASE_WEB_PAGE					= "http://" + WEB_SERVER_NAME + "/border.php";
	private static final String						TICKERTAPE_WEB_PAGE				= "http://" + WEB_SERVER_NAME + "/border1.php";
	private static final String						EXTRAFRAME_WEB_PAGE				= "http://" + WEB_SERVER_NAME + "/border2.php";
	private static final String						EXTRAFRAMESNOTICKER_WEB_PAGE	= "http://" + WEB_SERVER_NAME + "/border3.php";
	private static final String						EXTRAFRAMENOTICKER_WEB_PAGE		= "http://" + WEB_SERVER_NAME + "/border4.php";

	/**
	 * What type of "wrapper" shall we use to show our web pages?
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	public enum WrapperType {
		/**
		 * Use the normal border.php web page (with no ticker or extra frame)
		 */
		NORMAL,

		/**
		 * Use the border web page that includes the News Ticker
		 */
		TICKER,

		/**
		 * Use the border web page that includes the News Ticker AND one or more extra iFrames for announcements
		 */
		TICKERANDFRAME,

		/**
		 * There are multiple (4) frames but no ticker.
		 */
		FRAMESNOTICKER,
		
		
		/**
		 * There one extra frame but no ticker.
		 */
		FRAMENOTICKER,

		/**
		 * Show naked web pages without the border (untested)
		 */
		NONE
	};

	/**
	 * Create a connection to the instance of FireFox that is being targeted here
	 * 
	 * @param screenNumber
	 * @param virtualID
	 * @param dbID
	 * @param color
	 * 
	 */
	public ConnectionToFirefoxInstance(final int screenNumber, final int virtualID, final int dbID, final Color color) {
		// Create a connection to the instance of FireFox that is being targeted here

		bounds = ScreenLayoutInterpreter.getBounds(screenNumber);
		println(getClass(), " -- Screen size is " + bounds.width + " x " + bounds.height + ", vDisplay=" + virtualID + ", dbID="
				+ dbID);

		this.virtualID = virtualID;
		this.dbID = dbID;

		colorCode = Integer.toHexString(color.getRGB() & 0x00ffffff);
		colorCode = "000000".substring(colorCode.length()) + colorCode;

		port = PORT + screenNumber;
		openConnection();

		String moveTo = "window.moveTo(" + ((int) bounds.getX()) + "," + ((int) bounds.getY()) + ");";
		send(moveTo);

		// Perform a full reset of the browser every now and then.
		Timer timer = new Timer();
		TimerTask tt = new TimerTask() {
			public void run() {
				// The next time there is a channel change (even for a refresh), it will do a full reset.
				showingCanonicalSite.set(false);
				println(ConnectionToFirefoxInstance.class, ": next refresh of URL will be a FULL reset");
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
	public boolean changeURL(final String urlString, final WrapperType theWrapper, final int frameNumber)
			throws UnsupportedEncodingException {
		if (debug)
			println(getClass(), ": New URL: " + urlString);
		
		String frameName = "iframe";
		if (frameNumber > 0)
			frameName = "frame" + frameNumber;

		String s = "document.getElementById('" + frameName + "').src = '" + urlString + "';\n";
		if (frameNumber > 0)
			s += "document.getElementById('" + frameName + "').style.visibility='visible';\n";

		synchronized (showingCanonicalSite) {
			switch (theWrapper) {

			case NORMAL:
				if (!showingCanonicalSite.get()) {
					println(getClass(), " -- Sending full, new URL to browser, " + BASE_WEB_PAGE);
					showingCanonicalSite.set(true);
					s = "window.location=\"" + BASE_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8") + "&display="
							+ virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;

					if (isNumberDiscrete())
						s += "&shownumber=0";
					s += "\";\n";
				}
				break;

			case TICKER:
				if (!showingCanonicalSite.get()) {
					println(getClass(), " -- Sending full, new URL to browser, " + TICKERTAPE_WEB_PAGE);
					showingCanonicalSite.set(true);
					s = "window.location=\"" + TICKERTAPE_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8") + "&display="
							+ virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;

					if (isNumberDiscrete())
						s += "&shownumber=0";
					s += "\";\n";
				}
				break;

			case TICKERANDFRAME:
				if (!showingCanonicalSite.get()) {
					println(getClass(), " -- Sending full, new URL to browser, " + EXTRAFRAME_WEB_PAGE);
					showingCanonicalSite.set(true);
					s = "window.location=\"" + EXTRAFRAME_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8") + "&display="
							+ virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;

					if (isNumberDiscrete())
						s += "&shownumber=0";
					s += "\";\n";
				}
				break;

			case FRAMENOTICKER:
				if (!showingCanonicalSite.get()) {
					println(getClass(), " -- Sending full, new URL to browser, " + EXTRAFRAMENOTICKER_WEB_PAGE);
					showingCanonicalSite.set(true);
					s = "window.location=\"" + EXTRAFRAMENOTICKER_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8")
							+ "&display=" + virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height="
							+ bounds.height;

					if (isNumberDiscrete())
						s += "&shownumber=0";
					s += "\";\n";
				}
				break;			
				
			case FRAMESNOTICKER:
				if (!showingCanonicalSite.get()) {
					println(getClass(), " -- Sending full, new URL to browser, " + EXTRAFRAMESNOTICKER_WEB_PAGE);
					showingCanonicalSite.set(true);
					s = "window.location=\"" + EXTRAFRAMESNOTICKER_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8")
							+ "&display=" + virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height="
							+ bounds.height;

					if (isNumberDiscrete())
						s += "&shownumber=0";
					s += "\";\n";
				}
				break;

			case NONE:
				s = "window.location=\"" + urlString + "\";\n";
				println(getClass(), "Wrapper not used, new URL is " + urlString);
				showingCanonicalSite.set(false);
				break;
			}

			send(s);
			println(getClass(), " -- Sent: [[" + s + "]]");

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
				e.printStackTrace();
				connected = false;
				return false;
			}
		}
		// return true;
	}

	private boolean isNumberDiscrete() {
		// TODO Put this information into the database
		return (dbID == 13 || dbID == 14 || dbID == 15 || dbID == 16);
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
		println(getClass(), " -- Sent: [[" + s + "]]");

		try {
			waitForServer();
			if (!connected)
				System.err.println(getClass().getName() + ".showIdentity() -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			connected = false;
		}
		
		// TODO -- Add the PNG image of the QR code here.  Generate it with this:
		// try {
		//   BufferedImage image = gov.fnal.ppd.dd.util.CrunchifyQRCode.createQRCodeImage(lastURLString);
		// } catch (WriterException e) {
		//   e.printStackTrace();
		// }
	}

	/**
	 * Remove the self-identify dressings on the Display
	 */
	public void removeIdentify() {
		String s = "document.getElementById('numeral').style.opacity=" + (isNumberDiscrete() ? "0.0" : "0.4") + ";\n";
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
		println(getClass(), " -- Sent: [[" + s + "]]");

		try {
			waitForServer();
			if (!connected)
				System.err.println(getClass().getName() + ".removeIdentity() -- error from Display server!");
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
		println(getClass(), " -- Sent: [[" + s + "]]");

		try {
			waitForServer();
			if (!connected)
				System.err.println(getClass().getName() + ".showFrame() -- error from Display server!");
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
		println(getClass(), " -- Sent: [[" + s + "]]");

		try {
			waitForServer();
			if (!connected)
				System.err.println(getClass().getName() + ".hideFrame() -- error from Display server!");
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
					System.out.println(ConnectionToFirefoxInstance.class.getSimpleName() + ": already connected!");
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
							System.err.println("Unrecognized error!");
							e.printStackTrace();
						}
						long delay = 10;
						catchSleep(delay * 1000l);
					}
			}
		}.start();
	}

	private void send(String s) {
		while (!connected && out == null) {
			println(getClass(), " -- Not connected to FireFox instance at " + LOCALHOST + ":" + port + ".  Will try again in "
					+ WAIT_FOR_CONNECTION_TIME + " milliseconds");
			catchSleep(WAIT_FOR_CONNECTION_TIME);
		}
		synchronized (out) {
			if (debug)
				println(getClass(), " [" + s + "]");
			out.println(s);
		}
	}

	private boolean waitForServer() throws IOException {
		if (in != null) {
			char[] cbuf = new char[DEFAULT_BUFFER_SIZE];
			int numRead = in.read(cbuf, 0, DEFAULT_BUFFER_SIZE);
			lastReplyLine = new String(cbuf).substring(0, numRead - 1);
			if (debug)
				println(getClass(), ".waitForServer(): " + numRead + " chars from server: " + lastReplyLine);

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
		println(getClass(), " -- Returning from waitForServer(), connected=" + connected);
		return connected;
	}

	/**
	 * @param frameNumber
	 *            The frame number to turn off
	 */
	public void turnOffFrame(final int frameNumber) {
		String s = "document.getElementById('frame" + frameNumber + "').style.visibility='hidden';\n";

		send(s);
		println(getClass(), " -- Sent: [[" + s + "]]");

		try {
			waitForServer();
			if (!connected)
				System.err.println(getClass().getName() + ".removeIdentity() -- error from Display server!");
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

}
