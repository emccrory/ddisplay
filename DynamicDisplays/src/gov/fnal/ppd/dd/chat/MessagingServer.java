package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_DAY;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.dd.GlobalVariables.checkSignedMessages;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.launchMemoryWatcher;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SignedObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The server that can be run both as a console application or a GUI
 * 
 * TODO: Improve the architecture
 * 
 * <p>
 * According to http://docs.oracle.com/cd/E19957-01/816-6024-10/apa.htm (the first hit on Google for
 * "Messaging Server Architecture"), a well-built messaging server has these six components:
 * <ul>
 * <li>The dispatcher is the daemon (or service) component of the Messaging Server. It coordinates the activities of all other
 * modules. In Figure A.1, the dispatcher can be thought of as an envelope that contains and initiates the processes of all items
 * shown within the largest box.</li>
 * <li>The module configuration database contains general configuration information for the other modules.</li>
 * <li>The message transfer agent (MTA) handles all message accepting, routing, and delivery tasks.</li>
 * <li>The Messaging Server managers facilitate remote configuration and operation of the system and ensure that the modules are
 * doing their job and following the rules.</li>
 * <li>The AutoReply utility lets the Messaging Server automatically reply to incoming messages with predefined responses of your
 * choice.</li>
 * <li>The directory database--either a local directory database or a Directory Server--contains user and group account information
 * for the other modules.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * In this application of the idea, I have pretty much a stove-pipe server with only one bit of a protocol, handled elsewhere. The
 * message is an object (see MessageCarrier.java) with four attributes:
 * <ul>
 * <li>MessageType type; -- An enum of the sorts of messages we expect</li>
 * <li>String message; -- The message body (if it is required). in principle, this can be encrypted (and then ASCII-fied). In this
 * architecture, when the message is used, it is usually an XML document</li>
 * <li>String to; -- The name of the client who is supposed to get this message</li>
 * <li>String from; -- The name of the client who sent the message (allowing a reply of some sort)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * It may be possible to use ObjectOutputStream and ObjectInputStream to send the EncodedCarrier (XML) object directly, without the
 * existing XML marshalling and unmarshalling. But since this works, I have little motivation to make this change. Future developers
 * may be more enthusiastic to do this. The existing scheme makes it easy to add on encryption of the message (which is the XML
 * document), which would allow the routing of the message without decrypting.
 * </p>
 * 
 * <p>
 * What about encryption? If we ever need to secure this communication, it should be possible to encrypt parts of the message. A
 * simple way to implement this would be to add a time stamp to MessageCarrier which is encrypted and re-ASCII-fied. Using an
 * asymmetric encryption, the source (the ChannelSelector) can encrypt it with its secret key and then the destination (the Display)
 * can decrypt it with the public key and then verify that the date is "recent". Or one can encrypt the XML document itself (which
 * includes a time stamp). This will probably not be necessary at Fermilab as it is hard to imagine a situation where someone would
 * put bad stuff into the stream of messages and foul things up. But hackers are a clever bunch, so never say never.
 * </p>
 * <p>
 * An operational observation: A Client often drop out without saying goodbye. Almost always when this happens, the socket
 * connection between the server and the client gets corrupted and the server notices. When it does notice, it can delete this
 * client from its inventory. But sometimes this corruption does not happen and the socket somehow remains viable from the server's
 * perspective. In this situation, when the Client tries to reconnect it is turned away as its name is already in use. Thus, I have
 * implemented a heart beat from the server to each client to see if this client really is alive. See startPinger() method for
 * details.
 * </p>
 * 
 * *
 * <p>
 * Taken from the internet on 5/12/2014. @see <a
 * href="http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/" dreamincode.net</a>
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 */
public class MessagingServer {

	private boolean	showAliveMessages	= false;

	/*************************************************************************************************************************
	 * Handle the communications with a specific client in the messaging system. One instance of this thread will run for each
	 * client.
	 * 
	 * This class the surrounding MessagingServer class by calling display() and by manipulating the list, al.
	 * 
	 * Style note: Using the syntax, "this.attribute" within this inner class to mark the stuff owned by this class. I also tried
	 * marking the surrounding calls with "MessagingServer.this.", but this was just too much clutter (IMHO).
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * @copyright 2014
	 */
	private class ClientThread extends Thread {

