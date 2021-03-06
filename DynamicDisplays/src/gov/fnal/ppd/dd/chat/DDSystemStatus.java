package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameDisplay;
import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getMessagingServerName;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import gov.fnal.ppd.dd.ChannelSelector;
import gov.fnal.ppd.dd.util.guiUtils.JTextAreaBottom;
import gov.fnal.ppd.dd.util.nonguiUtils.CredentialsNotFoundException;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;

/**
 * <p>
 * A messaging client that shows what is in the messaging system, taylored to the specifics of the Dynamic Displays system
 * </p>
 * 
 * <p>
 * This mechanism was taken from the following web page on 5/12/2014: @see <a
 * href="http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/">dreamincode.net</a>
 * </p>
 */
public class DDSystemStatus extends JFrame {

	private static final long		serialVersionUID	= -1080299322567836987L;

	/**
	 * The French spelling of 'facade', with the 'tail' on the c.
	 */
	public static final String		FACADE				= "FA\u00c7ADE";

	// to hold the server address an the port number
	private JTextField				tfServer, tfPort;
	// to Logout and get the list of the users
	// private JButton login, logout, whoIsIn;
	// for the chat room
	private JTextAreaBottom			ta;

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
	private JScrollPane				scroller;
	private ImageIcon				dataIcon, infoIcon, clockIcon, facadeIcon, selectorIcon;

	private AtomicInteger			nextWhoIsIn			= new AtomicInteger(1);
	private JButton					refreshClients		= new JButton("Refresh in 00" + (nextWhoIsIn.get() + 1));
	private JCheckBox				showUpTime			= new JCheckBox("Times?");
	private JCheckBox				showFacades			= new JCheckBox("Fa" + "\u00c7ades?".toLowerCase());

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
		public void receiveIncomingMessage(final MessageCarrierXML msg) {
			ta.append(msg + "\n");

			// TODO Windows 7 display has a problem with this. Only the first node is ever displayed when this is redrawn.
			// It is getting all the messages and putting them into the tree properly, but the graphics is not showing up
			// (at least on my instance of Windows 7) 6/16/2014

			// This must be a message of type ISALIVE. Try it and see if that assumption is right.
			refresh = 10;
			String clientName = msg.getMessageOriginator();
			if (clientName == null || clientName.length() == 0) // || clientName.equals("NULL"))
				return;

			// String dateString = msg.getMessageValue();
			long dateValue = msg.getTimeStamp();
			if (clientName.contains("FA\u00c7ADE")) {
				if (showFacades.isSelected()) {
					String facadeName = clientName.substring(clientName.indexOf("--") + 3);

					int k = clientsTree.getModel().getChildCount(root);
					boolean found = false;
					for (int i = 0; i < k; i++) {
						DefaultMutableTreeNode q = (DefaultMutableTreeNode) clientsTree.getModel().getChild(root, i);
						if (q.toString().contains(facadeName)) {
							found = true;
							q.add(new DefaultMutableTreeNode(clientName));
							// System.out.println("A: " + clientName + " added to " + q.getUserObject());
							break;
						}
					}
					if (!found) {
						DefaultMutableTreeNode g = new DefaultMutableTreeNode(facadeName);
						root.add(g);
						g.add(new DefaultMutableTreeNode("'" + FACADE.toLowerCase()
								+ "' nodes are virtual connections between a channel changer & a real display"));
						if (showUpTime.isSelected())
							g.add(new DefaultMutableTreeNode("Connected " + dateValue));
						g.add(new DefaultMutableTreeNode(clientName));
						// System.out.println("B: " + clientName + " added to " + g.getUserObject());
					}
				}
			} else { // Not a Facade user
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(clientName);
				root.add(node);
				if (showUpTime.isSelected())
					node.add(new DefaultMutableTreeNode("Connected " + dateValue));
				// System.out.println("C: " + clientName + " added to root");
				if (clientName.contains(")")) {
					int num = Integer.parseInt(clientName.substring(clientName.indexOf('(') + 1, clientName.indexOf(')')));
					String s = "" + num;
					if (num < 10)
						s = "0" + num;
					node.add(new DefaultMutableTreeNode("Dynamic Display ** Number " + s + " ** "));
				} else if (clientName.equals(client.getName()))
					node.add(new DefaultMutableTreeNode("This " + DDSystemStatus.class.getSimpleName() + " Instance"));
				else if (clientName.contains("_listener_"))
					node.add(new DefaultMutableTreeNode("DD System Observer"));
				else
					node.add(new DefaultMutableTreeNode(ChannelSelector.class.getCanonicalName()));
			}
			// Apparently, there is a way to tell the JTree model that the tree has changed. Need to do that!
		};

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

