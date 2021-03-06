package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.dd.GlobalVariables.WAIT_FOR_SERVER_TIME;
import static gov.fnal.ppd.dd.chat.MessagingServer.SPECIAL_SERVER_MESSAGE_USERNAME;
import static gov.fnal.ppd.dd.util.nonguiUtils.ExitHandler.processingGracefulExit;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilderFactory;

import gov.fnal.ppd.dd.xml.messages.AreYouAliveMessage;
import gov.fnal.ppd.dd.xml.messages.YesIAmAliveMessage;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MessagingDataXML;
import gov.fnal.ppd.dd.chat.MessageConveyor;
import gov.fnal.ppd.dd.util.nonguiUtils.PropertiesFile;

/**
 * <p>
 * An implementation of a Messaging Client in the Dynamic Displays system.  This class can be run both from the command line or as a GUI
 * </p>
 * 
 * <p>
 * Taken from the Internet on 5/12/2014. <a
 * href="http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/" dreamincode.net</a>
 * </p>
 * <p>
 * Extensively modified by the author.
 * </p>
 * <p>
 * TODO -- Is there a way for two clients on the same node to talk directly to each other without a server? There is a use case that
 * has emerged whereby the user interacts with a display directly on that display through the Channel Selector. When a choice is
 * made, it sends this to the messaging server, which sends it right back to the same node but communicating with the display. This
 * would only work if there was some way to short-circuit this communication here in the messaging client. At this time (January,
 * 2016), the solution is not at all obvious (if it is possible at all).
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2014-21
 */
public class MessagingClient {

	private static boolean			showAllIncomingMessages			= PropertiesFile.getBooleanProperty("IncomingMessVerbose",
			false);
	// for I/O
	// to read from the socket
	private InputStream				sInput;
	// to write on the socket
	protected OutputStream			sOutput;
	private Socket					socket							= null;
	private DocumentBuilderFactory	dbf								= DocumentBuilderFactory.newInstance();

	// the name of our messaging server and our username
	private String					server, username;

	private int						portNumber;
	private ListenFromServer		listenFromServer;
	private Thread					restartThreadToServer;

	protected long					lastMessageReceived				= 0l;
	private boolean					keepMessagingClientGoing		= true;

	private final String			serverName						= SPECIAL_SERVER_MESSAGE_USERNAME;

	private boolean					readOnly						= true;
	private Thread					watchServer						= null;
	private long					dontTryToConnectAgainUntilNow	= System.currentTimeMillis();

	private String					presentStatus					= "Initializing";

	/**
	 * Constructor called by console mode server: the server address name: the port number: the username for this instance
	 * 
	 * @param server
	 *            The name of the server, e.g., "localhost" or "vip-mariadb-foobar.fnal.gov"
	 * @param port
	 *            The port number, e.g. 12345 (this is a 16-bit, IP socket number)
	 * @param username
	 *            The name of the user you want to be identified as
	 */
	public MessagingClient(String server, int port, String username) {
		this.server = server;
		this.portNumber = port;
		this.username = username;
		dbf.setNamespaceAware(true);
	}