		// the only types of message we will deal with here
		MessageCarrier		cm;
		SignedObject		cmSigned;

		// the date I connected
		String				date;

		// my unique id (easier for deconnection)
		int					id;

		// When was this client last seen?
		long				lastSeen			= System.currentTimeMillis();

		// If this client has been absent for too long, we put it "on notice"
		boolean				onNotice			= false;

		// the input stream for our conversation
		ObjectInputStream	sInput;
		// the output stream for our conversation
		ObjectOutputStream	sOutput;
		// the socket where to listen/talk
		Socket				socket;

		// Does it seem like the socket for this client is active?
		boolean				thisSocketIsActive	= false;

		// the Username of the Client
		String				username;

		// Constructor
		ClientThread(Socket socket) {
			super("ClientThread_of_MessagingServer_" + MessagingServer.uniqueId);
			// a unique id
			this.id = MessagingServer.uniqueId++;
			this.socket = socket;
			Object read = null;
			/* Creating both Data Stream */
			// System.out.println("Thread trying to create Object Input/Output Streams");
			try {
				// create output first
				this.sOutput = new ObjectOutputStream(socket.getOutputStream());
				this.sInput = new ObjectInputStream(socket.getInputStream());

				// read the username -- the first message from the new connection should be a login message
				read = this.sInput.readObject();
				if (read instanceof MessageCarrier) {
					if (((MessageCarrier) read).getType() != MessageType.LOGIN) {
						error("Expected LOGIN message from " + ((MessageCarrier) read).getMessage() + ", but got a "
								+ ((MessageCarrier) read).getType() + " message");
					}
					this.username = ((MessageCarrier) read).getMessage();
					display("'" + this.username + "' (" + id + ") has connected.");
				} else {
					display("Unexpected response from client '" + ((MessageCarrier) read).getFrom() + ": Type="
							+ read.getClass().getCanonicalName());
					return;
				}
			} catch (IOException e) {
				display("Exception creating new Input/output streams on socket (" + socket + ") due to this exception: " + e);
				return;
			} catch (Exception e) {
				if (read == null) {
					display("Expecting a String but got nothing (null)");
				} else
					display("Expecting a String but got a " + read.getClass().getSimpleName() + " [" + read + "]");
				printStackTrace(e);
			}
			this.date = new Date().toString();
		}

		// try to close everything
		protected void close() {
			try {
				if (this.sOutput != null)
					this.sOutput.close();
			} catch (Exception e) {
				printStackTrace(e);
			}
			try {
				if (this.sInput != null)
					this.sInput.close();
			} catch (Exception e) {
				printStackTrace(e);
			}
			try {
				if (this.socket != null)
					this.socket.close();
			} catch (Exception e) {
				printStackTrace(e);
			}
			this.sInput = null;
			this.sOutput = null;
			this.socket = null;
		}

		public long getLastSeen() {
			return this.lastSeen;
		}

		public void setLastSeen() {
			this.lastSeen = System.currentTimeMillis();
			setOnNotice(false);
		}

