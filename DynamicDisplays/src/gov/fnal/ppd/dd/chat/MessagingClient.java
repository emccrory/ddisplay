/*
 * MessagingClient
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.dd.GlobalVariables.WAIT_FOR_SERVER_TIME;
import static gov.fnal.ppd.dd.GlobalVariables.checkSignedMessages;
import static gov.fnal.ppd.dd.chat.MessagingServer.SPECIAL_SERVER_MESSAGE_USERNAME;
import static gov.fnal.ppd.dd.util.Util.catchSleep;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SignedObject;
import java.util.Date;
import java.util.Scanner;

/**
 * <p>
 * The Client that can be run both as a console or a GUI
 * </p>
 * 
 * <p>
 * Taken from the internet on 5/12/2014. @see <a
 * href="http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/" dreamincode.net</a>
 * </p>
 * <p>
 * Extensively modified by Elliott McCrory, Fermilab AD/Instrumentation, 2014-16
 * </p>
 * <p>
 * TODO -- Is there a way for two clients on the same node to talk directly to each other without a server? There is a use case that
 * has emerged whereby the user interacts with a display directly on that display through the Channel Selector. When a choice is
 * made, it sends this to the messaging server, which sends it right back to the same node but communicating with the display. This
 * would only work if there was some way to short-circuit this communication here in the messaging client. At this time (January,
 * 2016), the solution is not at all obvious (if it is possible at all).
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 */
public class MessagingClient {

	// for I/O
	private ObjectInputStream	sInput;															// to read from the socket
	private ObjectOutputStream	sOutput;															// to write on the socket
	private Socket				socket							= null;

	// the server, the port and the username
	private String				server, username;

	private int					port;
	private ListenFromServer	listenFromServer;
	private Thread				restartThreadToServer;

	protected long				lastMessageReceived				= 0l;
	private boolean				keepMessagingClientGoing		= true;

	private Boolean				syncReconnects					= false;
	private final String		serverName						= SPECIAL_SERVER_MESSAGE_USERNAME;

	private boolean				readOnly						= true;
	private Thread				watchServer						= null;
	private long				dontTryToConnectAgainUntilNow	= System.currentTimeMillis();

	/**
	 * Constructor called by console mode server: the server address port: the port number username: the username
	 * 
	 * @param server
	 *            The name of the server, e.g., "localhost"
	 * @param port
	 *            The port number, e.g. 1500
	 * @param username
	 *            The name of the user you want to be identified as
	 */
	public MessagingClient(String server, int port, String username) {
		this.server = server;
		this.port = port;
		this.username = username;
	}

