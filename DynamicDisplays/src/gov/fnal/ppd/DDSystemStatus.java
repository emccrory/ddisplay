package gov.fnal.ppd;

import gov.fnal.ppd.chat.MessageCarrier;
import gov.fnal.ppd.chat.MessagingClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * A messaging client that shows what is in the messaging system, taylored to the specifics of the Dynamic Displays system
 * 
 * Taken from http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/ on 5/12/2014
 */
public class DDSystemStatus extends JFrame {

	private static final long		serialVersionUID	= -1080299322567836987L;

	// to hold the server address an the port number
	private JTextField				tfServer, tfPort;
	// to Logout and get the list of the users
	// private JButton login, logout, whoIsIn;
	// for the chat room
	private JTextArea				ta;

	// the Client object
	private LocalMessagingClient	client;
	// the default port number
	private int						defaultPort;
	private String					defaultHost;

	private String					defaultUserName;
	private DefaultMutableTreeNode	root				= new DefaultMutableTreeNode(
																"Dynamic Displays Messaging System (initialized at " + new Date()
																		+ ")");

	private JTree					clientsTree			= new JTree(root);

	private int						refresh				= 0;

	private int						nextWhoIsIn			= 4;

	// TODO It is probably more correct to have the GUI class extend MessagingClient and then have JFrame be an attribute.
	// It is the other way around now.

	private class LocalMessagingClient extends MessagingClient {

		public LocalMessagingClient(String server, int port, String username) {
			super(server, port, username);
		}

		@Override
		public void displayLogMessage(final String msg) {
			if (msg.endsWith("\n"))
				ta.append(msg);
			else
				ta.append(msg + "\n");
		}

		@Override
		public void displayIncomingMessage(final String msg) {
			if (msg.endsWith("\n"))
				ta.append(msg);
			else
				ta.append(msg + "\n");

			if (msg.contains("WHOISIN")) {
				refresh = 10;
				String clientName = msg.substring("WHOISIN [".length(), msg.indexOf(']'));
				if (msg.contains("FA\u00c7ADE")) {
					String facadeName = msg.substring(msg.indexOf("--") + 3, msg.indexOf(']'));

					int k = clientsTree.getModel().getChildCount(root);
					boolean found = false;
					for (int i = 0; i < k; i++) {
						DefaultMutableTreeNode q = (DefaultMutableTreeNode) clientsTree.getModel().getChild(root, i);
						if (q.toString().contains(facadeName)) {
							found = true;
							q.add(new DefaultMutableTreeNode(clientName));
							break;
						}
					}
					if (!found) {
						DefaultMutableTreeNode g = new DefaultMutableTreeNode(facadeName);
						root.add(g);
						g.add(new DefaultMutableTreeNode(
								"'FA\u00c7ADE' nodes are virtual connections between a channel changer & a real display"));
						g.add(new DefaultMutableTreeNode("Connected at " + msg.substring(msg.indexOf("since") + 6)));
						g.add(new DefaultMutableTreeNode(clientName));
					}
				} else {
					DefaultMutableTreeNode node = new DefaultMutableTreeNode(clientName);
					root.add(node);
					node.add(new DefaultMutableTreeNode("Connected at " + msg.substring(msg.indexOf("since") + 6)));
					if (msg.contains(")"))
						node.add(new DefaultMutableTreeNode("Dynamic Display Number "
								+ msg.substring(msg.indexOf('(') + 1, msg.indexOf(')'))));
					else if (msg.contains("_listener_"))
						node.add(new DefaultMutableTreeNode("This is an instance of " + DDSystemStatus.class.getCanonicalName()));
					else
						node.add(new DefaultMutableTreeNode("This is an instance of " + ChannelSelector.class.getCanonicalName()));
				}

			}
		};

		//
		// TODO Use JTree to show the clients that are connected to the messaging server
		//

		@Override
		protected void connectionAccepted() {
			DDSystemStatus.this.connectionAccepted();
			super.connectionAccepted();
		}

		@Override
		protected void connectionFailed() {
			DDSystemStatus.this.connectionFailed();
			super.connectionFailed();
		}
	}

