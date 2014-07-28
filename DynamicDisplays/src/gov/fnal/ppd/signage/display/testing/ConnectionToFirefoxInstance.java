package gov.fnal.ppd.signage.display.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class ConnectionToFirefoxInstance {
	private static final int	DEFAULT_BUFFER_SIZE			= 300;
	private static final String	LOCALHOST					= "localhost";
	private static final int	PORT						= 32000;
	private static final long	WAIT_FOR_CONNECTION_TIME	= 15000;

	// private static final String FullScreenExecute = "var elem = document.body; elem.requestFullScreen();";

	private boolean				connected;
	private BufferedReader		in;
	private Socket				kkSocket;
	private String				lastReplyLine;
	private PrintWriter			out;

	private boolean				debug						= true;

	/**
	 * Create a connection to the instance of FireFox that is being targeted here
	 * 
	 */
	public ConnectionToFirefoxInstance() {
		// Create a connection to the instance of FireFox that is being targeted here
		openConnection();
	}

	/**
	 * Change the URL that is showing now
	 * 
	 * @param urlString
	 *            The URL that this instance should show now.
	 */
	public void changeURL(final String urlString) {
		System.out.println("New URL: " + urlString);
		send("window.location=\"" + urlString + "\"\n");
		// send(FullScreenExecute);

		try {
			waitForServer();
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
		try {
			System.out.println("Opening connection to FireFox instance to " + LOCALHOST + ":" + PORT + " ... ");
			kkSocket = new Socket(LOCALHOST, PORT);
			System.out.println("\tSocket connection to FF created");
			out = new PrintWriter(kkSocket.getOutputStream(), true);
			System.out.println("\tOutput stream established");
			in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
			System.out.println("\tInput stream established");
			connected = true;
			System.out.println("** Connected to FireFox instance on " + LOCALHOST + " through port number " + PORT + " **");
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + LOCALHOST + " --  ignoring.");
		} catch (IOException e) {
			if ("Connection refused".equals(e.getMessage()) || "Connection timed out".equals(e.getMessage())) 
				System.err.println("Couldn't get I/O for the connection to " + LOCALHOST + "/" + PORT + "\t(" + e.getMessage()
						+ ").");
			else
				e.printStackTrace();
			System.exit(-1);
		}

	}

	private synchronized void send(String s) {
		while (!connected && out == null) {
			System.out.println("Not connected to FireFox instance at " + LOCALHOST + ":" + PORT + ".  Will try again in "
					+ WAIT_FOR_CONNECTION_TIME + " milliseconds");
			try {
				Thread.sleep(WAIT_FOR_CONNECTION_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (debug)
			System.out.println("[" + s + "]");
		this.out.println(s);
	}

	private boolean waitForServer() throws IOException {
		if (in != null) {
			char[] cbuf = new char[DEFAULT_BUFFER_SIZE];
			int numRead = in.read(cbuf, 0, DEFAULT_BUFFER_SIZE);
			System.out.println(getClass().getSimpleName() + " --DEBUG-- Got these lines: " + numRead);
			lastReplyLine = new String(cbuf);

			connected = !lastReplyLine.toUpperCase().contains("\"ERROR\""); // Meh. Probably not right
		}
		return connected;
	}

}
