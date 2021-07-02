package gov.fnal.ppd.dd.chat;

/*
 * MessagingServerGUI
 *
 * Original version: Taken from the internet on 5/12/2014. @see <a
 * href="http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/"> dreamincode.net</a>
 * 
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */

import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.prepareUpdateWatcher;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import java.util.Date;

import gov.fnal.ppd.dd.util.nonguiUtils.CredentialsNotFoundException;
import gov.fnal.ppd.dd.util.nonguiUtils.JavaVersion;

/**
 * <p>
 * The messaging server as a daemon (not a GUI).
 * </p>
 * <p>
 * <b>History</b> - When the Dynamic Display system first started, it was nice to have the central messaging server be a GUI. All
 * the messages were dumped onto the GUI, and I could keep track of it rather easily. But as the system matured, it became apparent
 * that the overhead of the GUI was unnecessary - just be sure that everything gets put into log files. So that is what we have now.
 * </p>
 * <p>
 * This class, as you will see, is very small. Essentially all this class does is provide a main method to run the messaging server
 * itself (class MessagingServer)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 */
public class MessagingServerDaemon {

	/// The local server as an attribute to this class
	private static LocalMessagingServer	server;

	/// The IP port (socket) number on which all communications begin
	private int							port;

	/**
	 * Local instance of the MessagingServer class
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 *
	 */
	private class LocalMessagingServer extends MessagingServer {
		public LocalMessagingServer(int port) {
			super(port);
		}
	};

	/// server constructor that receive the port to listen to for connection as parameter
	private MessagingServerDaemon(int port) {
		// super("Dynamic Displays Messaging Server");
		server = null;
		this.port = port;
	}

	/**
	 * Start listening for messages
	 */
	public void start() {
		// create a new Server
		server = new LocalMessagingServer(port);
		// and start it as a thread
		new ServerRunning().start();
		server.logger.info(getClass() + ": Server started at " + (new Date()) + "\n");
	}

	/**
	 * This is the entry point to start the Server that does not use a GUI
	 * 
	 * @param args (No arguments are used)
	 */
	public static void main(String[] args) {
		println(MessagingServerDaemon.class, "Running from java version " + JavaVersion.getInstance().get());
		prepareUpdateWatcher(true);

		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		MessagingServerDaemon ms = new MessagingServerDaemon(MESSAGING_SERVER_PORT);
		ms.start();
	}

	/**
	 * A thread to run the Server
	 */
	private class ServerRunning extends Thread {
		public void run() {
			server.start(); // should stay in the call of this method until it fails

			// the server failed
			server.logger.info(MessagingServerDaemon.class + ": Server has stopped at " + (new Date()) + "\n");
			server = null;
		}
	}

}
