package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * The Client with its GUI
 * 
 * Taken from http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/ on 5/12/2014
 */
public class MessagingClientGUI extends JFrame implements ActionListener {

	private static final long		serialVersionUID	= 1L;
	// will first hold "Username:", later on "Enter message"
	private JLabel					label;
	// to hold the Username and later on the messages
	private JTextField				tf;
	// to hold the server address an the port number
	private JTextField				tfServer, tfPort;
	// to Logout and get the list of the users
	private JButton					login, logout, whoIsIn;
	// for the chat room
	private JTextArea				ta;
	// if it is for connection
	private boolean					connected;
	// the Client object
	private LocalMessagingClient	client;
	// the default port number
	private int						defaultPort;
	private String					defaultHost;

	private String					defaultUserName;

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
		public void receiveIncomingMessage(final MessageCarrier msg) {
			ta.append(msg.toString() + "\n");
		};

		@Override
		protected void connectionAccepted() {
			MessagingClientGUI.this.connectionAccepted();
			super.connectionAccepted();
		}

		@Override
		protected void connectionFailed() {
			MessagingClientGUI.this.connectionFailed();
			super.connectionFailed();
		}
	}

	// Constructor connection receiving a socket number
	MessagingClientGUI(String host, int port) {
		super("Chat Client");
		defaultPort = port;
		defaultHost = host;

		try {
			defaultUserName = InetAddress.getLocalHost().getCanonicalHostName() + "_listener_" + ((new Date()).getTime() % 100L);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		// The NorthPanel with:
		JPanel northPanel = new JPanel(new GridLayout(3, 1));
		// the server name and the port number
		JPanel serverAndPort = new JPanel(new GridLayout(1, 5, 1, 3));
		// the two JTextField with default value for server address and port number
		tfServer = new JTextField(host);
		tfPort = new JTextField("" + port);
		tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

		serverAndPort.add(new JLabel("Server Address:  "));
		serverAndPort.add(tfServer);
		serverAndPort.add(new JLabel("Port Number:  "));
		serverAndPort.add(tfPort);
		serverAndPort.add(new JLabel(""));
		// adds the Server an port field to the GUI
		northPanel.add(serverAndPort);

		// the Label and the TextField
		label = new JLabel("Enter your username below", SwingConstants.CENTER);
		northPanel.add(label);
		tf = new JTextField(defaultUserName);
		tf.setBackground(Color.WHITE);
		northPanel.add(tf);
		add(northPanel, BorderLayout.NORTH);

		// The CenterPanel which is the chat room
		ta = new JTextArea("Login to test the system\n", 80, 80);
		JPanel centerPanel = new JPanel(new GridLayout(1, 1));
		centerPanel.add(new JScrollPane(ta));
		ta.setEditable(false);
		add(centerPanel, BorderLayout.CENTER);

		// the 3 buttons
		login = new JButton("Login");
		login.addActionListener(this);
		logout = new JButton("Logout");
		logout.addActionListener(this);
		logout.setEnabled(false); // you have to login before being able to logout
		whoIsIn = new JButton("Who is in");
		whoIsIn.addActionListener(this);
		whoIsIn.setEnabled(false); // you have to login before being able to Who is in

		JPanel southPanel = new JPanel();
		southPanel.add(login);
		southPanel.add(logout);
		southPanel.add(whoIsIn);
		add(southPanel, BorderLayout.SOUTH);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 600);
		setVisible(true);
		tf.requestFocus();

	}

	// called by the Client to append text in the TextArea
	void append(String str) {
		ta.append(str);
		ta.setCaretPosition(ta.getText().length() - 1);
	}

	// called by the GUI is the connection failed
	// we reset our buttons, label, textfield
	void connectionFailed() {
		login.setEnabled(true);
		logout.setEnabled(false);
		whoIsIn.setEnabled(false);
		label.setText("Enter your username below");
		tf.setText("Anonymous");
		// reset port number and host name as a construction time
		tfPort.setText("" + defaultPort);
		tfServer.setText(defaultHost);
		// let the user change them
		tfServer.setEditable(false);
		tfPort.setEditable(false);
		// don't react to a <CR> after the username
		tf.removeActionListener(this);
		connected = false;
	}

	/*
	 * Button or JTextField clicked
	 */
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		// if it is the Logout button
		if (o == logout) {
			client.sendMessage(MessageCarrier.getLogout());
			return;
		}
		// if it the who is in button
		if (o == whoIsIn) {
			client.sendMessage(MessageCarrier.getWhoIsIn(client.getName()));
			return;
		}

		// ok it is coming from the JTextField
		if (connected) {
			// just have to send the message and send it to everyone
			client.sendMessage(MessageCarrier.getMessage(client.getName(), "NULL", tf.getText()));
			tf.setText("");
			return;
		}

		if (o == login) {
			// ok it is a connection request
			String username = tf.getText().trim();
			// empty username ignore it
			if (username.length() == 0)
				return;
			// empty serverAddress ignore it
			String server = tfServer.getText().trim();
			if (server.length() == 0)
				return;
			// empty or invalid port number, ignore it
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
			client = new LocalMessagingClient(server, port, username);
			// test if we can start the Client
			if (!client.start())
				return;
			tf.setText("");
			label.setText("Enter your message below");

			// Action listener for when the user enter a message
			tf.addActionListener(this);
		}

	}

	private void connectionAccepted() {
		connected = true;

		// disable login button
		login.setEnabled(false);
		// enable the 2 buttons
		logout.setEnabled(true);
		whoIsIn.setEnabled(true);
		// disable the Server and Port JTextField
		tfServer.setEditable(false);
		tfPort.setEditable(false);
	}

	/**
	 * @param args
	 *            Command line arguments (none expected()
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		String host = "localhost";
		if (args.length > 0 ) 
			host = args[0];

		new MessagingClientGUI(host, MESSAGING_SERVER_PORT);
	}

}
