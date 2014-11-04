package gov.fnal.ppd.chat;

import static gov.fnal.ppd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.signage.util.Util.launchMemoryWatcher;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The server that can be run both as a console application or a GUI
 * 
 * Taken from http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/ on 5/12/2014
 */
public class MessagingServer {

	int	numConnectionsSeen	= 0;

	private class ClientThreadList extends CopyOnWriteArrayList<ClientThread> {

		private static final boolean	REQUIRE_UNIQUE_USERNAMES	= true;

		private static final long		serialVersionUID			= 2919140620801861217L;

		@Override
		public boolean add(ClientThread ct) {
			if (ct == null)
				return false;
			if (REQUIRE_UNIQUE_USERNAMES) {
				int index = 0;
				for (ClientThread CT : this) {
					if (CT == null || CT.username == null) {
						display("Unexpected null ClientThread at index=" + index + ": [" + CT + "]");
						remove(CT); // How did THIS happen??
					} else if (CT.username.equals(ct.username))
						return false; // Duplicate username is not allowed!
					index++;
				}
			}
			// QUESTION -- Is a bad thing to have duplicate usernames? When/if the server directs messages to the intended user,
			// this WILL be necessary. But having a fairly anonymous clientelle works just fine.
			//
			// BUt I am seeing bugs whereby one client restarts and the server has not dropped the old connection.
			numConnectionsSeen++;
			return super.add(ct);
		}

	}

	// a unique ID for each connection
	private static int			uniqueId;
	// an ArrayList to keep the list of the Client
	// private ArrayList<ClientThread> al; This Template is supposed to help with ConcurrentModificationException
	private ClientThreadList	al;

	// to display time (Also used as a synchronization object for writing messages to the local terminal/GUI
	protected SimpleDateFormat	sdf;
	// the port number to listen for connection
	private int					port;
	// the boolean that will be turned of to stop the server
	private boolean				keepGoing;

	private Object				broadcasting			= "For synchronization";
	private Thread				showClientList			= null;

	protected int				totalMesssagesHandled	= 0;
	private long				startTime				= System.currentTimeMillis();
	private long				tooOldTime				= 4L * FIFTEEN_MINUTES;		// One hour

	/**
	 * server constructor that receive the port to listen to for connection as parameter in console
	 * 
	 * @param port
	 */
	public MessagingServer(int port) {
		// the port
		this.port = port;
		// to display hh:mm:ss
		sdf = new SimpleDateFormat("dd MMM HH:mm:ss");
		// ArrayList for the Client list
		al = new ClientThreadList();
		launchMemoryWatcher();
	}

