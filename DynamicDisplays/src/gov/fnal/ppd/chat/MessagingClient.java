package gov.fnal.ppd.chat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;

/**
 * The Client that can be run both as a console or a GUI
 * 
 * Taken from http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/ on 5/12/2014
 * Extensively modified by Elliott McCrory, Fermilab AD/Instrumentation, 2014
 */
public class MessagingClient {

	// for I/O
	private ObjectInputStream	sInput;		// to read from the socket
	private ObjectOutputStream	sOutput;		// to write on the socket
	private Socket				socket	= null;

	// the server, the port and the username
	private String				server, username;
	private int					port;

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
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		}
		// if it failed not much I can so
		catch (Exception ec) {
			displayLogMessage("Error connectiong to server:" + ec);
			ec.printStackTrace();
			return false;
		}

		String msg = "Connection accepted to " + socket.getInetAddress() + ", port " + socket.getPort() + ".  Username = '"
				+ username + "'";
		displayLogMessage(msg);

		/* Creating both Data Stream */
		try {
			sOutput = new ObjectOutputStream(socket.getOutputStream());
			sInput = new ObjectInputStream(socket.getInputStream());
		} catch (IOException eIO) {
			displayLogMessage("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server
		new ListenFromServer().start();
		// Send our username to the server this is the only message that we
		// will send as a String. All other messages will be ChatMessage objects
		try {
			sOutput.writeObject(username);
		} catch (IOException eIO) {
			displayLogMessage("Exception doing login : " + eIO);
			disconnect();
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
		System.out.println(msg);
	}

	/**
	 * Overridable to enable other sorts of usages.
	 * 
	 * @param msg
	 *            The message to display
	 */
	public void displayIncomingMessage(final String msg) {
		System.out.println(msg);
		System.out.print("> ");
	}

	/**
	 * To send a message to the server
	 * 
	 * @param msg
	 */
	public void sendMessage(MessageCarrier msg) {
		try {
			sOutput.writeObject(msg);
		} catch (Exception e) {
			displayLogMessage("Exception writing to server: " + e);
			e.printStackTrace();
		}
	}

	protected void connectionAccepted() {
		// Override this method if you need to know when a connection is accepted
	}

	protected void connectionFailed() {
		socket = null;

		long wait = 20000L;
		// Wait until the server returns
		while (socket == null) {
			displayLogMessage("Will wait " + wait + " ms for server to return.");
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			wait = 10000L;
			start();
		}
	}

	/*
	 * When something goes wrong Close the Input/Output streams and disconnect not much to do in the catch clause
	 */
	private void disconnect() {
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
		int portNumber = 1500;
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
				// break to do the disconnect
				break;
			}
			// message WhoIsIn
			else if (msg.equalsIgnoreCase("WHOISIN")) {
				client.sendMessage(MessageCarrier.getWhoIsIn());
			} else { // default to ordinary message
				client.sendMessage(MessageCarrier.getMessage(msg));
			}
		}
		// done disconnect
		client.disconnect();
	}

	/**
	 * a class that waits for the message from the server and append them to the JTextArea if we have a GUI or simply
	 * System.out.println() it in console mode
	 */
	class ListenFromServer extends Thread {

		public void run() {
			while (true) {
				try {
					String msg = (String) sInput.readObject();
					// if console mode print the message and add back the prompt
					displayIncomingMessage(msg);
				} catch (IOException e) {
					displayLogMessage("Server has closed the connection: " + e);
					connectionFailed();
					System.err.println("Server has closed the connection at " + new Date());
					e.printStackTrace();
					break;
				} catch (ClassNotFoundException e) {
					System.err.println("Exception caught at " + new Date());
					// can't happen with a String object but need the catch anyhow
					e.printStackTrace();
				}
			}
		}
	}
}