	/**
	 * Start the dialog between the client, here, and the server.
	 * 
	 * @return if we were successful
	 */
	public boolean start() {
		// Future feature: Allow for this node NOT to be connected to a server.
		// if ( server == null )
		// return true;

		if (watchServer == null) {
			watchServer = new Thread(username + "ping") {
				long lastPingSent = 0l;

				/**
				 * We need to be sure that the server is still there.
				 * 
				 * There have been times when the server disappears (I think this is when the server machine accidentally goes to
				 * sleep/hybernates) and the client does not notice it.
				 * 
				 * This thread does that
				 */
				@Override
				public void run() {
					long sleep = (long) (5 * ONE_MINUTE + 1000 * Math.random()); // First sleep is 5 minutes, plus a little bit
					keepMessagingClientGoing = true;
					while (keepMessagingClientGoing) {
						catchSleep(sleep);
						if (lastMessageReceived + 5 * ONE_MINUTE < System.currentTimeMillis()
								&& lastPingSent + ONE_MINUTE < System.currentTimeMillis()) {
							// We haven't heard from the server in a long time! Maybe it is dead or sleeping.
							sendMessage(MessageCarrierXML.getIAmAlive(username, serverName));
							displayLogMessage(
									"Sending unsolicited 'IAmAlive' message from " + username + " to the server (" + serverName
											+ ") because last message was " + (System.currentTimeMillis() - lastMessageReceived)
											+ " mSec ago (" + new Date(lastMessageReceived) + ")");
							lastPingSent = System.currentTimeMillis();

							// 10/19/2015 -- This is almost always an indication that our connection to the server is dead. Pinging
							// it does not really seem to help.

							// I have found a situation where the socket seems to stay alive, but there is nothing at the other end.
							// If there are two clients at this network location that are trying to connect with the same name, the
							// server says, "no way" and then disconnects them both. The client that was already connected (the
							// first client) is now forgotten by the server, but it does not notice it. Eventually the first client
							// will get to this segment of the code.
						}
						sleep = 9999L; // subsequent sleeps are ten seconds
					}
					watchServer = null;
				}
			};
			watchServer.start();
		} else {
			displayLogMessage("Already running an instance of the 'watch server' thread for " + username);
			// catchSleep(1);
			// new Exception("watchServer is already running!").printStackTrace();
		}
		if (socket != null) {
			throw new RuntimeException("Already started the server for " + username + ".");
		}
		displayLogMessage("Connecting to server '" + server + "' on port " + portNumber + " at " + (new Date()));
		// new Exception("Connecting to server debug").printStackTrace();

		// try to connect to the server
		try {
			socket = new Socket(server, portNumber);
			// November 2019: It looks like Windows may have changed to a finite timeout by default.
			socket.setSoTimeout(0);
		} catch (UnknownHostException ec) {
			displayLogMessage(
					"The messaging server just does not exist at this time!! '" + server + ":" + portNumber + "'.  Exception is " + ec);
			disconnect();
			dontTryToConnectAgainUntilNow = System.currentTimeMillis() + 5 * ONE_MINUTE;
			displayLogMessage(false,
					"We will wait FIVE MINUTES (10 min.) before trying to connect to the server again for " + username);
			return false;
		} catch (Exception ec) {
			// if it failed not much I can do except wait
			displayLogMessage(
					"Error connecting " + username + " to messaging server, '" + server + ":" + portNumber + "'.  Exception is " + ec);
			disconnect();
			dontTryToConnectAgainUntilNow = System.currentTimeMillis() + 10 * ONE_SECOND;

			return false;
		}

		/* Creating both Data Stream */
		try {
			// sOutput = new ObjectOutputStream(socket.getOutputStream());
			// sInput = new ObjectInputStream(socket.getInputStream());
			/*
			 * From http://stackoverflow.com/questions/5658089/java-creating-a-new-objectinputstream-blocks:
			 * 
			 * When you construct an ObjectInputStream, in the constructor the class attempts to read a header that the associated
			 * ObjectOutputStream on the other end of the connection has written. It will not return until that header has been
			 * read. So if you are seeing the constructor 'hang', it's because the other side of the socket either hasn't used an
			 * ObjectOutputStream, or hasn't flushed the data yet.
			 * 
			 * So, if new ObjectInputStream blocks, it is because the server has not responded yet.
			 */

			sOutput = socket.getOutputStream();
			sInput = socket.getInputStream();
		} catch (IOException eIO) {
			displayLogMessage("IOException creating new Input/output Streams: " + eIO);
			socket = null;
			disconnect();
			dontTryToConnectAgainUntilNow = System.currentTimeMillis() + 10 * ONE_SECOND;
			return false;
		} catch (Exception ex) {
			displayLogMessage("A non IO Exception creating new Input/output Streams: " + ex);
			socket = null;
			disconnect();
			dontTryToConnectAgainUntilNow = System.currentTimeMillis() + 10 * ONE_SECOND;
			return false;
		}

		String msg = "Connection accepted to " + socket.getInetAddress() + ", port #" + socket.getPort() + ".  Username = '"
				+ username + "' at " + (new Date());
		displayLogMessage(msg);
		// new Exception("Connection accepted debug").printStackTrace();

		// creates the Thread to listen to the server
		keepMessagingClientGoing = true;
		listenFromServer = new ListenFromServer();
		listenFromServer.start();

		// Send our username to the server
		sendMessage(MessageCarrierXML.getLogin(username));
		connectionAccepted();

		// success! Inform the caller that it worked
		return true;
	}

