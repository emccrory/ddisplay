package gov.fnal.ppd.signage.display.testing;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class ConnectionToFirefoxInstance {
	private static final int						DEFAULT_BUFFER_SIZE			= 300;
	private static final String						LOCALHOST					= "localhost";
	private static final int						PORT						= 32000;
	private static final long						WAIT_FOR_CONNECTION_TIME	= 15000;

	// private static final String FullScreenExecute = "var elem = document.body; elem.requestFullScreen();";

	private boolean									connected;
	private BufferedReader							in;
	private Socket									kkSocket;
	private String									lastReplyLine;
	private PrintWriter								out;
	private final int								port;

	private boolean									debug						= false;
	private int										displayID;
	private Color									color;

	private String									colorCode;
	private boolean									firstTime					= true;

	@SuppressWarnings("serial")
	private static final HashMap<String, String>	colorNames					= new HashMap<String, String>() {
																					{
																						put("003397", "Blue");
																						put("ffcc00", "Yellow");
																						put("f20019", "Red");
																						put("008000", "Green");
																						put("fe8420", "Orange");
																						put("ffffff", "White");
																						put("777777", "Gray");
																						put("800080", "Purple");
																						put("964b00", "Brown");
																						put("ffcbdb", "Pink");
																						put("000000", "Black");
																						put("ff0055", "Magenta");
																						put("a0d681", "Light Green");
																					}
																				};

	/**
	 * Create a connection to the instance of FireFox that is being targeted here
	 * 
	 * @param screenNumber
	 * @param displayID
	 * @param color
	 * 
	 */
	public ConnectionToFirefoxInstance(final int screenNumber, final int displayID, final Color color) {
		// Create a connection to the instance of FireFox that is being targeted here

		this.displayID = displayID;
		this.color = color;

		colorCode = Integer.toHexString(color.getRGB() & 0x00ffffff);
		colorCode = "000000".substring(colorCode.length()) + colorCode;

		// htmlTemplate = htmlTemplatePrecursor.replace(DISPLAY_MARKER, "" + displayID).replace(COLOR_MARKER, colorCode);

		// FIXME The does not work for screen != 0. Firefox needs to be configured to listen to port 32001 for screen #1, and this,
		// I think, needs to be done by hand. Not sure at this time (9/15/2014) how to do this.
		port = PORT; // + screenNumber;
		openConnection();
	}

	/**
	 * Change the URL that is showing now
	 * 
	 * @param urlString
	 *            The URL that this instance should show now.
	 * @param useTheWrapper
	 */
	public void changeURL(final String urlString, final boolean useTheWrapper) {
		if (debug)
			System.out.println("New URL: " + urlString);

		if (useTheWrapper) {

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

			String s = "";
			if (firstTime) {
				firstTime = false;
				s = "window.location=\"http://mccrory.fnal.gov/border.php?url=" + URLEncoder.encode(urlString) + "&display="
						+ displayID + "&color=" + colorCode + "\";\n";
				send(s);
			} else {
				s = "document.getElementById('iframe').src = '" + urlString + "';\n";
				// s += "document.getElementById('iframe').contentWindow.location.reload();\n";
				send(s);
			}
			System.out.println("--Sent: [[" + s + "]]");

			// send("window.location=\"http://mccrory.fnal.gov/border.php?url=" + URLEncoder.encode(urlString) + "&display="
			// + displayID + "&color=" + colorCode + "\";\n");
		} else {
			// Without the border wrapper
			send("window.location=\"" + urlString + "\";\n");
		}
		// An experiment: Can I turn off the scroll bars? The answer is no (it seems)
		// send("document.documentElement.style.overflow = 'hidden';\n");
		// send("document.body.scroll='no';\n");
		// send(FullScreenExecute);

		try {
			waitForServer();
		} catch (IOException e) {
			e.printStackTrace();
			connected = false;
		}
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
		s += "document.getElementById('iframe').style.width=1700;\n";
		s += "document.getElementById('iframe').style.height=872;\n";
		if (colorNames.containsKey(colorCode))
			s += "document.getElementById('colorName').innerHTML = '" + colorNames.get(colorCode) + "';\n";
		else
			s += "document.getElementById('colorName').innerHTML = '#" + colorCode + "';\n";

		send(s);
		System.out.println("--Sent: [[" + s + "]]");
	}

	/**
	 * Remove the self-identify dressings on the Display
	 */
	public void removeIdentify() {
		String s = "document.getElementById('numeral').style.opacity=0.4;\n";
		s += "document.getElementById('numeral').style.font='bold 120px sans-serif';\n";
		s += "document.getElementsByTagName('body')[0].setAttribute('style', 'padding:0; margin: 0;');\n";
		s += "document.getElementById('numeral').style.textShadow='0 0 4px black, 0 0 4px black, 0 0 4px black, 0 0 4px black, 0 0 4px black, 6px 6px 2px #"
				+ colorCode + "';\n";
		s += "document.getElementById('iframe').style.width=1916;\n";
		s += "document.getElementById('iframe').style.height=1074;\n";
		s += "document.getElementById('colorName').innerHTML = '';\n";
		
		send(s);
		System.out.println("--Sent: [[" + s + "]]");
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
				while (!connected)
					try {
						System.out.println("Opening connection to FireFox instance to " + LOCALHOST + ":" + port + " ... ");
						kkSocket = new Socket(LOCALHOST, port);
						System.out.println("\tSocket connection to FF created");
						out = new PrintWriter(kkSocket.getOutputStream(), true);
						System.out.println("\tOutput stream established");
						in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
						System.out.println("\tInput stream established");
						connected = true;
						System.out.println("** Connected to FireFox instance on " + LOCALHOST + " through port number " + port
								+ " **");
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
						System.err.println("It may be possible to turn on the FireFox connectivity.  You have " + delay
								+ " seconds to try.");
						try {
							sleep(delay * 1000l);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
			}
		}.start();
	}

	private void send(String s) {
		while (!connected && out == null) {
			System.out.println("Not connected to FireFox instance at " + LOCALHOST + ":" + port + ".  Will try again in "
					+ WAIT_FOR_CONNECTION_TIME + " milliseconds");
			try {
				Thread.sleep(WAIT_FOR_CONNECTION_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		synchronized (out) {
			if (debug)
				System.out.println("[" + s + "]");
			out.println(s);
		}
	}

	private boolean waitForServer() throws IOException {
		if (in != null) {
			char[] cbuf = new char[DEFAULT_BUFFER_SIZE];
			int numRead = in.read(cbuf, 0, DEFAULT_BUFFER_SIZE);
			lastReplyLine = new String(cbuf);
			if (debug)
				System.out.println(getClass().getSimpleName() + " --DEBUG-- Got " + numRead + " chars from server: "
						+ lastReplyLine);

			connected = !lastReplyLine.toUpperCase().contains("\"ERROR\""); // Probably not the right way to know if we are no
																			// longer connected
		}
		return connected;
	}

}
