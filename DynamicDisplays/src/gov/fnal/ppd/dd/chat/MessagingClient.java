package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.checkSignedMessages;
import static gov.fnal.ppd.dd.util.Util.catchSleep;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
 * Extensively modified by Elliott McCrory, Fermilab AD/Instrumentation, 2014
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 */
public class MessagingClient {

	// for I/O
	private ObjectInputStream	sInput;							// to read from the socket
	private ObjectOutputStream	sOutput;							// to write on the socket
	private Socket				socket				= null;

	// the server, the port and the username
	private String				server, username;

	private int					port;
	private ListenFromServer	listenFromServer;
	private Thread				restartThreadToServer;

	private long				lastMessageReceived	= 0l;
	private boolean				keepGoing			= true;

	private Object				syncReconnects		= new Object();
	private String				serverName;

	private boolean				readOnly			= true;

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
	 * To start the dialog
	 * 
	 * @return if we were successful
	 */
	public boolean start() {
		new Thread(username + "ping") {
			/**
			 * We need to be sure that the server is still there.
			 * 
			 * There have been times when the server disappears (I think this is when the server machine accidentally goes to
			 * sleep/hibernates) and the client does not notice it.
			 * 
			 * This thread does that
			 */
			public void run() {
				long sleep = 60000L;
				while (keepGoing) {
					catchSleep(sleep);
					if (lastMessageReceived + 5 * ONE_MINUTE < System.currentTimeMillis()) {
						// We haven't heard from the server in a long time! Maybe it is dead or sleeping.
						sendMessage(MessageCarrier.getIAmAlive(username, serverName, new Date().toString()));
						displayLogMessage("Sending an unsolicited 'IAmAlive' message to the server (" + serverName
								+ ") because we last got a message " + (System.currentTimeMillis() - lastMessageReceived)
								+ " msec ago");
						lastMessageReceived = System.currentTimeMillis() - ONE_MINUTE; // Wait a minute before trying again
					}
					sleep = 10000L;
				}
			}
		}.start();
		if (socket != null) {
			throw new RuntimeException("Already started the server.");
		}
		displayLogMessage("Connecting to server '" + server + "' on port " + port + " at " + (new Date()));
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		}
		// if it failed not much I can so
		catch (Exception ec) {
			displayLogMessage("Error connectiong to server:" + ec);
			disconnect();
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
			displayLogMessage("Exception creating new Input/output Streams: " + eIO);
			socket = null;
			disconnect();
			return false;
		}

		String msg = "Connection accepted to " + socket.getInetAddress() + ", port " + socket.getPort() + ".  Username = '"
				+ username + "' at " + (new Date());
		displayLogMessage(msg);

		// creates the Thread to listen from the server
		listenFromServer = new ListenFromServer();
		listenFromServer.start();

		// Send our username to the server this is the only message that we
		// will send as a String. All other messages will be ChatMessage objects

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

		// success we inform the caller that it worked
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
	 * @param msg
	 *            The message that was received just now.
	 */
	public void displayIncomingMessage(final MessageCarrier msg) {
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

		} catch (Exception e) {
			displayLogMessage(getClass().getSimpleName() + ".sendMessage(): Exception writing to server: " + e);
			new Thread("ReconnectToServer") {
				public void run() {
					retryConnection();
				}
			}.start();
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
			long	wait	= 10000L;

			public void run() {
				synchronized (syncReconnects) {
					while (socket == null) {
						displayLogMessage("Will wait " + (wait / 1000L)
								+ " seconds for server to return and then try to connect again.");
						catchSleep(wait);
						wait = (wait + 10000L > ONE_MINUTE ? ONE_MINUTE : wait + 10000L);
						if (!MessagingClient.this.start()) {
							displayLogMessage(this.getClass().getSimpleName()
									+ ".connectionFailed(): Server start failed again at " + (new Date()) + "...");
						}
					}
					displayLogMessage(this.getClass().getSimpleName() + ": Socket is now viable [" + socket
							+ "]; connection has been restored at " + (new Date()));
					restartThreadToServer = null;
				}
			}
		};
		restartThreadToServer.start();
	}

	/**
	 * When something goes wrong Close the Input/Output streams and disconnect not much to do in the catch clause
	 */
	public void disconnect() {
		try {
			if (sInput != null)
				sInput.close();
		} catch (Exception e) {
		} // not much else I can do
		try {
			if (sOutput != null)
				sOutput.close();
		} catch (Exception e) {
		} // not much else I can do
		try {
			if (socket != null)
				socket.close();
		} catch (Exception e) {
		} // not much else I can do

		sInput = null;
		sOutput = null;
		socket = null;
		listenFromServer = null;
		connectionFailed();
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

		public void run() {
			boolean showMessage1 = true, showMessage2 = true, showMessage3 = true;
			while (keepGoing) {
				try {
					MessageCarrier msg;
					Object read = sInput.readObject();
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
						if (msg.verifySignedObject(signedObject)) {
							if (showMessage1) {
								System.out.println("Message is properly signed: " + msg);
								showMessage1 = false;
							}
						} else {
							if (msg.getType().isReadOnly()) {
								if (showMessage2) {
									System.out.println(new Date() + "Message is NOT properly signed: [" + msg
											+ "] -- but it is a read-only message, so we'll accept it.");
									showMessage2 = false;
								}
							} else {
								if (showMessage3)
									System.err.print(new Date() + "Message is NOT PROPERLY SIGNED: [" + msg
											+ "] -- Ignoring this message!");
								showMessage3 = false;
								catchSleep(500L);
								continue;
							}
						}
					} else {
						System.err.println("Exception caught at " + new Date());
						new ClassNotFoundException().printStackTrace();
						// try to continue anyway ...
						catchSleep(1000L);
						continue;
					}

					lastMessageReceived = System.currentTimeMillis();
					serverName = msg.getFrom();
					if (msg.isThisForMe(username) && msg.getType() == MessageType.ISALIVE) {
						sendMessage(MessageCarrier.getIAmAlive(username, msg.getFrom(), "" + new Date()));
					} else
						displayIncomingMessage(msg);
				} catch (IOException e) {
					displayLogMessage("Server has closed the connection: " + e);
					break; // Leave the forever loop
				} catch (NullPointerException e) {
					displayLogMessage("NullPointerException from reading server!");
					break; // Leave the forever loop
				} catch (ClassNotFoundException e) {
					// can't happen with an Object cast, but need the catch anyhow
					e.printStackTrace();
				}
			}
			disconnect();
		}
	}
}