		// This method is what will run forever
		public void run() {
			// to loop until LOGOUT or we hit an unrecoverable exception
			// Bug fix for input and output objects: call "reset" after every read and write to not let these objects
			// remember their state. After a few hours, we run out of memory!
			this.thisSocketIsActive = true;
			while (this.thisSocketIsActive) {
				// Create this Object here so it can be seen by the catch blocks
				Object read = new Object();
				try {
					read = this.sInput.readObject();
					if (read instanceof MessageCarrier) {
						this.cm = (MessageCarrier) read;
						this.cmSigned = null;
					} else if (read instanceof SignedObject) {
						this.cmSigned = (SignedObject) read;
						this.cm = (MessageCarrier) cmSigned.getObject();
						String signatureString = this.cm.verifySignedObject(cmSigned);
						if (signatureString == null)
							event("Message is properly signed: " + this.cm);
						else {
							event("Message is NOT PROPERLY SIGNED: [" + this.cm + "]; reason = '" + signatureString
									+ "' -- ignoring this message.");
							continue;
						}
					} else if (read instanceof String) {
						display(this.username + ": Received message of type String, '" + read + "'");
						if ("NULL".equalsIgnoreCase((String) read))
							continue;
						display("\t\t -- This may be an old client trying to connect.");
						cm = MessageCarrier.getIAmAlive((String) read, "NULL", new Date().toString());
					} else if (read instanceof MessageType) {
						display(this.username + ": Received message of type MessageType, value='" + read + "'");
						continue;
					} else {
						display(this.username + ": A class not found exception -- " + read + ". returned object of type "
								+ read.getClass().getCanonicalName());
						new ClassNotFoundException().printStackTrace();
						break; // End the while(this.thisSocketIsActive) loop
					}
				} catch (Exception e) {
					String dis = "Client " + this.username + ": Exception reading input stream -- " + e + ";\n          "
							+ "The received message was '" + read + "'\n          (type=" + read.getClass().getCanonicalName()
							+ ")";
					e.printStackTrace();
					StackTraceElement[] trace = e.getStackTrace();
					dis += "\n" + trace[trace.length - 1]; // Print only the last line (reducing crap in the printout)

					display(dis);
					error(dis);

					break; // End the while(this.thisSocketIsActive) loop

					/*
					 * There is something odd going on here. Nov. 12, 2014 (continues on Dec. 19, 2014)
					 * 
					 * It seems that every time a lot of clients try to connect at once, for example when a new instance of the
					 * ChannelSelector comes up, this causes all of the existing connections to read an "EOF" here (sometimes worse,
					 * but it seems to mostly be EOF). Everything recovers (as it is designed to do), but this is a bug waiting to
					 * bite us harder. Some say that EOF is ignorable, but others say "No way!" For example:
					 * 
					 * http://stackoverflow.com/questions/12684072/eofexception-when-reading-files-with-objectinputstream
					 */
				}

				// The client that sent this message is still alive
				markClientAsSeen(this.cm.getFrom());

				if (this.cm.getType() == MessageType.AMALIVE && SPECIAL_SERVER_MESSAGE_USERNAME.equals(this.cm.getTo())
						&& showAliveMessages) {
					display("Got 'I'm Alive' message from " + cm.getFrom().trim());
					continue; // That's all for this loop iteration.
				}

				totalMesssagesHandled++;
				// Switch for the type of message receive
				switch (this.cm.getType()) {

				case ISALIVE:
				case AMALIVE:
					broadcast(this.cm);
					break;

				case MESSAGE:
					//
					// The message, received from a client, is relayed here to the client of its choosing
					// (unless that client is not authorized)
					//
					if (isAuthorized()) {
						broadcast(this);
						// broadcast(this.cmSigned);
					} else {
						display(this.username + " asked to send message of type " + this.cm.getType() + " to " + this.cm.getTo()
								+ ", but it was rejected because" + this.username + " is not authorized in the network.");
					}
					break;

				// ---------- Other types of messages are interpreted here by the server. ---------
				case LOGOUT:
					display(this.username + " disconnected with a LOGOUT message.");
					this.thisSocketIsActive = false;
					break;

				case WHOISIN:
					// writeMsg("WHOISIN List of the users connected at " + sdf.format(new Date()) + "\n");
					// scan all the users connected
					boolean purge = false;
					for (int i = 0; i < listOfMessagingClients.size(); ++i) {
						ClientThread ct = listOfMessagingClients.get(i);
						if (ct != null && ct.username != null && ct.date != null) {

							// This message comes from the server, who is acting on behalf of the client to say that it is alive.
							// This is not right in the context of signed messages--the server does not know how to sign this
							// message properly.
							//
							// A correct way to do this is to have the client (the one asking "WhoIsIn") to send a
							// "ping" type message to the server, which it will relay to every client it knows about.
							//
							// The way we are doing it here (which is not strictly correct) is to send unsigned messages
							// back to the client (the one asking "WhoIsIn") for each client out there.

							writeUnsignedMsg(MessageCarrier.getIAmAlive(ct.username, this.cm.getFrom(), "since " + ct.date));
						} else {
							display("Talking to " + this.username + " socket " + this.socket.getLocalAddress() + " (" + (i + 1)
									+ ") Error!  Have a null client");
							purge = true;
						}
					}
					if (purge)
						for (ClientThread CT : listOfMessagingClients) {
							if (CT == null) {
								display("Removing null ClientThread");
								listOfMessagingClients.remove(CT);
							} else if (CT.username == null) {
								display("Removing ClientThread with null username [" + CT + "]");
								listOfMessagingClients.remove(CT);
								CT.thisSocketIsActive = false;
							} else if (CT.date == null) {
								display("Removing ClientThread with null date [" + CT + "]");
								listOfMessagingClients.remove(CT);
								CT.thisSocketIsActive = false;
							}
						}
					break;
				}
			}
			// remove myself from the arrayList containing the list of the connected Clients
			display("Exiting forever loop for client '" + this.username + "' (thisSocketIsActive=" + this.thisSocketIsActive
					+ ") Removing id=" + id + ")");

			synchronized (listOfMessagingClients) {
				// scan the array list until we found the Id
				for (int i = 0; i < listOfMessagingClients.size(); ++i) {
					ClientThread ct = listOfMessagingClients.get(i);
					// found it
					if (ct.id == id) {
						listOfMessagingClients.remove(i);
						// Do not return; I want to see the information message at the end.
						break;
					}
				}
			}

			display(this.getClass().getSimpleName() + ": Number of remaining clients: " + listOfMessagingClients.size());
			close();
		}