	/**
	 * Start the dialog between the client, here, and the server.
	 * 
	 * @return if we were successful
	 */
	public boolean start() {
		if (watchServer == null) {
			watchServer = new Thread(username + "ping") {
				long	lastPingSent	= 0l;

				/**
				 * We need to be sure that the server is still there.
				 * 
				 * There have been times when the server disappears (I think this is when the server machine accidentally goes to
				 * sleep/hibernates) and the client does not notice it.
				 * 
				 * This thread does that
				 */
				public void run() {
					long sleep = (long) (5 * ONE_MINUTE + 1000 * Math.random()); // First sleep is 5 minutes, plus a little bit
					keepMessagingClientGoing = true;
					while (keepMessagingClientGoing) {
						catchSleep(sleep);
						if (lastMessageReceived + 5 * ONE_MINUTE < System.currentTimeMillis()
								&& lastPingSent + ONE_MINUTE < System.currentTimeMillis()) {
							// We haven't heard from the server in a long time! Maybe it is dead or sleeping.
							sendMessage(MessageCarrier.getIAmAlive(username, serverName, new Date().toString()));
							displayLogMessage("Sending unsolicited 'IAmAlive' message from " + username + " to the server ("
									+ serverName + ") because last message was "
									+ (System.currentTimeMillis() - lastMessageReceived) + " mSec ago ("
									+ new Date(lastMessageReceived) + ")");
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
			displayLogMessage("Already running an instance of the 'watch server' thread.");
			catchSleep(1);
			new Exception("watchServer is already running!").printStackTrace();
		}
		if (socket != null) {
			throw new RuntimeException("Already started the server.");
		}
		displayLogMessage("Connecting to server '" + server + "' on port " + port + " at " + (new Date()));
		// new Exception("Connecting to server debug").printStackTrace();

		// try to connect to the server
		try {
			socket = new Socket(server, port);
		} catch (UnknownHostException ec) {
			displayLogMessage("The messaging server just does not exist at this time!! '" + server + ":" + port
					+ "'.  Exception is " + ec);
			disconnect();
			dontTryToConnectAgainUntilNow = System.currentTimeMillis() + 10 * ONE_MINUTE;
			displayLogMessage("We will wait THIRTY MINUTES (30 min.) before trying to connect to the server again!");
			return false;
		} catch (Exception ec) {
			// if it failed not much I can so
			displayLogMessage("Error connecting to messaging server, '" + server + ":" + port + "'.  Exception is " + ec);
			disconnect();
			dontTryToConnectAgainUntilNow = System.currentTimeMillis() + 10 * ONE_SECOND;
			return false;
		}

		/* Creating both Data Stream */
		try {
			sOutput = new ObjectOutputStream(socket.getOutputStream());
			sInput = new ObjectInputStream(socket.getInputStream());

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

		String msg = "Connection accepted to " + socket.getInetAddress() + ", port " + socket.getPort() + ".  Username = '"
				+ username + "' at " + (new Date());
		displayLogMessage(msg);
		// new Exception("Connection accepted debug").printStackTrace();

		// creates the Thread to listen to the server
		keepMessagingClientGoing = true;
		listenFromServer = new ListenFromServer();
		listenFromServer.start();

		// Send our username to the server

		try {
			sOutput.writeObject(MessageCarrier.getLogin(username));
			sOutput.reset();
		} catch (IOException eIO) {
			displayLogMessage("Exception doing login : " + eIO);
			disconnect();
			socket = null;
			return false;
		}

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
		System.out.println(new Date() + ": " + msg);
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
	 */
	public void receiveIncomingMessage(final MessageCarrier msg) {
		System.out.println(msg);
	}

	/**
	 * Send a message to the server, converting it to a signed message if the messaging client, here, has the authority
	 * 
	 * @param msg
	 *            -- The message to sign and then send.
	 */
	public void sendMessage(MessageCarrier msg) {
		try {
			// System.out.println(MessagingClient.class.getSimpleName() + ".sendMessage(" + msg + ").  isReadOnly() = "
			// + msg.getType().isReadOnly());
			if (msg.getType().isReadOnly())
				sOutput.writeObject(msg);
			else
				sOutput.writeObject(msg.getSignedObject());

			// The following line is controversial: Is it a bug or is it a feature? Sun claims it is a feature -- it allows the
			// sender to re-send any message easily. But others (myself included) see this as a bug -- if you remember every message
			// every time, you'll run out of memory eventually. The call to "reset()" causes the output object to ignore this,
			// and all previous, messages it has sent.
			sOutput.reset();
			return;
		} catch (Exception e) {
			displayLogMessage(MessagingClient.class.getSimpleName() + ".sendMessage(): Exception writing to server: " + msg);
			catchSleep(1);
			System.err.println("Exception writing to server ... ");
			e.printStackTrace();
		}
		try {
			new Thread("ReconnectToServer") {
				public void run() {
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

		socket = null;

		// Wait until the server returns
		restartThreadToServer = new Thread(username + "_restart_connection") {
			long	wait	= dontTryToConnectAgainUntilNow - System.currentTimeMillis();

			public void run() {
				// An observation (2/19/2016): It takes about two cycles of this while loop to reconnect to the server
				synchronized (syncReconnects) {
					syncReconnects = true;
					while (socket == null) {
						if (wait < 0)
							wait = WAIT_FOR_SERVER_TIME;

						displayLogMessage("Will wait " + (wait / 1000L) + " secs before trying to connect to the server again.");
						catchSleep(wait);
						wait = WAIT_FOR_SERVER_TIME;
						if (!MessagingClient.this.start()) {
							displayLogMessage(MessagingClient.class.getSimpleName()
									+ ".connectionFailed(): Server start failed again at " + (new Date()) + "...");
						}
					}
					displayLogMessage(MessagingClient.class.getSimpleName() + ": Socket is now viable [" + socket
							+ "]; connection has been restored at " + (new Date()));
					restartThreadToServer = null;
				}
				syncReconnects = false;
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
		try {
			if (sInput != null)
				sInput.close();
		} catch (Exception e) {
			System.err.println("Exception caught while closing sInput! " + e.getLocalizedMessage());
		} // not much else I can do
		try {
			if (sOutput != null)
				sOutput.close();
		} catch (Exception e) {
			System.err.println("Exception caught while closing sOutput! " + e.getLocalizedMessage());
		} // not much else I can do, although it might just be that it is already closed.
		try {
			if (socket != null)
				socket.close();
		} catch (Exception e) {
			System.err.println("Exception caught while closing socket! " + e.getLocalizedMessage());
		} // not much else I can do

		sInput = null;
		sOutput = null;
		socket = null;
		listenFromServer = null;
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
		Scanner scan = new Scanner(System.in);
		// loop forever for message from the user
		while (true) {
			System.out.print("> ");
			// read message from user
			String msg = scan.nextLine();
			// logout if message is LOGOUT
			if (msg.equalsIgnoreCase("LOGOUT")) {
				client.sendMessage(MessageCarrier.getLogout());
				// Note: In this fake example, logging off will cause the system to try to log you back in in a moment.
				break; // break to do the disconnect
			}
			// message WhoIsIn
			else if (msg.equalsIgnoreCase("WHOISIN")) {
				client.sendMessage(MessageCarrier.getWhoIsIn(userName));
			} else { // default to ordinary message
				client.sendMessage(MessageCarrier.getMessage(userName, null, msg));
			}
		}
		// done: disconnect
		client.disconnect();
	}

	/**
	 * @return Is this client a rad-only client?
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

		private long	nextDisplayTime	= System.currentTimeMillis();

		public void run() {
			boolean showMessage1 = true, showMessage2 = true, showMessage3 = true;
			int aliveCount = 0;
			if (!keepMessagingClientGoing)
				System.err.println("ListenFromServer.run(): internal error encountered; keepMessagingClientGoing is false.");
			while (keepMessagingClientGoing) {
				try {
					MessageCarrier msg;
					Object read = sInput.readObject();
					lastMessageReceived = System.currentTimeMillis();
					dumpMessage(read);

					if (read instanceof MessageCarrier) {
						msg = (MessageCarrier) read;
						if (checkSignedMessages() && !msg.getType().isReadOnly()) {
							System.err.println("MessagingClient:ListenFromServer.run(): "
									+ "Received message is unsigned and could cause a 'write' to happen:\n" + "[" + msg
									+ "] -- IGNORING IT!");
							continue;
						}
					} else if (read instanceof SignedObject) {
						SignedObject signedObject = (SignedObject) read;
						msg = (MessageCarrier) signedObject.getObject();
						String signatureString = msg.verifySignedObject(signedObject);
						if (signatureString == null) {
							if (showMessage1) {
								System.out.println("Message is properly signed: " + msg);
								showMessage1 = false;
							}
							// TODO -- Is the IP address within the message correct? Should verify that, too.
						} else {
							if (msg.getType().isReadOnly()) {
								if (showMessage2) {
									System.out.println(new Date() + "Message is NOT properly signed: [" + msg + "]; reason = '"
											+ signatureString + "' -- but it is a read-only message, so we'll accept it.");
									showMessage2 = false;
								}
							} else {
								if (showMessage3)
									System.err.print(new Date() + "Message is NOT PROPERLY SIGNED: [" + msg + "]; reason = '"
											+ signatureString + "'-- Ignoring this message!");
								showMessage3 = false;
								catchSleep(500L);
								continue;
							}
						}
					} else {
						System.err.println("Got a message that was not of an expected type " + read.getClass().getCanonicalName()
								+ " at " + new Date());
						new ClassNotFoundException(read.getClass().getCanonicalName()).printStackTrace();
						// try to continue anyway ...
						catchSleep(1000L);
						continue;
					}

					// if (msg.isThisForMe(username)) {
					if (MessageCarrier.isUsernameMatch(msg.getTo(), username)) {
						switch (msg.getType()) {
						case ISALIVE:
							// displayLogMessage("Replying that I am alive to " + msg.getFrom());
							sendMessage(MessageCarrier.getIAmAlive(username, msg.getFrom(), "" + new Date()));
							// All done with this iteration of the loop.
							continue;

						case AMALIVE:
							// Print out some of these messages for the log file
							aliveCount++;
							if (System.currentTimeMillis() > nextDisplayTime) {
								displayLogMessage(ListenFromServer.class.getSimpleName() + ": Got 'AMALIVE' message from "
										+ msg.getFrom() + " [" + aliveCount + "]");
								if (System.currentTimeMillis() > nextDisplayTime + 15000L)
									// Continue to print these for 15 seconds, and the wait an hour to do it again.
									nextDisplayTime = System.currentTimeMillis() + 60 * ONE_MINUTE;
							}
							break;

						case LOGIN:
						case LOGOUT:
						case WHOISIN:
							displayLogMessage(ListenFromServer.class.getSimpleName() + ": Got a message that is being ignored.");
							System.out.println("\t\t" + msg);
							continue;

						case ERROR:
						case MESSAGE:
							// Normal stuff here
							break;
						}

						receiveIncomingMessage(msg);

					} else {
						displayLogMessage(ListenFromServer.class.getSimpleName()
								+ ": Got a message that is not for me! Intended for " + msg.getTo() + ", I am " + username);
						System.out.println("\t\t" + msg);
						receiveIncomingMessage(msg);
					}
				} catch (IOException e) {
					displayLogMessage("Server has closed the connection: " + e);
					break; // Leave the forever loop
				} catch (NullPointerException e) {
					displayLogMessage("NullPointerException from reading server!");
					e.printStackTrace();
					break; // Leave the forever loop
				} catch (ClassNotFoundException e) {
					// can't happen with an Object cast, but need the catch anyhow
					e.printStackTrace();
				}
			}
			displayLogMessage(ListenFromServer.class.getSimpleName() + ": Disconnecting from server.");
			disconnect();
		}

		long	nextDump	= 0L;

		/**
		 * Dump this to the diagnostics stream every 10 minutes or so, for about 30 seconds.
		 * 
		 * @param read
		 *            The message to dump
		 */
		private void dumpMessage(Object read) {
			if (nextDump > System.currentTimeMillis())
				return;
			displayLogMessage("Received message: [" + read + "]");
			if (nextDump + 30000L < System.currentTimeMillis())
				nextDump = System.currentTimeMillis() + 10 * 60 * 1000L;
		}
	}
}