	/**
	 * To send a message to the console or the GUI. Overridable to enable other sorts of usages.
	 * 
	 * @param msg
	 */
	public void displayLogMessage(final String msg) {
		displayLogMessage(true, msg);
	}

	public void displayLogMessage(boolean show, final String msg) {
		if (show)
			presentStatus = msg;
		println(getClass(), ": " + msg);
	}

	/**
	 * Overridable to enable other sorts of usages.
	 * 
	 * Changed the name from "displayIncomingMessage" in order to convey that this is an important aspect of dealing with messages
	 * that are received, beyond simply printing it out. In practice, many implementers of this method actually ANALYZE the message
	 * and act on it.
	 * 
	 * @param msg
	 *            The message that was received just now.
	 * @throws ErrorProcessingMessage
	 */
	public void receiveIncomingMessage(final MessageCarrierXML msg) throws ErrorProcessingMessage {
		System.out.println(msg);
	}

	/**
	 * Send a message to the server, converting it to a signed message if the messaging client, here, has the authority
	 * 
	 * @param msg
	 *            -- The message to sign and then send.
	 */
	public void sendMessage(MessageCarrierXML msg) {
		if (MessageConveyor.sendMessage(getClass(), msg, sOutput))
			return;

		if (showAllIncomingMessages)
			System.err.println(getClass().getSimpleName() + ": Sending message of type "
					+ msg.getMessageValue().getClass().getSimpleName() + " failed.");
		// Message failed. Do we need to reconnect?
		try {
			new Thread("ReconnectToServer") {
				@Override
				public void run() {
					catchSleep(1000);
					retryConnection();
				}
			}.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void connectionAccepted() {
		// Override this method if you need to know when a connection is accepted
	}

	/**
	 * 
	 */
	public void retryConnection() {
		// disconnect() calls retryConnection() at the end.
		disconnect();
		// retryConnection();
	}

	/**
	 * @return the user name of this client in the messaging system
	 */
	public String getName() {
		return username;
	}

	protected synchronized void connectionFailed() {
		if (restartThreadToServer != null && restartThreadToServer.isAlive())
			return; // Already trying to restart the thread

		if (processingGracefulExit) {
			displayLogMessage(MessagingClient.class.getSimpleName() + " (" + username
					+ "): Will not attempt reconnect since we seem to be exiting the JVM");
			return;
		}
		socket = null;

		// Wait until the server returns
		restartThreadToServer = new Thread(username + "_restart_connection") {
			long wait = dontTryToConnectAgainUntilNow - System.currentTimeMillis();

			@Override
			public void run() {
				// An observation (2/19/2016): It takes about two cycles of this while loop to reconnect to the server
				// synchronized (syncReconnects) { I think this sync is not necessary (there can only be one instance of this
				// thread }
				displayLogMessage(MessagingClient.class.getSimpleName() + " (" + username + "): Starting reconnect thread.");
				double counter = 1;
				while (socket == null) {
					if (wait < 0)
						wait = (long) (0.5 * WAIT_FOR_SERVER_TIME + (50.0 * Math.random()));

					displayLogMessage(false,
							username + ": Will wait " + wait + " milliseconds before trying to connect to the server again.");
					catchSleep(wait);
					// Add a random offset each time so that if there are lots of waiting clients, they don't all hit at once.
					wait = (long) (WAIT_FOR_SERVER_TIME * (0.5 + Math.log(counter)) + 100.0 * Math.random());
					counter += 0.2;
					if (counter > 90) // exp(4.5) = 90.00171313
						counter = 90; // Limit the maximum wait to about 5 minutes
					MessagingClient.this.start();
				}
				displayLogMessage(MessagingClient.class.getSimpleName() + ": Socket for " + username
						+ " is now viable; connection has been restored at " + (new Date()) + "\n\tSocket=[" + socket
						+ "]\n\tENDING reconnect thread.");
				restartThreadToServer = null;
			}
		};
		restartThreadToServer.start();
	}

	/**
	 * When something goes wrong Close the Input/Output streams, disconnect and then try to reconnect
	 */
	public void disconnect() {
		close();
		connectionFailed();
	}

	/**
	 * Disconnect from the server.
	 */
	protected void close() {
		keepMessagingClientGoing = false;
		String progressMessage = "Closing sockets;";
		try {
			if (sInput != null) {
				sInput.close();
				progressMessage += ", closed sInput";
			} else {
				progressMessage += ", (sInput was null already)";
			}
		} catch (Exception e) {
			System.err.println("Exception caught while closing sInput! " + e.getLocalizedMessage());
		} // not much else I can do
		try {
			if (sOutput != null) {
				sOutput.close();
				progressMessage += ", closed sOutput";
			} else {
				progressMessage += ", (sOutput was null already)";
			}
		} catch (Exception e) {
			System.err.println("Exception caught while closing sOutput! " + e.getLocalizedMessage());
		} // not much else I can do, although it might just be that it is already closed.
		try {
			if (socket != null) {
				socket.close();
				progressMessage += ", closed socket";
			} else {
				progressMessage += ", (socket was null already)";
			}
		} catch (Exception e) {
			System.err.println("Exception caught while closing socket! " + e.getLocalizedMessage());
		} // not much else I can do

		sInput = null;
		sOutput = null;
		socket = null;
		listenFromServer = null;
		println(getClass(), progressMessage);
	}

	/**
	 * To start the Client in console mode use one of the following command > java Client > java Client username > java Client
	 * username portNumber > java Client username portNumber serverAddress at the console prompt If the portNumber is not specified
	 * 1500 is used If the serverAddress is not specified "localHost" is used If the username is not specified "Anonymous" is used >
	 * java Client is equivalent to > java Client Anonymous 1500 localhost are eqquivalent
	 * 
	 * In console mode, if an error occurs the program simply stops when a GUI id used, the GUI is informed of the disconnection
	 * 
	 * @param args
	 *            Command-line arguments
	 */
	public static void main(String[] args) {
		// default values
		int portNumber = 32123; // MESSAGING_SERVER_PORT;
		String serverAddress = "localhost";
		String userName = "Anonymous";

		// depending of the number of arguments provided we fall through
		switch (args.length) {
		// > javac Client username portNumber serverAddr
		case 3:
			serverAddress = args[2];
			// > javac Client username portNumber
		case 2:
			try {
				portNumber = Integer.parseInt(args[1]);
			} catch (Exception e) {
				System.out.println("Invalid port number.");
				System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
				return;
			}
			// > javac Client username
		case 1:
			userName = args[0];
			// > java Client
		case 0:
			break;
		// invalid number of arguments
		default:
			System.out.println("Usage is: > java Client [username] [portNumber] {serverAddress]");
			return;
		}
		// create the Client object
		MessagingClient client = new MessagingClient(serverAddress, portNumber, userName);
		// test if we can start the connection to the Server
		// if it failed nothing we can do
		if (!client.start())
			return;

		// wait for messages from user
		try (Scanner scan = new Scanner(System.in)) {
			// loop forever for message from the user
			while (true) {
				System.out.print("> ");
				// read message from user
				String msg = scan.nextLine();
				// logout if message is LOGOUT
				if (msg.equalsIgnoreCase("LOGOUT")) {
					client.sendMessage(MessageCarrierXML.getLogout(userName));
					// Note: In this fake example, logging off will cause the system to try to log you back in in a moment.
					break; // break to do the disconnect
				}
				// message WhoIsIn
				else if (msg.equalsIgnoreCase("WHOISIN")) {
					client.sendMessage(MessageCarrierXML.getWhoIsIn(userName, serverAddress));
					// } else { // default to ordinary message
					// client.sendMessage(MessageCarrierXML.getMessage(userName, null, msg));
				}
			}
			// done: disconnect
			client.disconnect();
		}
	}

	/**
	 * @return Is this client a read-only client?
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * @param readOnly
	 *            -- Should this client be read-only?
	 */
	public void setReadOnly(final boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * Give the sub-classes here the chance to do something complicated when there is an error
	 * 
	 * @param why
	 *            Why was there an error
	 */
	public void replyToAnError(final String why) {
		System.err.println(why);
	}

	/**
	 * <p>
	 * A class that waits for the message from the server and append them to the JTextArea if we have a GUI or simply
	 * System.out.println() it in console mode
	 * </p>
	 * 
	 * <p>
	 * <b>Bug fix</b> <em>(December, 2014)</em> -- when the messaging server gets put into hibernation (a bad thing that really
	 * needs to be avoided!), the clients do not notice it. Because of the way the NUC computers in the ROC are set up, the easiest
	 * way to recover from the hibernation is to restart the server. When the server comes back online, the clients are no wiser and
	 * continue to believe that their socket to the server is OK. The only way to test this is to send the server a message from
	 * time to time.
	 * </p>
	 * <p>
	 * The existence of the attribute lastMessageReceiver is to make sure the server is still talking to me. The timeout of this
	 * test is handled in the method {@link #start() start} of class MessageClient.
	 * </p>
	 */
	class ListenFromServer extends Thread {

		private long nextDisplayTime = System.currentTimeMillis();

		@Override
		public void run() {
			int aliveCount = 0;
			if (!keepMessagingClientGoing)
				System.err.println("ListenFromServer.run(): internal error encountered; keepMessagingClientGoing is false.");
			BufferedReader receiver = new BufferedReader(new InputStreamReader(sInput));
			while (keepMessagingClientGoing) {
				try {
					MessageCarrierXML msg = MessageConveyor.getNextDocument(getClass(), receiver, "the messaging server");
					lastMessageReceived = System.currentTimeMillis();
					dumpMessage(msg);

					if (showAllIncomingMessages)
						println(getClass(), "Received this message: " + msg);

					if (MessageCarrierXML.isUsernameMatch(msg.getMessageRecipient(), username)) {
						Class<? extends MessagingDataXML> clazz = msg.getMessageValue().getClass();
						if (clazz.equals(AreYouAliveMessage.class)) {
							// displayLogMessage("Replying that I am alive to " + msg.getFrom());
							sendMessage(MessageCarrierXML.getIAmAlive(username, msg.getMessageOriginator()));
						} else if (clazz.equals(YesIAmAliveMessage.class)) {
							// Print out some of these messages for the log file
							aliveCount++;
							if (System.currentTimeMillis() > nextDisplayTime) {
								displayLogMessage(false,
										ListenFromServer.class.getSimpleName() + ": Got 'AMALIVE' message from "
												+ msg.getMessageOriginator() + ", message=[" + msg.getMessageValue() + "] ["
												+ aliveCount + "]");
								if (System.currentTimeMillis() > nextDisplayTime + 15000L)
									// Continue to print these for 15 seconds, and the wait an hour to do it again.
									nextDisplayTime = System.currentTimeMillis() + 60 * ONE_MINUTE;
							}
						}

					} else {
						displayLogMessage(
								ListenFromServer.class.getSimpleName() + ": Got a message that is not for me! Intended for "
										+ msg.getMessageRecipient() + ", I am " + username);
						System.out.println("\t\t" + msg);
					}
					receiveIncomingMessage(msg);

				} catch (UnrecognizedCommunicationException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					displayLogMessage("NullPointerException from reading server!");
					e.printStackTrace();
					break; // Leave the forever loop
				} catch (ErrorProcessingMessage e) {
					// TODO -- We are catching a failure from the lowest level. Need to send an error response to the sender.
					e.printStackTrace();
				} catch (SocketTimeoutException e) {
					// This is unlikely to happen, given the way we have designed the method.  But it is here anyway,
					e.printStackTrace();
				}
			} // end of while forever loop

			displayLogMessage(ListenFromServer.class.getSimpleName() + ": Disconnecting from server.");
			disconnect();
			displayLogMessage(ListenFromServer.class.getSimpleName() + ": Exiting listening thread.");
		}

		long						nextDump		= 0L;
		int							messageCounter	= 0;
		private static final long	QUIET_INTERVAL	= 15 * ONE_MINUTE;
		private static final long	NOISY_INTERVAL	= 20 * ONE_SECOND;

		/**
		 * Dump this to the diagnostics stream every 15 minutes, for 20 seconds.
		 * 
		 * @param read
		 *            The message to dump
		 */
		private void dumpMessage(Object read) {
			// For the Display messaging client, almost all of the received messages are "Are You Alive?"
			messageCounter++;
			if (nextDump > System.currentTimeMillis())
				return;
			displayLogMessage("Received message #" + messageCounter + ": [" + read + "]");
			if (nextDump + NOISY_INTERVAL < System.currentTimeMillis())
				nextDump = System.currentTimeMillis() + QUIET_INTERVAL;
		}
	}

	public String getPresentStatus() {
		return presentStatus;
	}

}