		@Override
		public String toString() {
			return this.username + ", id=" + id + ", socket=[" + socket + "], date=" + date;
		}

		/**
		 * Write an unsigned object to the Client output stream
		 */
		boolean writeUnsignedMsg(MessageCarrier msg) {
			// if Client is still connected send the message to it
			if (!socket.isConnected()) {
				close();
				return false;
			}

			// write the message to the stream
			try {
				sOutput.writeObject(msg);
				sOutput.reset();
				return true;
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				display("IOException sending message to " + this.username);
				display(e.toString());
				printStackTrace(e);
			} catch (Exception e) {
				display(e.getLocalizedMessage() + ", Error sending message to " + this.username);
				printStackTrace(e);
			}
			return false;
		}

		/**
		 * Write a signed object to the Client output stream. **THIS IS THE PREFERRED WAY TO DO IT**
		 */
		boolean writeMsg(SignedObject signedMsg) {
			// if Client is still connected send the message to it
			if (!socket.isConnected()) {
				close();
				return false;
			}

			// write the message to the stream
			try {
				sOutput.writeObject(signedMsg);
				sOutput.reset();
				return true;
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				display("IOException sending signed message to " + this.username);
				display(e.toString());
				printStackTrace(e);
			} catch (Exception e) {
				display(e.getLocalizedMessage() + ", Error sending signed message to " + this.username);
				printStackTrace(e);
			}
			return false;
		}

		/**
		 * Is this client on notice, that is, have we recently thought it was tardy??
		 * 
		 * @return if this client is on notice for being tardy/absent
		 */
		public boolean isOnNotice() {
			return onNotice;
		}

		/**
		 * Put this client on notice, or take it off.
		 * 
		 * @param onNotice
		 */
		public void setOnNotice(boolean onNotice) {
			this.onNotice = onNotice;
		}

		public String getRemoteIPAddress() {
			String retval = socket.getRemoteSocketAddress().toString();
			return retval.substring(1, retval.indexOf(':'));
		}

		public boolean isAuthorized() {
			// TODO -- add an authorization that this messenger can/may send a command to this display
			return !checkSignedMessages() || cm.getType().isReadOnly() || cmSigned != null;
		}
	}

	/*************************************************************************************************************************
	 * Simple inner class to handle the insertion of a ClientThread object into a list.
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * @copyright 2014
	 * 
	 */

	private class ClientThreadList extends CopyOnWriteArrayList<ClientThread> {

		private static final boolean	REQUIRE_UNIQUE_USERNAMES	= true;

		private static final long		serialVersionUID			= 2919140620801861217L;

