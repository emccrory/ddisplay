package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.util.Util.catchSleep;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
	private ObjectInputStream	sInput;				// to read from the socket
	private ObjectOutputStream	sOutput;				// to write on the socket
	private Socket				socket	= null;

	// the server, the port and the username
	private String				server, username;

	private int					port;
	private ListenFromServer	listenFromServer;
	private Thread				restartThreadToServer;

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
	 *            The message to display
	 */
	public void displayIncomingMessage(final MessageCarrier msg) {
		System.out.println(msg);
	}

	/**
	 * To send a message to the server
	 * 
	 * @param msg
	 */
	public void sendMessage(MessageCarrier msg) {
		try {
			sOutput.writeObject(msg);
			sOutput.reset();
		} catch (Exception e) {
			displayLogMessage(getClass().getSimpleName() + ".sendMessage(): Exception writing to server: " + e);
			new Thread("ReconnectToServer") {
				public void run() {
					connectionFailed();
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
		connectionFailed();
	}

	/**
	 * @return the username of this client in the messaging system
	 */
	public String getName() {
		return username;
	}

	protected void connectionFailed() {
		if (restartThreadToServer != null && restartThreadToServer.isAlive())
			return; // Already trying to restart the thread

		socket = null;

		// Wait until the server returns
		restartThreadToServer = new Thread() {
			long	wait	= 10000L;

			public void run() {
				while (socket == null) {
					displayLogMessage("Will wait " + (wait / 1000L)
							+ " seconds for server to return and then try to connect again.");
					catchSleep(wait);
					wait = (wait + 10000L > ONE_MINUTE ? ONE_MINUTE : wait + 10000L);
					if (!MessagingClient.this.start()) {
						displayLogMessage(this.getClass().getSimpleName() + ".connectionFailed(): Server start failed again at "
								+ (new Date()) + "...");
					}
				}
				displayLogMessage(this.getClass().getSimpleName() + ".connectionFailed(): Socket is now viable [" + socket
						+ "]; connection has been restored at " + (new Date()));
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
	 * a class that waits for the message from the server and append them to the JTextArea if we have a GUI or simply
	 * System.out.println() it in console mode
	 */
	class ListenFromServer extends Thread {
		public boolean	keepGoing	= true;

		public void run() {
			while (keepGoing) {
				try {
					MessageCarrier msg = (MessageCarrier) sInput.readObject();
					if (msg.isThisForMe(username) && msg.getType() == MessageType.ISALIVE) {
						sendMessage(MessageCarrier.getIAmAlive(username, msg.getFrom(), "" + new Date()));
					} else
						displayIncomingMessage(msg);
				} catch (IOException e) {
					displayLogMessage("Server has closed the connection: " + e);
					break; // Leave the forever loop
				} catch (ClassNotFoundException e) {
					System.err.println("Exception caught at " + new Date());
					// can't happen with a String object but need the catch anyhow
					e.printStackTrace();
					// try to continue anyway ...
				} catch (NullPointerException e) {
					displayLogMessage("NullPointerException from reading server!");
					break; // Leave the forever loop
				}
			}
			disconnect();
		}
	}
}