	// Constructor connection receiving a socket number
	DDSystemStatus(String host, int port) {
		super("DD System Peeker");
		defaultPort = port;
		defaultHost = host;

		try {
			defaultUserName = InetAddress.getLocalHost().getCanonicalHostName() + "_listener_" + ((new Date()).getTime() % 100L);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		JTabbedPane tabs = new JTabbedPane();
		add(tabs, BorderLayout.CENTER);

		JPanel northPanel = new JPanel();
		// the server name and the port number
		JPanel serverAndPort = new JPanel(new GridLayout(1, 4));

		// the two JTextField with default value for server address and port number
		tfServer = new JTextField(host);
		tfPort = new JTextField("" + port);
		tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

		serverAndPort.add(new JLabel("Server Address:  "));
		serverAndPort.add(tfServer);
		serverAndPort.add(new JLabel("Port Number:  "));
		serverAndPort.add(tfPort);
		// adds the Server an port field to the GUI
		northPanel.add(serverAndPort);

		add(northPanel, BorderLayout.NORTH);

		// The CenterPanel which is the chat room
		ta = new JTextArea("Raw messages from the server\n", 20, 80);
		JPanel messagePanel = new JPanel(new GridLayout(1, 1));
		messagePanel.add(new JScrollPane(ta));
		ta.setEditable(false);

		tabs.add(new JScrollPane(clientsTree), "Messaging Clients");
		tabs.add(messagePanel, "Raw messages");

		DefaultMutableTreeNode placeHolderNode = new DefaultMutableTreeNode("Awaiting List of Clients");

		root.add(placeHolderNode);

		// the 3 buttons
		// login = new JButton("Login");
		// login.addActionListener(this);
		// logout = new JButton("Logout");
		// logout.addActionListener(this);
		// logout.setEnabled(false); // you have to login before being able to logout
		// whoIsIn = new JButton("Who is in");
		// whoIsIn.addActionListener(this);
		// whoIsIn.setEnabled(false); // you have to login before being able to Who is in
		//
		// JPanel southPanel = new JPanel();
		// southPanel.add(login);
		// southPanel.add(logout);
		// southPanel.add(whoIsIn);
		// add(southPanel, BorderLayout.SOUTH);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 600);
		setVisible(true);

		new Thread("LookForRefresh") {
			public void run() {
				while (true) {
					try {
						sleep(10L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (refresh-- == 0) {
						// clientsTree.repaint();
						repaint();
						refresh = -1;
					}
				}
			}
		}.start();

		new Thread("Login") {
			public void run() {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				login();
			}
		}.start();

		new Thread("IssueAWhoIsInCommand") {
			public void run() {
				while (true) {
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (client != null && nextWhoIsIn-- == 0) {
						whoIsIn();
						nextWhoIsIn = 10;
					}
				}
			}
		}.start();
	}

	// called by the Client to append text in the TextArea
	void append(String str) {
		ta.append(str);
		ta.setCaretPosition(ta.getText().length() - 1);
	}

	// called by the GUI is the connection failed
	// we reset our buttons, label, textfield
	void connectionFailed() {
		// login.setEnabled(true);
		// logout.setEnabled(false);
		// whoIsIn.setEnabled(false);
		// reset port number and host name as a construction time
		tfPort.setText("" + defaultPort);
		tfServer.setText(defaultHost);
		// let the user change them
		tfServer.setEditable(false);
		tfPort.setEditable(false);

	}

	private void logout() {
		client.sendMessage(new MessageCarrier(MessageCarrier.LOGOUT, ""));
	}

	private void whoIsIn() {
		if (client == null)
			return;
		client.sendMessage(new MessageCarrier(MessageCarrier.WHOISIN, ""));
		root.removeAllChildren();
		root.setUserObject("Dynamic Displays Messaging System, " + new Date());
		nextWhoIsIn = 10;
	}

	private void login() {
		// a connection request

		String server = tfServer.getText().trim();
		if (server.length() == 0)
			return;
		// empty or invalid port numer, ignore it
		String portNumber = tfPort.getText().trim();
		if (portNumber.length() == 0)
			return;
		int port = 0;
		try {
			port = Integer.parseInt(portNumber);
		} catch (Exception en) {
			return; // nothing I can do if port number is not valid
		}

		// try creating a new Client with GUI
		client = new LocalMessagingClient(server, port, defaultUserName);
		// test if we can start the Client
		if (!client.start()) {
			append("Login failed for some reason!\n");
		} else {
			append("Login as " + defaultUserName + "\n");
		}

	}

	private void connectionAccepted() {
		tfServer.setEditable(false);
		tfPort.setEditable(false);
	}

	/**
	 * @param args
	 *            Command line arguments (none expected()
	 */
	public static void main(String[] args) {
		int portNumber = 1500;
		String host = "mccrory.fnal.gov";
		if (args.length == 1) {
			try {
				portNumber = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("Invalid port number.");
				System.out.println("Usage is: > java " + DDSystemStatus.class.getCanonicalName() + " [portNumber]");
				System.exit(-1);
			}
		} else if (args.length == 2) {
			try {
				host = args[0];
				portNumber = Integer.parseInt(args[1]);
			} catch (Exception e) {
				System.out.println("Invalid port number.");
				System.out.println("Usage is: > java " + DDSystemStatus.class.getCanonicalName() + " [hostName] [portNumber]");
				System.exit(-1);
			}
		}
		new DDSystemStatus(host, portNumber);
	}

}