	/**
	 * Start the messaging server
	 */
	public void start() {
		startPinger();
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try {
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);

			// infinite loop to wait for connections
			while (keepGoing) {
				// format message saying we are waiting
				display("Waiting for Clients on port " + port + ".");

				Socket socket = serverSocket.accept(); // accept connection if I was asked to stop
				// if (!keepGoing)
				// break;
				ClientThread t = new ClientThread(socket); // make a thread of it
				if (al.add(t)) // save it in the ArrayList
					t.start();
				else
					display("Error! Duplicate username requested, '" + t.username + "'");
				if (showClientList == null) {

					// This block is to print out the current set of clients. Rather than printing it out every time
					// a client connects, we wait 3 seconds from the first connect to do this. Usually, a lot of clients
					// connect at the same time--either the Channel Selector or when all the Displays reboot.

					showClientList = new Thread("ShowClientList") {
						long	WAIT_FOR_ALL_TO_CONNECT_TIME	= 3000;

						public void run() {
							try {
								sleep(WAIT_FOR_ALL_TO_CONNECT_TIME);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							String m = "List of connected clients:\n";
							for (ClientThread CT : al) {
								m += "                     " + CT.username + " at " + CT.date + " ID=" + CT.id + "\n";
							}
							display(m);
							showDiagnostics();
							showClientList = null;
						}
					};
					showClientList.start();
				}

			}
			// I was asked to stop
			display("Closing the server port");
			try {
				serverSocket.close();
				for (int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException ioE) {
						// not much I can do
						ioE.printStackTrace();
					}
				}
			} catch (Exception e) {
				display("Exception closing the server and clients: " + e);
				e.printStackTrace();
			}
		}
		// something went wrong
		catch (IOException e) {
			String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
			e.printStackTrace();
		} catch (Exception e) {
			String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
			e.printStackTrace();
		}
	}

	private void startPinger() {

		new Thread("Pinger") {
			public long	sleepPeriod	= 1000L;

			public void run() {
				try {
					sleep(sleepPeriod);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				while (keepGoing) {
					try {
						sleep(sleepPeriod);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// TODO Implement PING/PONG protocol
					// This protocol is not done very well. There are some places where it seems to just read the
					// message as a string and some places that it looks at the message as a real Message object. This needs
					// to be made self-consistent!

					// TODO -- Figure out a way for the server to send "Are You Alive" messages to each client.
					if (false)
						broadcast(MessageCarrier.getIsAlive("NULL", "NULL"));

					sleepPeriod = (sleepPeriod > FIFTEEN_MINUTES ? FIFTEEN_MINUTES : sleepPeriod * 2);

					showDiagnostics();
				}
			}
		}.start();
	}

	protected void showDiagnostics() {
		// check for oldest client (lastSeen)
		long oldest = System.currentTimeMillis();
		String oldestName = "";
		List<String> unSeen = new ArrayList<String>();
		List<String> tooOld = new ArrayList<String>();
		for (ClientThread CT : al) {
			if (CT.getLastSeen() == 0) {
				unSeen.add(CT.username);
			} else {
				if (CT.getLastSeen() < oldest) {
					oldest = CT.getLastSeen();
					oldestName = CT.username;
				}
				if ((System.currentTimeMillis() - CT.getLastSeen()) > tooOldTime) {
					tooOld.add(CT.username);
				}
			}
		}
		long longAliveTime = System.currentTimeMillis() - startTime;
		long hours = longAliveTime / (3600L * 1000L);
		long days = hours / 24L;
		hours = hours % 24L;
		long minutes = (longAliveTime % (36000L * 1000L)) / 60000L;
		long seconds = (longAliveTime % (60L * 1000L)) / 1000L;

		display(MessagingServer.class.getSimpleName() + " has been alive " + days + " days, " + hours + " hrs " + minutes + " min "
				+ seconds + " sec -- \n " + "                       " + numConnectionsSeen + " connections accepted, " + al.size()
				+ " client" + (al.size() != 1 ? "s" : "") + " connected right now, " + totalMesssagesHandled
				+ " messages handled\n" + "                       " + "Oldest client is [" + oldestName + "], last seen "
				+ (System.currentTimeMillis() - oldest) + " msec ago (although it could be a tie)\n" + "                       "
				+ "There are " + unSeen.size() + " clients never heard from at this time.\n" + "                       "
				+ "Clients that are too old: " + tooOld.size());
		if (tooOld.size() > 0) {
			String m = MessagingServer.class.getSimpleName() + ": List of clients that are too old\n";
			for (String S : tooOld)
				m += "                       " + S + "\n";
			display(m);
		}
	}

	/*
	 * For the GUI to stop the server
	 */
	protected void stop() {
		keepGoing = false;
		// connect to myself as Client to exit statement
		// Socket socket = serverSocket.accept();
		try {
			new Socket("localhost", port);
		} catch (Exception e) {
			// nothing I can really do
			e.printStackTrace();
		}
	}

	/**
	 * Display an event (not a message) to the console or the GUI
	 */
	protected void display(String msg) {
		synchronized (sdf) {
			String time = sdf.format(new Date()) + " " + msg;
			System.out.println(time);
		}
	}

	/**
	 * to broadcast a message to all Clients
	 * 
	 * Overrideable if the base class wants to know about the messages.
	 * 
	 * Be sure to call super.broadcast(message) to actually get the message broadcast, though!
	 * 
	 * @param message
	 *            -- The message to broadcast
	 */
	protected void broadcast(MessageCarrier message) {
		synchronized (broadcasting) {
			// we loop in reverse order in case we would have to remove a Client
			// because it has disconnected
			for (int i = al.size(); --i >= 0;) {
				ClientThread ct = al.get(i);
				// try to write to the Client if it fails remove it from the list
				if (message.getTo() == null || message.getTo().equals("NULL") || message.getTo().equals(ct.username))
					if (!ct.writeMsg(message)) {
						al.remove(i);
						display("Disconnected Client " + ct.username + " removed from list.");
					}
			}
		}
	}

	/**
	 * To run as a console application just open a console window and: > java Server > java Server portNumber If the port number is
	 * not specified 1500 is used
	 * 
	 * @param args
	 *            -- The command line arguments. [0] is the port number, otherwise 1500 is assumed
	 */
	public static void main(String[] args) {
		// start server on port 1500 unless a PortNumber is specified
		int portNumber = MESSAGING_SERVER_PORT;
		switch (args.length) {
		case 1:
			try {
				portNumber = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("Invalid port number.");
				System.out.println("Usage is: > java Server [portNumber]");
				return;
			}
		case 0:
			break;
		default:
			System.out.println("Usage is: > java Server [portNumber]");
			return;

		}
		// create a server object and start it
		MessagingServer server = new MessagingServer(portNumber);
		server.start();
	}

	/**
	 * One instance of this thread will run for each client. It handles all the messages from that client to the server (that's me!)
	 */
	class ClientThread extends Thread {
		// the socket where to listen/talk
		Socket				socket;
		ObjectInputStream	sInput;
		ObjectOutputStream	sOutput;
		// my unique id (easier for deconnection)
		int					id;
		// the Username of the Client
		String				username;
		// the only type of message we will receive
		MessageCarrier		cm;
		// the date I connect
		String				date;
		private boolean		thisSocketIsActive	= false;
		private long		lastSeen			= System.currentTimeMillis();

		// Constructor
		ClientThread(Socket socket) {
			super("ClientThread_of_MessagingServer_" + uniqueId);
			// a unique id
			id = uniqueId++;
			this.socket = socket;
			Object read = null;
			/* Creating both Data Stream */
			// System.out.println("Thread trying to create Object Input/Output Streams");
			try {
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());

				// TODO It seems that the only certain way to assure that the client at the other end of this read is
				// still alive is to establish some sort of "Still Alive" protocol between client and server. It is
				// possible for the client to die in such a way as to NOT close the socket. The socket is closed
				// when the client exits, and sometimes it seems to close when the client dies--but not always.
				// This may have been addressed in the method startPinger().

				// read the username -- the first message from the new connection
				read = sInput.readObject();
				if (read instanceof MessageCarrier) {
					username = ((MessageCarrier) read).getMessage();
				} else if (read instanceof String) {
					username = (String) read;
				}
				display("'" + username + "' (" + id + ") has connected.");
			} catch (IOException e) {
				display("Exception creating new Input/output streams on socket (" + socket + ") due to this exception: " + e);
				return;
			} catch (Exception e) {
				if (read == null) {
					display("Expecting a String but got nothing (null)");
				} else
					display("Expecting a String but got a " + read.getClass().getSimpleName() + " [" + read + "]");
				e.printStackTrace();
			}
			date = new Date().toString();
		}

		public long getLastSeen() {
			return lastSeen;
		}

		// This method is what will run forever

		public void run() {
			// to loop until LOGOUT or we hit an unrecoverable exception
			Object read = new Object();
			// Bug fix for input and output objects: call "reset" after every read and write to not let these objects
			// remember their state. After a few hours, we run out of memory!
			thisSocketIsActive = true;
			while (thisSocketIsActive) {
				// read a String (which is an object)
				try {
					read = sInput.readObject();
					cm = (MessageCarrier) read;
				} catch (ClassNotFoundException e) {
					display(username + ": A class not found exception -- " + e + ". returned object of type "
							+ read.getClass().getCanonicalName());
					e.printStackTrace();
					break; // End the while(thisSocketIsActive) loop
				} catch (Exception e) {
					display(username + ": Exception reading input stream -- " + e + "; The received message was '" + read
							+ "' (type=" + read.getClass().getCanonicalName() + ")");
					System.err.println(username + ": Exception reading input stream -- " + e + "; The received message was '"
							+ read + "' (type=" + read.getClass().getCanonicalName() + ")"); // Put this here to assure that the
					// stack-trace and this message are together
					// in the console (debugging)
					e.printStackTrace();
					break; // End the while(thisSocketIsActive) loop
				}

				// The client that sent this message is still alive
				String from = cm.getFrom();
				for (ClientThread CT : al) {
					if (CT.username.equals(from))
						CT.lastSeen = System.currentTimeMillis();
				}

				totalMesssagesHandled++;
				// Switch for the type of message receive
				switch (cm.getType()) {

				case MESSAGE:
				case ISALIVE:
				case AMALIVE:
					//
					// The message, received from a client, is relayed here to the client of its choosing
					//
					broadcast(cm);
					break;

				// ---------- Other types of messages are interpreted here by the server. ---------
				case LOGOUT:
					display(username + " disconnected with a LOGOUT message.");
					thisSocketIsActive = false;
					break;

				case WHOISIN:
					// writeMsg("WHOISIN List of the users connected at " + sdf.format(new Date()) + "\n");
					// scan all the users connected
					boolean purge = false;
					for (int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						if (ct != null && ct.username != null && ct.date != null)
							writeMsg(MessageCarrier.getIAmAlive(ct.username, cm.getFrom(), "since " + ct.date));
						else {
							display("Talking to " + username + " socket " + socket.getLocalAddress() + " (" + (i + 1)
									+ ") Error!  Have a null client");
							purge = true;
						}
					}
					if (purge)
						for (ClientThread AL : al) {
							if (AL == null) {
								display("Removing null ClientThread");
								al.remove(AL);
							} else if (AL.username == null) {
								display("Removing ClientThread with null username [" + AL + "]");
								al.remove(AL);
								AL.thisSocketIsActive = false;
							} else if (AL.date == null) {
								display("Removing ClientThread with null date [" + AL + "]");
								al.remove(AL);
								AL.thisSocketIsActive = false;
							}
						}
					break;
				}
			}
			// remove myself from the arrayList containing the list of the connected Clients
			display("Exiting forever loop for client '" + username + "' (thisSocketIsActive=" + thisSocketIsActive
					+ ") Removing id=" + id + ")");

			synchronized (al) {
				// scan the array list until we found the Id
				for (int i = 0; i < al.size(); ++i) {
					ClientThread ct = al.get(i);
					// found it
					if (ct.id == id) {
						al.remove(i);
						// Do not return; I want to see the information message at the end.
						break;
					}
				}
			}

			System.out.println(this.getClass().getSimpleName() + ": Number of remaining clients: " + al.size());
			close();
		}

		// try to close everything
		protected void close() {
			try {
				if (sOutput != null)
					sOutput.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (sInput != null)
					sInput.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (socket != null)
					socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			sInput = null;
			sOutput = null;
			socket = null;
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(MessageCarrier msg) {
			// if Client is still connected send the message to it
			if (!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
				sOutput.reset();
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				display("IOException sending message to " + username);
				display(e.toString());
				e.printStackTrace();
			} catch (Exception e) {
				display(e.getLocalizedMessage() + ", Error sending message to " + username);
				e.printStackTrace();
			}
			return true;
		}

		@Override
		public String toString() {
			return username + ", id=" + id + ", socket=[" + socket + "], date=" + date;
		}
	}

}