		setTreeIcons();

		JTabbedPane tabs = new JTabbedPane();
		add(tabs, BorderLayout.CENTER);

		refreshClients.setFont(new Font("courier", Font.BOLD, 11));
		refreshClients.setToolTipText("Click to refresh the client list now");

		ActionListener checkBoxListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				nextWhoIsIn.set(0);
			}
		};
		refreshClients.addActionListener(checkBoxListener);

		showUpTime.addActionListener(checkBoxListener);
		showFacades.addActionListener(checkBoxListener);
		showUpTime.setToolTipText("Show the up time as part of the display for a client");
		showFacades.setToolTipText("Show the connections of the channel selectors");
		// With a lot of Displays in the system, having these default to OFF makes for a cleaner, clearer display
		showUpTime.setSelected(false);
		showFacades.setSelected(false);

		JPanel northPanel = new JPanel();
		// the server name and the port number
		Box serverAndPort = Box.createHorizontalBox();

		// the two JTextField with default value for server address and port number
		tfServer = new JTextField(host);
		tfPort = new JTextField("" + port);
		tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

		serverAndPort.add(new JLabel("Server: "));
		serverAndPort.add(Box.createRigidArea(new Dimension(5, 5)));
		serverAndPort.add(tfServer);
		serverAndPort.add(Box.createRigidArea(new Dimension(5, 5)));
		serverAndPort.add(new JLabel("Port: "));
		serverAndPort.add(Box.createRigidArea(new Dimension(5, 5)));
		serverAndPort.add(tfPort);
		serverAndPort.add(Box.createRigidArea(new Dimension(5, 5)));
		serverAndPort.add(refreshClients);
		serverAndPort.add(Box.createRigidArea(new Dimension(5, 5)));
		serverAndPort.add(showUpTime);
		serverAndPort.add(Box.createRigidArea(new Dimension(5, 5)));
		serverAndPort.add(showFacades);

		// adds the Server an port field to the GUI
		northPanel.add(serverAndPort);

		add(northPanel, BorderLayout.NORTH);

		// The CenterPanel which is the chat room
		ta = new JTextAreaBottom("Raw messages from the server\n", 20, 80);
		JPanel messagePanel = new JPanel(new GridLayout(1, 1));
		messagePanel.add(new JScrollPane(ta));
		ta.setEditable(false);

		scroller = new JScrollPane(clientsTree);
		tabs.add(scroller, "Messaging Clients");
		tabs.add(messagePanel, "Raw messages");

		DefaultMutableTreeNode placeHolderNode = new DefaultMutableTreeNode("Awaiting List of Clients");

		root.add(placeHolderNode);

		setTreeIcons();

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 800);
		setVisible(true);

		new Thread("LookForRefresh") {
			public void run() {
				while (true) {
					catchSleep(10L);

					if (refresh-- == 0) {
						// clientsTree.repaint();
						repaint();
						refresh = -1;
						for (int i = 0; i < clientsTree.getRowCount(); i++)
							clientsTree.expandRow(i);

						// for (int k = 0; k < root.getChildCount(); k++) {
						// System.out.println(k + ": " + root.getChildAt(k).toString());
						// }
					}
				}
			}
		}.start();

		new Thread("Login") {
			public void run() {
				catchSleep(1000);
				login();
			}
		}.start();

		new Thread("IssueAWhoIsInCommand") {

			public void run() {
				long sleep = 500;
				while (true) {
					catchSleep(sleep);
					sleep = 1000;

					if (client != null && nextWhoIsIn.decrementAndGet() < 0) {
						whoIsIn();
						nextWhoIsIn.set(599);
					}
					refreshClients.setText("Refresh in " + (nextWhoIsIn.get() < 9 ? "00" : (nextWhoIsIn.get() < 99 ? "0" : ""))
							+ (nextWhoIsIn.get() + 1));
				}
			}
		}.start();
	}

	private class MyRenderer extends DefaultTreeCellRenderer {

		private static final long	serialVersionUID	= 1648726489238080826L;

		public MyRenderer() {
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
				int row, boolean hasFocus) {

			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			if (leaf)
				if (isDisplay(value))
					setIcon(dataIcon);
				else if (isFacade(value))
					setIcon(facadeIcon);
				else if (isTime(value))
					setIcon(clockIcon);
				else if (isSelector(value))
					setIcon(selectorIcon);
				else
					setIcon(infoIcon);
			return this;
		}

		private boolean isDisplay(Object value) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			String nodeInfo = (String) (node.getUserObject());
			return nodeInfo.indexOf("Dynamic Display") >= 0;
		}

		private boolean isFacade(Object value) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			String nodeInfo = (String) (node.getUserObject());
			return nodeInfo.indexOf(FACADE) >= 0;
		}

		protected boolean isTime(Object value) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			String nodeInfo = (String) (node.getUserObject());
			return nodeInfo.indexOf("Connected") >= 0;
		}

		protected boolean isSelector(Object value) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			String nodeInfo = (String) (node.getUserObject());
			return nodeInfo.indexOf("Selector") >= 0;
		}
	}

	private void setTreeIcons() {
		if (dataIcon == null) {
			ImageIcon bigIcon = new ImageIcon("bin/gov/fnal/ppd/dd/images/data-icon.gif");
			Image I = bigIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
			dataIcon = new ImageIcon(I);

			bigIcon = new ImageIcon("bin/gov/fnal/ppd/dd/images/info.jpg");
			I = bigIcon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
			infoIcon = new ImageIcon(I);

			bigIcon = new ImageIcon("bin/gov/fnal/ppd/dd/images/clock.png");
			I = bigIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
			clockIcon = new ImageIcon(I);

			bigIcon = new ImageIcon("bin/gov/fnal/ppd/dd/images/eye.png");
			I = bigIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
			facadeIcon = new ImageIcon(I);

			bigIcon = new ImageIcon("bin/gov/fnal/ppd/dd/images/selector.jpg");
			I = bigIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
			selectorIcon = new ImageIcon(I);
		}
		clientsTree.setCellRenderer(new MyRenderer());
	}

	// called by the Client to append text in the TextArea
	void append(String str) {
		ta.append(str);
		ta.setCaretPosition(ta.getText().length() - 1);
	}

	// called by the GUI is the connection failed
	// we reset our buttons, label, textfield
	void connectionFailed() {
		// reset port number and host name as a construction time
		tfPort.setText("" + defaultPort);
		tfServer.setText(defaultHost);
		// let the user change them
		tfServer.setEditable(false);
		tfPort.setEditable(false);

	}

	private void whoIsIn() {
		if (client == null) {
			System.err.println(this.getClass().getSimpleName() + ".whoIsIn(): Messaging client is null!");
			return;
		}

		// TODO Only redraw the tree if something changes.
		root = new DefaultMutableTreeNode("Dynamic Displays Messaging System, " + new Date());
		clientsTree = new JTree(root);
		setTreeIcons();

		// FIXME -- Maybe there is a way to generate a new tree when these WHOISIN messages come in and then see if the two trees
		// are equal().

		client.sendMessage(MessageCarrierXML.getWhoIsIn("me", getMessagingServerNameDisplay()));
		ta.append("\n------ " + new Date() + " ------\n");

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				scroller.setViewportView(clientsTree);
				repaint();
			}
		});
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
			en.printStackTrace();
			return; // nothing I can do if port number is not valid
		}

		// try creating a new Client with GUI
		client = new LocalMessagingClient(server, port, defaultUserName);
		// test if we can start the Client
		if (!client.start()) {
			append("Login failed for some reason!\n");
		} else {
			append("Logged in as " + defaultUserName + "\n");
		}

	}

	private void connectionAccepted() {
		tfServer.setEditable(false);
		tfPort.setEditable(false);
	}

	/**
	 * @param args
	 *            Command line arguments (none expected())
	 */
	public static void main(String[] args) {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		getMessagingServerNameDisplay();
		
		int portNumber = MESSAGING_SERVER_PORT;
		String host = getMessagingServerName();
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
