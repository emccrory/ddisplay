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
import static gov.fnal.ppd.dd.util.GeneralUtilities.println;

import java.util.Date;

import gov.fnal.ppd.dd.CredentialsNotFoundException;
import gov.fnal.ppd.dd.util.JavaVersion;

/**
 * <p>
 * The messaging server as a daemon (not a GUI)
 * </p>
 * <p>
 * Extensively modified by Elliott McCrory, Fermilab AD/Instrumentation, 2014
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 */
public class MessagingServerDaemon {

	// my server
	private static LocalMessagingServer	server;
	private int							port;

	private class LocalMessagingServer extends MessagingServer {
		public LocalMessagingServer(int port) {
			super(port);
		}
	};

	// server constructor that receive the port to listen to for connection as parameter
	MessagingServerDaemon(int port) {
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
		server.logger.info("Server started at " + (new Date()) + "\n");
	}

	/**
	 * entry point to start the Server without the GUI
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		println(MessagingServerDaemon.class, "Running from java version " + JavaVersion.getCurrentVersion());
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
	class ServerRunning extends Thread {
		public void run() {
			server.start(); // should stay in this method until it fails

			// the server failed
			server.logger.info("Server has stopped at " + (new Date()) + "\n");
			server = null;
		}
	}

}
