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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Date;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import gov.fnal.ppd.dd.CredentialsNotFoundException;
import gov.fnal.ppd.dd.util.guiUtils.JTextAreaBottom;
import gov.fnal.ppd.dd.util.nonguiUtils.JavaVersion;

/**
 * <p>
 * The messaging server as a GUI
 * </p>
 * <p>
 * Extensively modified by Elliott McCrory, Fermilab AD/Instrumentation, 2014
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 */
public class MessagingServerGUI extends JFrame implements ActionListener, WindowListener {

	private static final long		serialVersionUID	= 1L;
	// the stop and start buttons
	private JButton					stopStart;
	// JTextArea for the chat room and the events
	private JTextAreaBottom			chat, event;
	// The port number
	private JTextField				tPortNumber;
	// my server
	private LocalMessagingServer	server;

	private class LocalMessagingServer extends MessagingServer {
		public LocalMessagingServer(int port) {
			super(port);
		}
	};

	// server constructor that receive the port to listen to for connection as parameter
	MessagingServerGUI(int port) {
		super("Dynamic Displays Messaging Server");
		server = null;
		// in the NorthPanel the PortNumber the Start and Stop buttons
		JPanel north = new JPanel();
		north.add(new JLabel("Port number: "));
		tPortNumber = new JTextField("  " + port);
		north.add(tPortNumber);
		// to stop or start the server, we start with "Start"
		stopStart = new JButton("Start");
		stopStart.addActionListener(this);
		north.add(stopStart);
		add(north, BorderLayout.NORTH);

		// the event and chat room
		Box center = Box.createVerticalBox();
		center.add(new JLabel("-------------------- Diagnostic messages from the server --------------------"));
		chat = new JTextAreaBottom(80, 80);
		chat.setEditable(false);
		appendRoom("--- Message log started at " + new Date() + " ---\n");
		center.add(new JScrollPane(chat));

		center.add(Box.createRigidArea(new Dimension(20, 20)));
		center.add(new JLabel("-------------------- Chat Messages ------------------------------------------"));
		event = new JTextAreaBottom(80, 80);
		event.setEditable(false);
		appendEvent("--- Event log started at " + new Date() + " ---\n");
		center.add(new JScrollPane(event));
		add(center, BorderLayout.CENTER);

		// The windows usually ends up on the far left edge of the screen - this rigid area makes it a bit easier to read
		add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.WEST);

		// need to be informed when the user click the close button on the frame
		addWindowListener(this);
		setSize(1200, 900);
		setVisible(true);
	}

	// append message to the two JTextArea
	// position at the end
	void appendRoom(String str) {
		chat.append(str);
		chat.setCaretPosition(chat.getText().length() - 1);
	}

	void appendEvent(String str) {
		event.append(str);
		event.setCaretPosition(event.getText().length() - 1);
	}

	// start or stop where clicked
	public void actionPerformed(ActionEvent e) {
		// if running we have to stop
		if (server != null) {
			server.stop();
			server = null;
			tPortNumber.setEditable(true);
			stopStart.setText("Start");
			return;
		}
		// OK start the server
		int port;
		try {
			port = Integer.parseInt(tPortNumber.getText().trim());
		} catch (Exception er) {
			appendEvent("Invalid port number\n");
			er.printStackTrace();
			return;
		}
		// create a new Server
		server = new LocalMessagingServer(port);
		// and start it as a thread
		new ServerRunning().start();
		appendEvent("Server started at " + (new Date()) + "\n");
		stopStart.setText("Stop");
		tPortNumber.setEditable(false);
	}

	/**
	 * Start listening for messages
	 */
	public void start() {
		actionPerformed(null);
	}

	/**
	 * entry point to start the Server
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		println(MessagingServerGUI.class, "Running from java version " + JavaVersion.getInstance().get());

		prepareUpdateWatcher(true);

		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		MessagingServerGUI ms = new MessagingServerGUI(MESSAGING_SERVER_PORT);
		ms.start();
	}

	/*
	 * If the user click the X button to close the application I need to close the connection with the server to free the port
	 */
	public void windowClosing(WindowEvent e) {
		// if my Server exist
		if (server != null) {
			try {
				server.stop(); // ask the server to close the connection
			} catch (Exception eClose) {
				eClose.printStackTrace();
			}
			server = null;
		}
		// dispose the frame
		dispose();
		System.exit(0);
	}

	// I can ignore the other WindowListener method
	public void windowClosed(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	/**
	 * A thread to run the Server
	 */
	class ServerRunning extends Thread {
		public void run() {
			server.start(); // should stay in this method until it fails
			// the server failed
			stopStart.setText("Start");
			tPortNumber.setEditable(true);
			appendEvent("Server has stopped at " + (new Date()) + "\n");
			server = null;
		}
	}

}