		@Override
		public boolean add(ClientThread ct) {
			if (ct == null || SPECIAL_SERVER_MESSAGE_USERNAME.equals(ct.username))
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

	protected static final String	SPECIAL_SERVER_MESSAGE_USERNAME	= "Server Message";

	/* ************************************************************************************************************************
	 * Begin the definition of the class MessagingServer
	 * 
	 * Static stuff
	 */

	// a unique ID for each connection
	static int						uniqueId;

	/**
	 * To run as a console application just open a console window and: > java Server > java Server portNumber If the port number is
	 * not specified 1500 is used
	 * 
	 * @param args
	 *            -- The command line arguments. [0] is the port number, otherwise MESSAGING_SERVER_PORT (49999) is used
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

	private void markClientAsSeen(String from) {
		for (ClientThread CT : listOfMessagingClients)
			if (from.equals(CT.username))
				CT.setLastSeen();
	}

	// an ArrayList to keep the list of the Client
	// private ArrayList<ClientThread> al; This Template is supposed to help with ConcurrentModificationException
	ClientThreadList			listOfMessagingClients;
	private Object				broadcasting			= "An Object for Java synchronization";
	// the boolean that will be turned of to stop the server
	private boolean				keepGoing;

	private int					numConnectionsSeen		= 0;
	// the port number to listen for connection
	private int					port;

	// to display time (Also used as a synchronization object for writing messages to the local terminal/GUI
	protected SimpleDateFormat	sdf;
	private Thread				showClientList			= null;
	private long				startTime				= System.currentTimeMillis();

	private long				sleepPeriodBtwPings		= 2000L;
	private long				tooOldTime				= 100L * sleepPeriodBtwPings;

	protected int				totalMesssagesHandled	= 0;
	private int					numClientsRemoved		= 0;
	private int					numClientsputOnNotice	= 0;

	/**
	 * server constructor that receive the port to listen to for connection as parameter in console
	 * 
	 * @param port
	 */
	public MessagingServer(int port) {
		this.port = port;
		sdf = new SimpleDateFormat("dd MMM HH:mm:ss");
		// ArrayList of the Clients
		listOfMessagingClients = new ClientThreadList();
		launchMemoryWatcher();
	}

	private void broadcast(ClientThread ct) {
		if (ct.cmSigned != null)
			broadcast(ct.cmSigned);
		else
			broadcast(ct.cm);
	}

	/**
	 * Broadcast an unsigned message to a Client
	 * 
	 * Overrideable if the base class wants to know about the messages.
	 * 
	 * Be sure to call super.broadcast(message) to actually get the message broadcast, though!
	 * 
	 * @param message
	 *            -- The message to broadcast
	 */
	protected void broadcast(MessageCarrier mc) {
		synchronized (broadcasting) { // Only send one message at a time
			// we loop in reverse order in case we would have to remove a Client because it has disconnected
			for (int i = listOfMessagingClients.size(); --i >= 0;) {
				ClientThread ct = listOfMessagingClients.get(i);
				// try to write to the Client if it fails remove it from the list
				if (mc.isThisForMe(ct.username))
					if (!ct.writeUnsignedMsg(mc)) {
						listOfMessagingClients.remove(i);
						display("Disconnected Client " + ct.username + " removed from list.");
					}
			}
		}
	}

	/**
	 * Broadcast a signed message to a Client
	 * 
	 * Overrideable if the base class wants to know about the messages.
	 * 
	 * Be sure to call super.broadcast(message) to actually get the message broadcast, though!
	 * 
	 * @param message
	 *            -- The message to broadcast
	 */
	protected void broadcast(SignedObject message) {
		try {
			MessageCarrier mc = (MessageCarrier) message.getObject();
			synchronized (broadcasting) { // Only send one message at a time
				// we loop in reverse order in case we would have to remove a Client because it has disconnected
				for (int i = listOfMessagingClients.size(); --i >= 0;) {
					ClientThread ct = listOfMessagingClients.get(i);
					// try to write to the Client if it fails remove it from the list
					if (mc.isThisForMe(ct.username))
						if (!ct.writeMsg(message)) {
							listOfMessagingClients.remove(i);
							display("Disconnected Client " + ct.username + " removed from list.");
						}
				}
			}
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Display a message to the console or the GUI
	 */
	protected void display(String msg) {
		synchronized (sdf) { // Only print one message at a time
			String time = sdf.format(new Date()) + " " + msg;
			System.out.println(time);
		}
	}
	
	protected void event(String msg) {
		synchronized (sdf) { // Only print one message at a time
			String time = sdf.format(new Date()) + " " + msg;
			System.out.println(time);
		}
	}

	protected void error(String msg) {
		synchronized (sdf) { // Only print one message at a time
			String time = sdf.format(new Date()) + " " + msg;
			System.err.println(time);
		}
	}

	protected void printStackTrace(Exception e) {
		synchronized (sdf) { // Only print one message at a time
			e.printStackTrace();
		}
	}

	protected void performDiagnostics(boolean show) {
		// check for old clientS (using ClientThread.lastSeen)

		String oldestName = "";
		long oldestTime = System.currentTimeMillis() + FIFTEEN_MINUTES;
		for (ClientThread CT : listOfMessagingClients) {
			if (show && CT.getLastSeen() < oldestTime) {
				oldestTime = CT.getLastSeen();
				oldestName = CT.username + " (" + CT.getRemoteIPAddress() + "), last seen " + new Date(CT.getLastSeen());
			}
			if ((System.currentTimeMillis() - CT.getLastSeen()) > tooOldTime) {

				// Give this client a second chance.

				if (CT.isOnNotice()) {
					listOfMessagingClients.remove(CT);
					CT.close();
					display("Removed Client [" + CT.username + "] because its last response was more than " + (tooOldTime / 1000L)
							+ " seconds ago: " + new Date(CT.getLastSeen()));
					numClientsRemoved++;
					CT = null; // Not really necessary, but it makes me feel better.
				} else {
					display("Client [" + CT.username + "] is now 'on notice' because its last response was more than "
							+ (tooOldTime / 1000L) + " seconds ago: " + new Date(CT.getLastSeen()));
					CT.setOnNotice(true);
					numClientsputOnNotice++;
				}
			}
		}

		if (show) {
			long aliveTime = System.currentTimeMillis() - startTime;
			long days = aliveTime / ONE_DAY;
			long hours = (aliveTime % ONE_DAY) / ONE_HOUR;
			long mins = (aliveTime % ONE_HOUR) / ONE_MINUTE;
			long secs = (aliveTime % ONE_MINUTE) / ONE_SECOND;

			display("\n                       " + MessagingServer.class.getSimpleName() + " has been alive " + days + " d " + hours
					+ " hr " + mins + " min " + secs + " sec (" + aliveTime + " msec)\n                       "
					+ numConnectionsSeen + " connections accepted, " + listOfMessagingClients.size() + " client"
					+ (listOfMessagingClients.size() != 1 ? "s" : "") + " connected right now, " + totalMesssagesHandled
					+ " messages handled\n" + "                       Oldest client is " + oldestName + "\n"
					+ "                       Number of clients put 'on notice': " + numClientsputOnNotice + "\n"
					+ "                       Number of clients removed due to lack of response: " + numClientsRemoved);
		}
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
				if (listOfMessagingClients.add(t)) // save it in the ArrayList
					t.start();
				else
					display("Error! Duplicate username requested, '" + t.username + "'");
				if (showClientList == null) {

					// This block is to print out the current set of clients. Rather than printing it out every time
					// a client connects, we wait 3 seconds from the first connect to do this. Usually, a lot of clients
					// connect at the same time--either the Channel Selector or when all the Displays reboot.

					showClientList = new Thread("ShowClientList") {
						long	WAIT_FOR_ALL_TO_CONNECT_TIME	= 3000L;

						public void run() {
							catchSleep(WAIT_FOR_ALL_TO_CONNECT_TIME);

							String m = "List of connected clients:\n";
							for (ClientThread CT : listOfMessagingClients) {
								m += "           " + CT.username + " (" + CT.getRemoteIPAddress() + "), last seen at "
										+ new Date(CT.getLastSeen()) + " ID=" + CT.id + "\n";
							}
							display(m);
							performDiagnostics(true);
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
				for (int i = 0; i < listOfMessagingClients.size(); ++i) {
					ClientThread tc = listOfMessagingClients.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException ioE) {
						// not much I can do
						printStackTrace(ioE);
					}
				}
			} catch (Exception e) {
				display("Exception closing the server and clients: " + e);
				printStackTrace(e);
			}
		}
		// something went wrong
		catch (IOException e) {
			String msg = sdf.format(new Date()) + " IOException on new ServerSocket: " + e + "\n";
			display(msg);
			printStackTrace(e);
		} catch (Exception e) {
			String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
			printStackTrace(e);
		}
	}

	/**
	 * Ping each client from time to time to assure that it is (still) alive
	 */
	private void startPinger() {
		new Thread("Pinger") {
			private int		nextClient	= 0;
			private long	lastPrint	= System.currentTimeMillis();

			public void run() {
				catchSleep(15000L); // Wait a bit before starting the pinging and the diagnostics

				while (keepGoing)
					try {
						catchSleep(sleepPeriodBtwPings); // Defaults: 200 seconds is "too old"; Two seconds between pings

						if (listOfMessagingClients.size() == 0)
							continue;

						// This sleep period assures that each client is pinged at least twice before it is "too old"
						sleepPeriodBtwPings = Math.min(tooOldTime / listOfMessagingClients.size() / 2L, 2000L);

						/*
						 * This boolean turns on the diagnostic message (in ClientThread.run()) that echos when a client says
						 * "I am alive". Turn it on for two minutes, just before the diagnostic is printed, which would show about
						 * 60 such messages (at a period of 2Hz). today, there are ~40 clients connected, so we'll see them all
						 * 
						 * 1/10/15 -- Change from 78000L after the last time to 800000L (13.4 minutes; prints for 96 seconds or so)
						 */
						showAliveMessages = (lastPrint + 80000L) < System.currentTimeMillis();

						// Figure out which client(s) to ping
						List<String> clist = new ArrayList<String>();

						long oldestTime = listOfMessagingClients.get(0).getLastSeen();
						String oldestClientName = listOfMessagingClients.get(0).username;
						// Ping any client that is "on notice", plus the oldest one that is not on notice
						for (int i = 1; i < listOfMessagingClients.size(); i++) {
							ClientThread CT = listOfMessagingClients.get(i);
							if (CT.isOnNotice())
								clist.add(CT.username);
							else if (CT.getLastSeen() < oldestTime) {
								oldestTime = CT.getLastSeen();
								oldestClientName = CT.username;
							}
						}

						clist.add(oldestClientName);

						for (String S : clist) {
							catchSleep(1);
							MessageCarrier mc = MessageCarrier.getIsAlive(SPECIAL_SERVER_MESSAGE_USERNAME, S);
							// This is a read-only message and DOES NOT NEED a signature.
							broadcast(mc);
						}

						/*
						 * TODO Maybe the right way to do this is to _broadcast_ a "Are You Alive" message (really, broadcast with
						 * one message to anyone that may be listening) and capture the results. In this case, it would be nice to
						 * have each client wait some random number of milliseconds so the messages come in with few(er) collisions.
						 * The method here works, but it takes some time to go through all the clients. This delay means that it is
						 * a long time before we actually learn that a client is not there.
						 */

						/*
						 * The idea here is to ping any client that is "on notice," plus the oldest client. If I have implemented
						 * this right and all clients are prompt about reporting, then in the steady state it will march through and
						 * ping the clients in the same order. Clients that send something to the server (like the Facades) will get
						 * pushed to the end of the list, though.
						 */

						nextClient = (++nextClient) % listOfMessagingClients.size();
						if (nextClient == 0) {
							boolean old = (lastPrint + FIFTEEN_MINUTES) < System.currentTimeMillis();
							performDiagnostics(old);

							if (old)
								lastPrint = System.currentTimeMillis();
						}
					} catch (Exception e) {
						// Random, unexpected exceptions have bitten me before (killing this thread); don't let that happen again.
						e.printStackTrace();
					}
			}
		}.start();
	}

	/*
	 * For the GUI to stop the server
	 */
	protected void stop() {
		keepGoing = false;
		// connect to myself as Client to exit statement
		// Socket socket = serverSocket.accept();
		try {
			// Huh? (11/04/2014)
			new Socket("localhost", port);
		} catch (Exception e) {
			// nothing I can really do
			printStackTrace(e);
		}
	}

}
