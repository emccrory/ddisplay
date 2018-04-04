/*
 * ConnectionToFirefoxInstance
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.isThisURLNeedAnimation;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.display.ScreenLayoutInterpreter;
import gov.fnal.ppd.dd.emergency.EmergencyMessage;
import gov.fnal.ppd.dd.emergency.Severity;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connects to an instance of Firefox browser and sends JavaScript instructions to it. This is the "lowest" level class in the
 * suite.
 * 
 * (November, 2015) -- Need to be careful about not letting different threads stomp on the activities of this class. Up until now,
 * there were "synchronize" blocks on the instance(s) of this class.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ConnectionToFirefoxInstance {
	private static final int					MAXIMUM_BUFFER_SIZE				= 1024;
	private static final String					LOCALHOST						= "localhost";
	private static final int					PORT							= 32000;

	// private static final String FullScreenExecute = "var elem = document.body; elem.requestFullScreen();";

	private static int							numberOfScreens					= 0;
	private String								instance						= "";

	private boolean								connected						= false;
	private BufferedReader						in;
	private Socket								kkSocket;
	private String								lastReplyLine;
	private PrintWriter							out;
	private final int							port;

	private boolean								debug							= true;
	private int									virtualID, dbID;

	private String								colorCode;
	private AtomicBoolean						showingCanonicalSite			= new AtomicBoolean(false);
	private Rectangle							bounds;
	private boolean								showNumber						= true;

	private boolean								tryingToCloseNow				= false;
	protected boolean							firstTimeOpeningConnection		= true;
	private boolean								showingEmergencyCommunication	= false;
	private boolean								badNUC							= false;
	private boolean								hasEmergencyCapabilities		= true;
	private boolean								elementIDError					= false;

	private static List<Command>				finalCommand					= new ArrayList<Command>();

	private static final Map<String, String>	colorNames						= GetColorsFromDatabase.get();

	// private static final String BASE_WEB_PAGE = "http://" + WEB_SERVER_NAME + "/border.php";
	// private static final String EXTRAFRAME_WEB_PAGE = "http://" + WEB_SERVER_NAME + "/border2.php";
	// private static final String EXTRAFRAMESNOTICKER_WEB_PAGE = "http://" + WEB_SERVER_NAME + "/border3.php";
	// private static final String EXTRAFRAMENOTICKER_WEB_PAGE = "http://" + WEB_SERVER_NAME + "/border4.php";

	private static final String					TICKERTAPE_WEB_PAGE				= "http://" + WEB_SERVER_NAME + "/border6.php";
	private static final String					WEB_PAGE_EMERGENCY_FRAME		= "http://" + WEB_SERVER_NAME + "/border7.php";

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

		// if (testEmergencyStuff) {
		// // TESTING of Emergency message thing
		// TimerTask g = new TimerTask() {
		// boolean on = false;
		//
		// public void run() {
		// if (on) {
		// EmergencyMessage test = new EmergencyMessage();
		// test.setHeadline("Test Message");
		// test.setMessage("This is a test of the Fermilab Dynamic Displays emergency alert system.");
		// test.setSeverity(Severity.TESTING);
		//
		// showEmergencyCommunication(test);
		// on = false;
		// } else {
		// removeEmergencyCommunication();
		// on = true;
		// }
		// }
		// };
		// timer.schedule(g, 15000, 20000);
		// }
	}

	/**
	 * Change the URL that is showing now.
	 * 
	 * Applying "synchronized" here just in case there are back-to-back change requests. Is this necessary, since there are
	 * synchronizations on the in and the out sockets?
	 * 
	 * @param urlStrg
	 *            The URL that this instance should show now.
	 * @param theWrapper
	 *            What kind of wrapper page shall this be? Normal, ticker-tape or none ("none" is really not going to work)
	 * @param frameNumber
	 *            The frame number to which this content is targeted
	 * @param specialCode
	 *            The code, from the database, for this URL (e.g., has sound)
	 * @return Was the change successful?
	 * @throws UnsupportedEncodingException
	 *             -- if the url we have been given is bogus
	 */
	public synchronized boolean changeURL(final String urlStrg, final WrapperType theWrapper, final int frameNumber,
			final int specialCode) throws UnsupportedEncodingException {

		String urlString = urlStrg;
		if (badNUC && isThisURLNeedAnimation(urlStrg)) {
			urlString = urlStrg + "&zoom=0";
		}
		if (debug)
			if (frameNumber == 0)
				println(getClass(), instance + " New URL: " + urlString);
			else
				println(getClass(), instance + " New URL for frame number " + frameNumber + ": " + urlString);

		if (showingEmergencyCommunication) {
			removeEmergencyCommunication();
		}

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
			if ((specialCode | 1) != 0) {
				// TODO - This URL has sound. Hmmm.
			}
			if ((specialCode | 2) != 0) {
				// TODO - This URL cannot be used in our border frame URL. Hmmm.
			}
			if (!showingCanonicalSite.get()) {

				// FIXME - There is a class of URLs that will not let us put their content into our border frame
				// web site. They are giving an error, "does not permit cross-origin framing".

				// The first site I have encountered for this is the site that shows a Google Sheets presentation. The work-around,
				// I think, will be to abandon this border web page for these sites (only). This will break the fancy, beautiful
				// "identify" functionality, but I can probably figure out a work-around for this, too.

				// The solution is (probably) to flag a web page in the database as having this restriction, and then getting that
				// flag down to here, where this operation can then send just the URL, without the border stuff. There is a field
				// in the Channel table currently called "Sound". This can be changed to "Flags" and then we can put all kinds of
				// stuff like that in there ("Does the page have sound?" "Is this a non-cross-origin web page?" Etc.) There is
				// also a field in the XML transmission called <code>. That flag can go there.

				println(getClass(), instance + " Sending full, new URL to browser, " + WEB_PAGE_EMERGENCY_FRAME);
				showingCanonicalSite.set(true);
				s = "window.location=\"" + WEB_PAGE_EMERGENCY_FRAME + "?url=" + URLEncoder.encode(urlString, "UTF-8") + "&display="
						+ virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;

				if (isNumberHidden())
					s += "&shownumber=0";
				s += "\";\n";
			}
			break;

		case TICKER:
			// case FERMITICKER:
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

		// case TICKERANDFRAME:
		// if (!showingCanonicalSite.get()) {
		// println(getClass(), instance + " Sending full, new URL to browser " + EXTRAFRAME_WEB_PAGE);
		// showingCanonicalSite.set(true);
		// s = "window.location=\"" + EXTRAFRAME_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8") + "&display="
		// + virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;
		//
		// if (isNumberHidden())
		// s += "&shownumber=0";
		// s += "\";\n";
		// }
		// break;
		//
		// case FRAMENOTICKER:
		// String borderPage = WEB_PAGE_EMERGENCY_FRAME; // EXTRAFRAMENOTICKER_WEB_PAGE);
		//
		// if (!showingCanonicalSite.get()) {
		// println(getClass(), instance + " Sending full, new URL to browser " + borderPage);
		// showingCanonicalSite.set(true);
		// s = "window.location=\"" + borderPage + "?url=" + URLEncoder.encode(urlString, "UTF-8") + "&display=" + virtualID
		// + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;
		//
		// if (isNumberHidden())
		// s += "&shownumber=0";
		// s += "\";\n";
		// }
		// break;
		//
		// case FRAMESNOTICKER:
		// if (!showingCanonicalSite.get()) {
		// println(getClass(), instance + " Sending full, new URL to browser " + EXTRAFRAMESNOTICKER_WEB_PAGE);
		// showingCanonicalSite.set(true);
		// s = "window.location=\"" + EXTRAFRAMESNOTICKER_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8")
		// + "&display=" + virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;
		//
		// if (isNumberHidden())
		// s += "&shownumber=0";
		// s += "\";\n";
		// }
		// break;
		//
		// case NONE:
		// s = "window.location=\"" + urlString + "\";\n";
		// println(getClass(), instance + " Wrapper not used.  THIS IS UNTESTED!  New URL is " + urlString + instance);
		// showingCanonicalSite.set(false);
		// break;
		}

		// ********************** Here is where the javascript message is sent to Firefox, containing the new URL ************

		send(s); // **********************************************************************************************************

		// *******************************************************************************************************************

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
			setIsNotConnected();
			return false;
		}
		// }
		// return true;
	}

	private void setIsNotConnected() {
		connected = false;
		if (tryingToCloseNow == false) {
			tryingToCloseNow = true;
			new Thread("RestartConnectionToFireFox") {
				public void run() {
					println(ConnectionToFirefoxInstance.class, "\n\n\t\tCLOSING connection to FireFox " + getInstance() + "\n\n");
					try {
						if (kkSocket != null)
							kkSocket.close();
						if (out != null)
							out.close();
						if (in != null)
							in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					kkSocket = null;
					out = null;
					in = null;

					catchSleep(2000L);
					println(ConnectionToFirefoxInstance.class, "\t\tAttempting to re-open the connection to FireFox "
							+ getInstance() + "\n\n");
					tryingToCloseNow = false;
					openConnection();
				}
			}.start();
		}
	}

	private boolean isNumberHidden() {
		return !showNumber;
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
			if (!waitForServer())
				System.err.println(getClass().getName() + ".forceRefresh()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			setIsNotConnected();
		}

		// if (lastURL == null)
		// return;
		// // Send the last URL back to the display
		//
		// String frameName = "iframe";
		// if (frameNumber > 0)
		// frameName = "frame" + frameNumber;
		//
		// s = "document.getElementById('" + frameName + "').src = '" + lastURL + "';\n";
		// if (frameNumber > 0)
		// s += "document.getElementById('" + frameName + "').style.visibility='visible';\n";
		//
		// send(s);
		//
		// try {
		// waitForServer();
		// if (!connected)
		// System.err.println(getClass().getName() + ".forceRefresh()" + instance + " -- error from Display server!");
		// } catch (IOException e) {
		// e.printStackTrace();
		// setConnected(false;
		// }
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
			if (!waitForServer())
				System.err.println(getClass().getName() + ".showIdentity()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			setIsNotConnected();
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
			if (!waitForServer())
				System.err.println(getClass().getName() + ".removeIdentity()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			setIsNotConnected();
		}
	}

	/**
	 * @param frameNumber
	 *            the frame number to show in the HTML file
	 */
	public void showFrame(final int frameNumber) {
		String s = "document.getElementById('frame" + frameNumber + "').style.visibility='visible';\n";
		send(s);

		try {
			if (!waitForServer())
				System.err.println(getClass().getName() + ".showFrame()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			setIsNotConnected();
		}
	}

	/**
	 * @param frameNumber
	 *            the frame number to hide in the HTML file
	 */
	public void hideFrame(final int frameNumber) {
		String s = "document.getElementById('frame" + frameNumber + "').style.visibility='hidden';\n";
		send(s);

		try {
			if (!waitForServer())
				System.err.println(getClass().getName() + ".hideFrame()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			setIsNotConnected();
		}
	}

	/**
	 * Remove the FireFox window that was created earlier.
	 */
	public void exit() {
		// Remove the FireFox window that was created earlier.
		try {
			if (kkSocket != null)
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
		if (connected) {
			println(getClass(), " -- already connected!");
			return;
		}
		new Thread("OpenConToFirefox") {
			int	numFailures					= 0;
			int	iterationsToWaitFirstTime	= 12;
			int	iterationsToWaitOtherTimes	= 22;

			public void run() {
				if (connected) {
					println(getClass(), " -- already connected!");
					return;
				}
				long delay = 1000L;
				numFailures = 0;
				int iterationsWait = iterationsToWaitOtherTimes; // We give up after about 5 minutes
				if (firstTimeOpeningConnection) {
					iterationsWait = iterationsToWaitFirstTime; // First time through, we give up after about 2 minutes
					firstTimeOpeningConnection = false;
				}
				Class clazz = ConnectionToFirefoxInstance.class;
				while (!connected) {
					try {
						println(clazz, ":\tOpening connection to FireFox instance to " + LOCALHOST + ":" + port + " ... ");
						kkSocket = new Socket(LOCALHOST, port);
						println(clazz, ":\tSocket connection to FF created");
						out = new PrintWriter(kkSocket.getOutputStream(), true);
						println(clazz, ":\tOutput stream established");
						in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
						kkSocket.setSoTimeout(10000);
						println(clazz, ":\tInput stream established");
						println(clazz, ": ** Connected to FireFox instance on " + LOCALHOST + " through port number " + port
								+ " ** -- " + new Date());
						connected = true;
						break;
					} catch (UnknownHostException e) {
						System.err.println("Don't know about host " + LOCALHOST + " --  ignoring. (" + numFailures + ")");
						numFailures++;
					} catch (IOException e) {
						numFailures++;
						if ("Connection refused".equals(e.getMessage()) || "Connection timed out".equals(e.getMessage())) {
							System.err.println("Couldn't get I/O for the connection to " + LOCALHOST + ":" + port + " -- ("
									+ e.getMessage() + ") (" + numFailures + ")");
						} else {
							System.err.println(instance + "Unrecognized error! (" + numFailures + ")");
							e.printStackTrace();
						}
					}
					if (numFailures >= iterationsWait) {
						System.err.println(ConnectionToFirefoxInstance.class.getSimpleName()
								+ ".openConnection().Thread.run(): Firefox instance is not responding.  ABORT!");
						saveAndExit();
						// This limit was put in at the same time as a "while loop" in the canonical (Linux) shell script that
						// re-executes the "run display" class when an error exit status happens.
					}
					System.err.println("Will sleep for " + delay + " msec and then try again");
					catchSleep(delay);
					delay = Math.min(delay + 5000L, 15000L);
				}
			}
		}.start();
	}

	private void send(String s) {
		long sleepTime = 1000L;
		while (!connected || out == null) {
			println(getClass(), ".send() -- Not connected to FireFox instance at " + LOCALHOST + ":" + port
					+ ".  Will look again in " + sleepTime + " milliseconds");
			catchSleep(sleepTime);
			sleepTime = Math.min(sleepTime + 5000L, 15000L);
		}
		if (debug)
			println(getClass(), instance + " sending [" + s + "]");
		synchronized (out) {
			out.println(s);
		}
	}

	private boolean waitForServer() throws IOException {
		if (in != null) {
			try {
				char[] cbuf = new char[MAXIMUM_BUFFER_SIZE];
				int numRead = 0;
				synchronized (in) {
					// TODO It would be nice to time out of this blocking read. (At least I think it is blocking)
					numRead = in.read(cbuf, 0, MAXIMUM_BUFFER_SIZE);
				}
				if (numRead > 2) {
					if (numRead == MAXIMUM_BUFFER_SIZE) {
						new Exception("have read the maximum number of bytes, " + numRead
								+ ", from the Firefox input stream.  This cannot be good").printStackTrace();
					}
					lastReplyLine = new String(cbuf).substring(0, numRead - 1);
				} else
					lastReplyLine += "(Unexpectedly short reply of " + numRead + " chars from browser) -- [" + new String(cbuf)
							+ "]";

				if (debug)
					println(getClass(), ".waitForServer()" + instance + ": " + numRead + " chars from server: [" + lastReplyLine
							+ "]");

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

				/*
				 * A special error is returned if the Firefox Tab has disappeared: "tab navigated or closed"
				 */
				if (lastReplyLine.toLowerCase().contains("tab navigated or closed")) {
					println(getClass(), "\n\n******************************************************\n\nRecevied this line: ["
							+ lastReplyLine + "]\n\n******************************************************\n\n"
							+ "ABORTING APPLICATION!\n******************************************************");
					saveAndExit();
				}

				// If we are asking for an element in the web document that does not exist, we'll see this error:
				if (lastReplyLine.toLowerCase().contains("evaluation error: typetrror: document.getelementbyid(...) is null")) {
					elementIDError = true;
					return true;
				} else {
					connected = numRead > 0 && !lastReplyLine.toUpperCase().contains("\"ERROR\"");
					if (!connected)
						setIsNotConnected();
				}
			} catch (SocketTimeoutException e) {
				System.err.println(new Date() + " " + ConnectionToFirefoxInstance.class.getSimpleName()
						+ ".waitForServer(): No response from Firefox plugin");
				setIsNotConnected();
			}
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
			if (!waitForServer())
				System.err.println(getClass().getName() + ".removeIdentity()" + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			setIsNotConnected();
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

	public static void saveAndExit() {
		// Save the current channel(s) and close the Java VM
		for (Command C : finalCommand)
			if (C != null)
				C.execute();

		System.out.println("\n\n********** EXITING **********\n");
		System.exit(-1);
	}

	/**
	 * Remove this command from the list -- not needed anymore.
	 * 
	 * @param c
	 *            The command to remove
	 */
	public void removeFinalCommand(final Command c) {
		finalCommand.remove(c);
	}

	/**
	 * @param c
	 *            the finalCommand to set
	 * @return The command is returned back to the sender
	 */
	public Command addFinalCommand(final Command c) {
		finalCommand.add(c);
		return c;
	}

	/**
	 * Force an emergency message onto the screen!
	 * 
	 * @param message
	 *            -- The message, which is (presumably) formatted HTML
	 * 
	 * @return Did it work?
	 */
	public boolean showEmergencyCommunication(final EmergencyMessage message) {
		if (message.getSeverity() == Severity.REMOVE)
			return removeEmergencyCommunication();

		String mess = message.toHTML() + "<p class='date'>Message was last updated at this time: " + new Date() + "</p>";

		Color borderColor = null;
		switch (message.getSeverity()) {
		case ALERT:
			borderColor = Color.RED.darker();
			break;

		case INFORMATION:
			borderColor = Color.GREEN.darker();
			break;

		case TESTING:
			borderColor = Color.GRAY;
			break;

		case EMERGENCY:
			borderColor = Color.RED;
			break;

		case REMOVE:
			// Already handled above
			break;
		}

		String s = "";
		if (borderColor != null) {
			String rgb = getHex(borderColor.getRed()) + getHex(borderColor.getGreen()) + getHex(borderColor.getBlue());
			s = "document.getElementById('emergencyframe1').style.borderColor = '#" + rgb + "';\n";
		}

		s += "document.getElementById('emergencyframe1').innerHTML='" + mess.replace("'", "\\'") + "';\n";
		s += "document.getElementById('emergencyframe1').style.visibility='visible';\n";

		send(s);
		showingEmergencyCommunication = true;

		try {
			if (waitForServer()) {
				if (elementIDError)
					hasEmergencyCapabilities = false;
				return true;
			}
			return false;
		} catch (IOException e) {
			if (numberOfScreens > 1)
				System.err.println("Exception for EMERGENCY MESSAGE on port #" + port);
			e.printStackTrace();
			setIsNotConnected();
			return false;
		}
	}

	private String getHex(int val) {
		String retval = Integer.toHexString(0x000000ff & val);
		if (val < 16)
			return "0" + retval;
		return retval;
	}

	/**
	 * @return Did it work?
	 */
	private boolean removeEmergencyCommunication() {
		String s = "document.getElementById('emergencyframe1').innerHTML='';\n";
		s += "document.getElementById('emergencyframe1').style.visibility='hidden';\n";

		send(s);
		showingEmergencyCommunication = false;

		try {
			if (waitForServer()) {
				if (elementIDError)
					hasEmergencyCapabilities = false;
				return true;
			}
			return false;
		} catch (IOException e) {
			if (numberOfScreens > 1)
				System.err.println("Exception EMERGENCY MESSAGE on port #" + port);
			e.printStackTrace();
			setIsNotConnected();
			return false;
		}
	}

	public void setBadNUC(boolean badNUC) {
		this.badNUC = badNUC;
	}
}
