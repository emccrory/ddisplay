/*
 * ConnectionToFirefoxInstance
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.isThisURLNeedAnimation;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;

import gov.fnal.ppd.dd.util.ExitHandler;

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
public class ConnectionToFirefoxInstance extends ConnectionToBrowserInstance {
	private static final int	MAXIMUM_BUFFER_SIZE			= 1024;
	private static final String	LOCALHOST					= "localhost";
	private static final int	PORT						= 32000;

	// private static final String FullScreenExecute = "var elem = document.body; elem.requestFullScreen();";

	private BufferedReader		in;
	private Socket				kkSocket;
	private String				lastReplyLine;
	private PrintWriter			out;
	private int					port;

	private boolean				tryingToCloseNow			= false;
	protected boolean			firstTimeOpeningConnection	= true;
	@SuppressWarnings("unused")
	private boolean				hasEmergencyCapabilities	= true;

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
		super(screenNumber, virtualID, dbID, color, showNumber);

	}

	@Override
	protected void setPositionAndSize() {
		String moveTo = "window.moveTo(" + ((int) bounds.getX()) + "," + ((int) bounds.getY()) + ");";
		send(moveTo);

		String resizeTo = "window.resizeTo(" + ((int) bounds.getWidth()) + "," + ((int) bounds.getHeight()) + ");";
		send(resizeTo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.fnal.ppd.dd.display.client.ConnectionToBrowserInstance#changeURL(java.lang.String,
	 * gov.fnal.ppd.dd.display.client.WrapperType, int, int)
	 */
	@Override
	public synchronized boolean changeURL(final String urlStrg, final WrapperType theWrapper, final int specialCode)
			throws UnsupportedEncodingException {

		String urlString = urlStrg;
		if (badNUC && isThisURLNeedAnimation(urlStrg)) {
			urlString = urlStrg + "&zoom=0";
		}
		if (debug)
			println(getClass(), instance + " New URL: " + urlString);

		if (showingEmergencyCommunication) {
			removeEmergencyCommunication();
		}

		String frameName = "iframe";

		String s = "document.getElementById('" + frameName + "').src = '" + urlString + "';\n";
		
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

				if (!showNumber)
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

				if (!showNumber)
					s += "&shownumber=0";
				s += "\";\n";
			}
			break;

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

		// }
		return true;
	}

	private void setIsNotConnected() {
		connected = false;
		if (tryingToCloseNow == false) {
			tryingToCloseNow = true;
			new Thread("RestartConnectionToFireFox") {
				public void run() {
					println(ConnectionToFirefoxInstance.class, "\n\n\t\tCLOSING connection to FireFox " + getConnectionCode() + "\n\n");
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
					println(ConnectionToFirefoxInstance.class,
							"\t\tAttempting to re-open the connection to FireFox " + getConnectionCode() + "\n\n");
					tryingToCloseNow = false;
					openConnection();
				}
			}.start();
		}
	}

	@Override
	public void send(String s) {
		
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

		try {
			if (!waitForServer())
				System.err.println(getClass().getName() + ".send() " + instance + " -- error from Display server!");
		} catch (IOException e) {
			e.printStackTrace();
			setIsNotConnected();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.fnal.ppd.dd.display.client.ConnectionToBrowserInstance#exit()
	 */
	@Override
	public void exit() {
		// Remove the FireFox window that was created earlier.
		try {
			if (kkSocket != null)
				kkSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.fnal.ppd.dd.display.client.ConnectionToBrowserInstance#getTheReply()
	 */
	@Override
	public Object getTheReply() {
		return lastReplyLine;
	}

	@Override
	public void openConnection() {
		if (connected) {
			println(getClass(), " -- already connected!");
			return;
		}

		port = PORT + screenNumber;

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
				Class<ConnectionToFirefoxInstance> clazz = ConnectionToFirefoxInstance.class;
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
					} catch (ConnectException e) {
						numFailures++;
						System.err.println("Couldn't get I/O for the connection to " + LOCALHOST + ":" + port + " -- ("
								+ e.getMessage() + ") (" + numFailures + ") [a]");
					} catch (IOException e) {
						numFailures++;
						if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Connection timed out")) {
							System.err.println("Couldn't get I/O for the connection to " + LOCALHOST + ":" + port + " -- ("
									+ e.getMessage() + ") (" + numFailures + ") [b]");
						} else {
							System.err.println(instance + "Unrecognized error! (" + numFailures + ")");
							e.printStackTrace();
						}
					}
					if (numFailures >= iterationsWait) {
						System.err.println(ConnectionToFirefoxInstance.class.getSimpleName()
								+ ".openConnection().Thread.run(): Firefox instance is not responding.  ABORT!");
						ExitHandler.saveAndExit("Firefox instance is not responding in " + getClass().getName());
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
					println(getClass(),
							".waitForServer()" + instance + ": " + numRead + " chars from server: [" + lastReplyLine + "]");

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
					println(getClass(),
							"\n\n******************************************************\n\nRecevied this line: [" + lastReplyLine
									+ "]\n\n******************************************************\n\n"
									+ "ABORTING APPLICATION!\n******************************************************");
					ExitHandler.saveAndExit("Received 'tab not there' message in " + getClass().getName());
				}

				// If we are asking for an element in the web document that does not exist, we'll see this error:
				if (lastReplyLine.toLowerCase().contains("evaluation error: typetrror: document.getelementbyid(...) is null")) {
					return true;
				}
				connected = numRead > 0 && !lastReplyLine.toUpperCase().contains("\"ERROR\"");
				if (!connected)
					setIsNotConnected();
			} catch (SocketTimeoutException e) {
				System.err.println(new Date() + " " + ConnectionToFirefoxInstance.class.getSimpleName()
						+ ".waitForServer(): No response from Firefox plugin");
				e.printStackTrace();
				setIsNotConnected();
			}
		}
		println(getClass(), instance + " Returning from waitForServer(), connected=" + connected);
		return connected;
	}

	/**
	 * @return a string that tells which connection instance this is (e.g., " (port #32000)")
	 */
	@Override
	public String getConnectionCode() {
		return instance;
	}

}
