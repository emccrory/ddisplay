package gov.fnal.ppd.dd.chat;

/*
 * MessagingServer
 *
 * Taken from the internet on 5/12/2014. @see <a
 * href="http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/" dreamincode.net</a>
 * 
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_DAY;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.dd.GlobalVariables.SIMPLE_RECOVERABLE_ERROR;
import static gov.fnal.ppd.dd.GlobalVariables.UNRECOVERABLE_ERROR;
import static gov.fnal.ppd.dd.GlobalVariables.setLogger;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.launchMemoryWatcher;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.interfaces.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.util.nonguiUtils.JavaChangeListener;
import gov.fnal.ppd.dd.util.nonguiUtils.JavaVersion;
import gov.fnal.ppd.dd.util.specific.ObjectSigning;
import gov.fnal.ppd.dd.util.version.VersionInformation;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;
import gov.fnal.ppd.dd.xml.SubscriptionSubject;
import gov.fnal.ppd.dd.xml.messages.AreYouAliveMessage;
import gov.fnal.ppd.dd.xml.messages.ChangeChannelReply;
import gov.fnal.ppd.dd.xml.messages.EmergencyMessXML;
import gov.fnal.ppd.dd.xml.messages.ErrorMessage;
import gov.fnal.ppd.dd.xml.messages.LoginMessage;
import gov.fnal.ppd.dd.xml.messages.WhoIsInMessage;
import gov.fnal.ppd.dd.xml.messages.YesIAmAliveMessage;

/**
 * The central messaging server for the Dynamic Displays system
 * 
 * <p>
 * In this application of the idea, I have pretty much a stove-pipe server with only one bit of a protocol, handled elsewhere. The
 * message is an object (see MessageCarrierXML.java) with four attributes:
 * <ul>
 * <li>MessageType type; -- An enum of the sorts of messages we expect</li>
 * <li>String message; -- The message body (if it is required). in principle, this can be encrypted (and then ASCII-fied). In this
 * architecture, when the message is used, it is usually an XML document</li>
 * <li>String to; -- The name of the client who is supposed to get this message.
 * <em>This could be changed to a "subscription topic."</em></li>
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
 * simple way to implement this would be to add a time stamp to MessageCarrierXML which is encrypted and re-ASCII-fied. Using an
 * asymmetric encryption, the source (the ChannelSelector) can encrypt it with its secret key and then the destination (the Display)
 * can decrypt it with the public key and then verify that the date is "recent". Or one can encrypt the XML document itself (which
 * includes a time stamp). This will probably not be necessary at Fermilab as it is hard to imagine a situation where someone would
 * put bad stuff into the stream of messages and foul things up. But hackers are a clever bunch, so never say never.
 * </p>
 * <p>
 * We have opted not to encrypt these messages, but rather to cryptographically sign them.
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
 * <p>
 * <b>TODO</b> - Improve the architecture
 * </p>
 * 
 * <p>
 * According to http://docs.oracle.com/cd/E19957-01/816-6024-10/apa.htm (the first hit on Google for "Messaging Server Architecture"
 * ), a well-built messaging server has these six components:
 * <ul>
 * <li>The dispatcher is the daemon (or service) component of the Messaging Server. It coordinates the activities of all other
 * modules. In Figure A.1, the dispatcher can be thought of as an envelope that contains and initiates the processes of all items
 * shown within the largest box. <i>Yes-ish</i></li>
 * <li>The module configuration database contains general configuration information for the other modules.<i>Yes-ish</i></li>
 * <li>The message transfer agent (MTA) handles all message accepting, routing, and delivery tasks. <i>Yes-ish</i></li>
 * <li>The Messaging Server managers facilitate remote configuration and operation of the system and ensure that the modules are
 * doing their job and following the rules. <i>Not really</i></li>
 * <li>The AutoReply utility lets the Messaging Server automatically reply to incoming messages with predefined responses of your
 * choice. <i>No</i></li>
 * <li>The directory database--either a local directory database or a Directory Server--contains user and group account information
 * for the other modules. <i>Yes-ish</i></li>
 * </ul>
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 */
public class MessagingServer implements JavaChangeListener {

	private static boolean		showAliveMessages		= false;

	private static boolean		showAllIncomingMessages	= false;

	private static final int	MAX_STATUS_MESSAGE_SIZE	= 4196;

	/*************************************************************************************************************************
	 * Handle the communications with a specific client in the messaging system. One instance of this thread will run for each
	 * client.
	 * 
	 * This class the surrounding MessagingServer class by calling logger.fine() and by manipulating the list, al.
	 * 
	 * Style note: Using the syntax, "this.attribute" within this inner class to mark the stuff owned by this class. I also tried
	 * marking the surrounding calls with "MessagingServer.this.", but this was just too much clutter (IMHO).
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 */
	private class ClientThread extends Thread {

		// the only types of message we will deal with here
		MessageCarrierXML			cm;

		// the date I connected
		// String dateString;
		Date						date;

		// my unique id (easier for deconnection)
		int							id;

		// When was this client last seen?
		long						lastSeen			= System.currentTimeMillis();

		// If this client has been absent for too long, we put it "on notice"
		boolean						onNotice			= false;
		int							numOutstandingPings	= 0;

		// the input stream for our conversation
		protected InputStream		sInput;
		protected BufferedReader	receiver;
		// the output stream for our conversation
		protected OutputStream		sOutput;

		// the socket where to listen/talk
		Socket						socket;

		// Does it seem like the socket for this client is active?
		boolean						thisSocketIsActive	= false;

		// the Username of the Client
		String						username;
		private InetAddress			theIPAddress;
		private Thread				myShutdownHook;

		/**
		 * Constructor - call when a potential client connects to the socket. Note that this implementation relies on the fact that
		 * this constructor will return almost immediately!
		 * 
		 * @param socket
		 */
		ClientThread(Socket socket) {
			super("ClientThread_of_MessagingServer_" + MessagingServer.uniqueId);

			// a unique id
			this.id = MessagingServer.uniqueId.getAndIncrement();
			this.socket = socket;

			final int initialID = this.id;
			this.myShutdownHook = new Thread("ShutdownHook_of_MessagingServer_" + MessagingServer.uniqueId) {
				public void run() {
					println(ClientThread.class,
							"Shutting down client connection, initialID=" + initialID + ", final ID=" + ClientThread.this.id);
					close(true);
				}
			};
			Runtime.getRuntime().addShutdownHook(this.myShutdownHook);

			theIPAddress = socket.getInetAddress();
		}

		public void initialize() {
			MessageCarrierXML read = null;
			/* Creating both Data Stream */
			logger.fine("Starting client thread at uniqueID=" + id + " connected to " + theIPAddress + ", port "
					+ socket.getLocalPort() + ", remote port " + socket.getPort());
			boolean probablePortScanner = false;
			if (theIPAddress.toString().contains("131.225.12.") || theIPAddress.toString().contains("0:0:0:0:0:0:")) {
				// Well, at Fermilab, this is a port scanner. It would be cheating to ignore these requests altogether since
				// this is an assumption here (that is, if the suite goes elsewhere, or they change their addressing, this shield
				// will not help). So we really need to figure out how to withstand these probes.
				probablePortScanner = true;
			}
			int socketTimeout = 10 * ((int) ONE_SECOND);

			try {
				/*
				 * FIXME ??? --- On June 2, 2015, 15:16:33 (approximately) I connected a display server with a mixed-up IP address.
				 * Its internal reckoning of its IP address was different from what DNS said. I was able to connect to the server
				 * (and this is the spot where that connection happens on the server), and everything seemed to work. However, at
				 * that precise moment, the messaging server lost track of ALL of its clients, and all the clients lost track of the
				 * server. I do not see how this could have happened, unless it is something within the "socket" mechanism of Java.
				 * So, future self, be warned that this could happen again and foul everything up.
				 */

				socket.setSoTimeout(socketTimeout);
				// create output first
				this.sOutput = socket.getOutputStream();
				this.sInput = socket.getInputStream();
				this.receiver = new BufferedReader(new InputStreamReader(sInput));

				if (probablePortScanner)
					logger.fine("First call to MessageConveyor.getNextDocument(), local port=" + this.socket.getLocalPort());

				// Consider this: Maybe we only accept connections from IP addresses we know, from looking inside the database.
				// Only Displays and Controllers can and may connect.

				// This message MUST be a login message that comes immediately.
				read = MessageConveyor.getNextDocument(getClass(), receiver, " (new connection, client ID=" + this.id + ")");

				// Whew! Got through the first read, so wait forever on subsequent ones.
				socket.setSoTimeout(0);

				if (probablePortScanner)
					logger.fine("Got a document from MessageConveyor.getNextDocument() of type "
							+ read.getMessageValue().getClass().getCanonicalName());

				if (!(read.getMessageValue() instanceof LoginMessage)) {
					logger.warning("Expected " + LoginMessage.class.getCanonicalName() + " message " + read.getMessageValue()
							+ ", but got a " + read.getMessageValue().getClass().getCanonicalName() + " message");
					close();
					return;
				}
				LoginMessage lm = (LoginMessage) read.getMessageValue();
				String un = lm.getClient().getName();
				this.username = un + "_" + id;
				logger.fine("'" + this.username + "' has connected.");
				setName("ClientThread_of_MessagingServer_" + username);
				this.date = new Date();
			} catch (UnrecognizedCommunicationException e) {
				logger.warning("We have a potential client that is not behaving properly.  We will try to ignore it. "
						+ exceptionString(e));
				close();
			} catch (SocketTimeoutException e) {
				logger.warning(
						"We have a potential client that did not immediately respond to a connection.  We will try to ignore it. "
								+ exceptionString(e));
				close();
			} catch (Exception e) {
				// Usually, it seems we fall here when something tries to connect to us but is not one of the Java clients.
				// At Fermilab, this happens when this port gets scanned.
				logger.warning("Exception creating new Input/output streams on socket (" + socket + ") due to this exception: "
						+ exceptionString(e));
				// e.printStackTrace();
				close();
			}
			// this.dateString = date.toString();
		}

		// try to close everything
		protected void close() {
			close(false);
		}

		protected void close(boolean duringShutdown) {
			String progress = "";
			try {
				logger.fine("Closing client " + username);
				thisSocketIsActive = false;
				numClosedCallsSeen++;
				try {
					if (this.sOutput != null) {
						this.sOutput.close();
						progress += " Closed sOutput.";
					}
				} catch (Exception e) {
					logger.warning(exceptionString(e));
					progress += " Problems closing sOutput.";
				}
				try {
					if (this.sInput != null) {
						this.sInput.close();
						progress += " Closed sInput.";
					}
				} catch (Exception e) {
					logger.warning(exceptionString(e));
					progress += " Problems closing sInput.";
				}
				try {
					if (this.socket != null) {
						this.socket.close();
						progress += " Closed socket.";
					}
				} catch (Exception e) {
					logger.warning(exceptionString(e));
					progress += " Problems closing socket.";
				}
				this.sInput = null;
				this.sOutput = null;
				this.socket = null;

				if (!duringShutdown && this.myShutdownHook != null) {
					Runtime.getRuntime().removeShutdownHook(this.myShutdownHook);
					this.myShutdownHook = null;
					progress += " Removed shutdown hook.";
				} else {
					progress += " No shutdown hook to remove.";
				}
			} catch (Exception e) {
				logger.warning(exceptionString(e));
			}
			logger.fine("Closed client " + username + ":" + progress);
		}

		public long getLastSeen() {
			return this.lastSeen;
		}

		public void setLastSeen() {
			this.lastSeen = System.currentTimeMillis();
			setOnNotice(false);
			resetNumOutstandingPings();
		}

		/**
		 * This is the heart of the messaging server: The place where all of the incoming messages are received and processed.
		 * 
		 * Each instance of this inner class corresponds, one-to-one, to a specific client.
		 * 
		 * This method will run for as long as this client is connected.
		 * 
		 * Historically, this is where the most difficult problems are encountered. So, beware!
		 * 
		 */
		public void run() {
			// to loop until LOGOUT or we hit an unrecoverable exception
			// Bug fix for input and output objects: call "reset" after every read and write to not let these objects
			// remember their state. After a few hours, we run out of memory!

			// This block of code is continuing to make problems (7/17/2015). One way to handle it is to simplify it
			// somehow. There is a lot of diagnostics mixed in with the processing. Maybe I need to pull that out in some way.
			// Bottom line: Simplify!!

			this.thisSocketIsActive = true;
			while (this.thisSocketIsActive) {
				MessageCarrierXML read = null;
				try {
					this.cm = read = MessageConveyor.getNextDocument(getClass(), receiver, username);
					setLastSeen();
					if (showAllIncomingMessages)
						logger.fine("MessagingServer.ClientThread: Got message: " + cm);

					// Idiot check: (Rarely seen, and never seen if the "username" is not null)
					if (!MessageCarrierXML.isUsernameMatch(username, cm.getMessageOriginator()))
						logger.warning("Oops (a).  My assumption is wrong.  This thread is for '" + username
								+ "' and the from field on the message is '" + cm.getMessageOriginator() + "'");

					if (!read.isReadOnly()) {
						if (read.isSignatureValid()) {
							logger.fine("Message is properly signed: " + this.cm);
						} else {
							logger.warning("Message is NOT PROPERLY SIGNED: [" + this.cm
									+ "]; -- ignoring this message and sending an error message back to the client.");

							System.out.println("(((( Improperly signed message is this:\n" + read + "\n))))");

							// Reply to the client that this message was rejected!
							writeUnsignedMsg(MessageCarrierXML.getErrorMessage(SPECIAL_SERVER_MESSAGE_USERNAME,
									this.cm.getMessageOriginator(), "The message to display '" + this.cm.getMessageRecipient()
											+ "' does not have a correct cryptographic signature; it has been rejected."));
							continue;
						}
					}
				} catch (Exception e) {
					String dis = e.getClass().getName() + ", Message=[" + e.getMessage() + "], client=" + this.username + " ";
					logger.warning(dis + exceptionString(e));

					// Make all the other incoming messages wait until the processing here is done.
					if (read == null) {
						String mes = "The reading of the input stream produced a null object. Client: " + this.username;
						logger.warning(mes);
					} else {
						dis = "Client " + this.username + ": Exception reading input stream -- " + e + ";\n          "
								+ "The received message was '" + read + "'\n          (type=" + read.getClass().getCanonicalName()
								+ ")";

						String frm = "null";
						String typ = "null";
						String tto = "null";
						if (this.cm != null) {
							frm = this.cm.getMessageOriginator();
							typ = this.cm.getMessageValue().toString();
							tto = this.cm.getMessageRecipient();
						}
						// Reply to the client that this message was rejected!
						String errorMessage = "Your message of type " + typ + " to client, " + tto
								+ " was not processed because there was an internal error in the messaging server; exception type is "
								+ e.getClass().getCanonicalName();
						logger.fine("Sending error message to client: [[[" + errorMessage + "]]]");
						if (!frm.equals("null"))
							writeUnsignedMsg(MessageCarrierXML.getErrorMessage(SPECIAL_SERVER_MESSAGE_USERNAME, frm, errorMessage));
					}
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
				} // ----- End of "try" block overseeing the read, this time, of the socket -----

				totalMesssagesHandled++;

				Class<?> clazz = this.cm.getMessageValue().getClass();
				if (clazz.equals(AreYouAliveMessage.class)
						&& SPECIAL_SERVER_MESSAGE_USERNAME.equals(this.cm.getMessageRecipient())) {
					if (showAliveMessages)
						logger.fine("Alive msg: " + cm.getMessageOriginator().trim().replace(".fnal.gov", ""));
					continue; // That's all for this while-loop iteration. Go read the socket again...
				}

				if (showAllIncomingMessages)
					logger.fine("MessagingServer.ClientThread: Got message of type " + clazz + ": " + cm);

				if (clazz.equals(YesIAmAliveMessage.class) || clazz.equals(ErrorMessage.class)
						|| clazz.equals(ChangeChannelReply.class)) {
					broadcast(this.cm);
				} else if (clazz.equals(LoginMessage.class)) {
					logger.fine(this.username + " disconnected with a LOGOUT message.");
					this.thisSocketIsActive = false;
					numLogoutsSeen++;
				} else if (clazz.equals(WhoIsInMessage.class)) {
					// writeMsg("WHOISIN List of the users connected at " + sdf.format(new Date()) + "\n");
					// scan all the users connected
					boolean purge = false;
					// The iterator (implicitly used here) makes a thread-safe copy of the list prior to traversing it.
					// logger.fine("Responding to WhoIsIn message with data from " + listOfMessagingClients.size() + " clients");
					for (ClientThread ct : listOfMessagingClients) {
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

							// logger.fine("Responding with 'YesIAmAlive' message to " + this.cm.getMessageOriginator() + " from " +
							// ct.username);

							writeUnsignedMsg(
									MessageCarrierXML.getIAmAlive(ct.username, this.cm.getMessageOriginator(), date.getTime()));
						} else {
							logger.warning("Talking to " + this.username + " socket " + this.socket.getLocalAddress()
									+ ". Error!  Unexpectedly have a null client");
							purge = true;
						}
					}
					if (purge)
						for (ClientThread CT : listOfMessagingClients) {
							if (CT == null) {
								logger.fine("Removing null ClientThread");
								remove(CT);
								numRemovedNullClientThread++;
							} else if (CT.username == null) {
								logger.fine("Removing ClientThread with null username [" + CT + "]");
								remove(CT);
								CT.thisSocketIsActive = false;
								numRemovedNullUsername++;
							} else if (CT.date == null) {
								logger.fine("Removing ClientThread with null date [" + CT + "]");
								remove(CT);
								CT.thisSocketIsActive = false;
								numRemovedNullDate++;
							}
						}
				} else if (clazz.equals(EmergencyMessXML.class)) {
					if (isAuthorized(this.cm.getMessageOriginator(), this.cm.getMessageRecipient())
							&& isClientAnEmergencySource(this.cm.getMessageOriginator())) {
						broadcast(this);
						// broadcast(this.cmSigned);
					} else {
						String mess = "Message rejected!  '" + this.username + "' asked to send message of type " + clazz + " to '"
								+ this.cm.getMessageRecipient() + "'\n\t\tbut ";
						if (isClientAnEmergencySource(this.cm.getMessageOriginator())) {
							mess += "it is not authorized to send any sort of message to this client";

						} else {
							mess += "it is not authorized to generate an emergency message";
						}
						logger.warning(mess);

						// Reply to the client that this message was rejected!
						writeUnsignedMsg(
								MessageCarrierXML.getErrorMessage(SPECIAL_SERVER_MESSAGE_USERNAME, this.cm.getMessageOriginator(),
										"You are not authorized to put an emergency message on the display called "
												+ this.cm.getMessageRecipient() + ".\nThis directive has been rejected."));
					}
				} else if (clazz.equals(SubscriptionSubject.class)) {
					SubscriptionSubject subject = (SubscriptionSubject) this.cm.getMessageValue();
					ClientThreadList ctl = subjectListeners.get(subject);
					if (ctl == null) {
						ctl = new ClientThreadList();
						subjectListeners.put(subject.getTopic(), ctl);
					}
					ctl.add(this);
				} else {
					//
					// The message, received from a client, is relayed here to the client of its choosing
					// (unless that client is not authorized)
					//
					if (isAuthorized(this.cm.getMessageOriginator(), this.cm.getMessageRecipient())) {
						broadcast(this);
						// broadcast(this.cmSigned);
					} else {
						logger.warning("Message rejected!  '" + this.username + "' asked to send message of type "
								+ clazz.getSimpleName() + " to '" + this.cm.getMessageRecipient()
								+ "'\n\t\tbut it is not authorized to send a message to this client");

						// Reply to the client that this message was rejected!
						writeUnsignedMsg(
								MessageCarrierXML.getErrorMessage(SPECIAL_SERVER_MESSAGE_USERNAME, this.cm.getMessageOriginator(),
										"You are not authorized to change the channel on the display called "
												+ this.cm.getMessageRecipient() + ".\nThis directive has been rejected."));
					}
				}

				// TODO Add a message type of "SubscribeTo" and work that into the code.
			}
			// remove myself from the arrayList containing the list of the connected Clients
			logger.fine("Exiting CLIENT forever loop for '" + this.username + "' (thisSocketIsActive=" + this.thisSocketIsActive
					+ ") Removing id=" + id + ") (This is a normal operation)");

			synchronized (listOfMessagingClients) {
				// scan the array list until we found the Id
				for (ClientThread ct : listOfMessagingClients) {
					// found it
					if (ct.id == id) {
						remove(ct);
						logger.fine("Removed id=" + id);
						numRemovedExitedForeverLoop++;
						// Do not return; I want to see the information message at the end.
						break;
					}
				}
				// Is it strange when control comes here? The client.id was not found in the list.
			}

			if (ObjectSigning.dropClient(this.username))
				logger.fine(this.getClass().getSimpleName() + ": '" + this.username + "' Removed from ObjectSigning cache");

			logger.fine(this.getClass().getSimpleName() + ": Number of remaining clients: " + listOfMessagingClients.size());
			close();
		}

		@Override
		public String toString() {
			return this.username + ", socket=[" + socket + "], date=" + date;
		}

		/**
		 * Write a regular message to the Client output stream
		 */
		boolean writeUnsignedMsg(MessageCarrierXML msg) {
			// System.out.println(new Date() + " " + getClass().getSimpleName() + ".writeUnsignedMsg() for " + username
			// + ": message is [" + msg + "]");
			if (socket == null) {
				logger.warning("Socket object is null--cannot send the message " + msg);
				return false;
			}
			// if Client is still connected send the message to it
			if (!socket.isConnected() || socket.isClosed() || socket.isOutputShutdown() || socket.isInputShutdown()) {
				numUnexpectedClosedSockets2++;
				close();
				logger.warning("Position 2 (failure)");
				return false;

				// ***** Important Note on the limitations of this bit of the code! *****

				/*
				 * It is not possible to detect when a client on a socket is killed due to, say, a sudden reboot. This code seems to
				 * handle all the other situations, but not the reboot one. (I tested "kill -9" on a client, and that seemed to be
				 * caught by this code.) See, for example, http://stackoverflow.com/questions/969866/java-detect-lost-connection
				 */
			}

			// write the message to the stream
			try {
				String theMessageString = MyXMLMarshaller.getXML(msg);
				// logger.fine("Sending message of type " + msg.getMessageValue().getClass().getSimpleName() + " to " +
				// this.username);
				return MessageConveyor.writeToSocket(theMessageString, sOutput);

			} catch (Exception e) {
				logger.warning(e.getLocalizedMessage() + ", Error sending message to " + this.username + "\n" + exceptionString(e));
			}
			logger.warning("Position 3 (failure)");
			return false;
		}

		// /**
		// * Write a signed object to the Client output stream. **THIS IS THE PREFERRED WAY TO DO IT**
		// */
		// boolean writeMsg(SignedObject signedMsg) {
		// // if Client is still connected send the message to it
		// if (!socket.isConnected()) {
		// numUnexpectedClosedSockets1++;
		// close();
		// return false;
		// }
		//
		// // write the message to the stream
		// try {
		// sOutput.writeObject(signedMsg);
		// sOutput.reset();
		// return true;
		// }
		// // if an error occurs, do not abort just inform the user
		// catch (IOException e) {
		// logger.warning("IOException sending signed message to " + this.username + "\n" + e + "\n" + exceptionString(e));
		// } catch (Exception e) {
		// logger.warning(e.getLocalizedMessage() + ", Error sending signed message to " + this.username + e + "\n"
		// + exceptionString(e));
		// }
		// return false;
		// }

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

		public boolean incrementNumOutstandingPings() {
			return ++numOutstandingPings > 4;
		}

		public void resetNumOutstandingPings() {
			numOutstandingPings = 0;
		}

		public String getRemoteIPAddress() {
			String retval = socket.getRemoteSocketAddress().toString();
			return retval.substring(1, retval.indexOf(':'));
		}

		public boolean isAuthorized(String from, String to) {
			return ObjectSigning.isClientAuthorized(from, to);
		}
	}

	private boolean isClientAnEmergencySource(String from) {
		return ObjectSigning.getInstance().isEmergMessAllowed(from);
	}

	/*
	 * TODO -- New message to return the status of the server
	 * 
	 * It would be a useful diagnostic to ask the server to return to a client a diagnostics dump. The data in this dump would
	 * include the stuff that is kept in the diagnostics method, like the client names, how many messages there have been, some
	 * accounting for the errors that have been handled, how old each client is and how long since we have last seen it, etc.
	 * 
	 * This would be a new message type, I think.
	 * 
	 * I think, in general, I need to implement a handshake in the protocol. The thing that is missing is that the client that sends
	 * a "change the channel" message gets no feedback that its signature is wrong or that it does not have the authority to command
	 * that display. This new message will need this sort of handshake, and we definitely can use this handshake in the normal
	 * message flow.
	 * 
	 * 3/20/2015 -- E. McCrory.
	 */

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
						logger.fine("Unexpected null ClientThread at index=" + index + ": [" + CT + "]");
						remove(CT); // How did THIS happen??
						numRemovedNullUsername++;
					} else if (CT.username.equals(ct.username)) {
						logger.fine("Client named " + ct.username + " already in this list");
						return false; // Duplicate username is not allowed!
					}
					index++;
				}
			}
			// QUESTION -- Is a bad thing to have duplicate usernames? When/if the server directs messages to the intended user,
			// this WILL be necessary. But having a fairly anonymous clientelle works just fine.
			//
			// BUt I am seeing bugs whereby one client restarts and the server has not dropped the old connection.
			numConnectionsSeen++;
			boolean retval = super.add(ct);
			if (!retval) {
				logger.warning("Adding a client named " + ct.username + " gives an error from CopyOnWriteArrayList.add()");
			}
			return retval;
		}

	}

	/**
	 * The name we use for the messaging server, when it is sending a message out
	 */
	public static final String						SPECIAL_SERVER_MESSAGE_USERNAME	= "Server Message";

	private static int								myDB_ID							= 0;

	/*
	 * ************************************************************************************************************************
	 * Begin the definition of the class MessagingServer
	 * 
	 * Static stuff
	 */

	// a unique ID for each connection. It is atomic just in case two ask to connect at the same time (we have never seen this).
	static AtomicInteger							uniqueId						= new AtomicInteger(0);

	// private void markClientAsSeen(String from) {
	// for (ClientThread CT : listOfMessagingClients)
	// if (from.equals(CT.username) || CT.username.startsWith(from) || from.startsWith(CT.username)) {
	// logger.fine("Marking client " + from + "/" + CT.username + " as seen");
	// CT.setLastSeen();
	// }
	// }

	// an ArrayList to keep the list of all the Client
	ClientThreadList								listOfMessagingClients;

	ConcurrentHashMap<String, ClientThreadList>		subjectListeners;

	// A hash of the most recent message published on each subject
	ConcurrentHashMap<String, MessageCarrierXML>	lastMessage;

	// A synchronization object
	private Object									broadcasting					= "Object 1 for Java synchronization";
	// the boolean that will be turned of to stop the server
	private boolean									keepGoing;

	private int										numConnectionsSeen				= 0;
	// the port number to listen for connection
	private int										port;

	// to display time (Also used as a synchronization object for writing messages to the local terminal/GUI
	protected SimpleDateFormat						sdf;
	private Thread									showClientList					= null;
	private long									startTime						= System.currentTimeMillis();

	private long									sleepPeriodBtwPings				= 2 * ONE_SECOND;
	// "Too Old Time" is one minute
	private long									tooOldTime						= ONE_MINUTE;

	protected int									totalMesssagesHandled			= 0;

	private int										numRemovedForPings1				= 0;
	private int										numRemovedForPings2				= 0;
	private int										numClientsPutOnNotice			= 0;
	public int										numRemovedBadWriteSeen			= 0;
	public int										numRemovedNullClientThread		= 0;
	public int										numRemovedNullUsername			= 0;
	public int										numRemovedNullDate				= 0;
	public int										numRemovedExitedForeverLoop		= 0;
	private int										numRemovedDuplicateUsername		= 0;
	private int										numClosedCallsSeen				= 0;
	private int										numLogoutsSeen					= 0;

	private int										numUnexpectedClosedSockets1		= 0;
	private int										numUnexpectedClosedSockets2		= 0;
	private int										numDuplicateClients				= 0;
	private int										numPortScannerAttempts			= 0;
	private int										numberOfServerMessagesReceived	= 0;

	// private Object oneMessageAtATime = new String("For synchronization");

	// protected Logger logger;
	protected LoggerForDebugging					logger;

	private JavaVersion								javaVersion;

	/**
	 * server constructor that receive the port to listen to for connection as parameter in console
	 * 
	 * @param port
	 */
	public MessagingServer(int port) {
		this.port = port;
		JavaVersion.getInstance().addJavaChangeListener(this);

		try {
			// logger = Logger.getLogger(MessagingServer.class.getName());
			logger = new LoggerForDebugging(MessagingServer.class.getName());

			FileHandler fileTxt = new FileHandler("../../log/messagingServer.log");
			SimpleFormatter sfh = new SimpleFormatter() {
				private static final String	format1			= "[%1$tF %1$tT] [%2$s] %3$s %n";
				private static final String	format2			= "---------- From Logger %3$s: ---------- %n[%1$tF %1$tT] [%2$s] %4$s %n";
				private String				lastLoggerName	= "";

				@Override
				public synchronized String format(LogRecord record) {
					if (lastLoggerName.equals(record.getLoggerName())) {
						return String.format(format1, new Date(record.getMillis()), record.getLevel().getLocalizedName(),
								record.getMessage());
					}
					lastLoggerName = record.getLoggerName();
					return String.format(format2, new Date(record.getMillis()), record.getLevel().getLocalizedName(),
							lastLoggerName, record.getMessage());
				}

			};
			fileTxt.setFormatter(sfh);
			logger.addHandler(fileTxt);
			// logger.setLevel(Level.FINE);
			logger.setLevel(Level.FINER);
			// logger.setLevel(Level.FINEST);

			setLogger(logger.getLogger());

		} catch (Exception e) {
			logger.warning(exceptionString(e));
		}

		sdf = new SimpleDateFormat("dd MMM HH:mm:ss");

		// ArrayList of the Clients
		listOfMessagingClients = new ClientThreadList();
		subjectListeners = new ConcurrentHashMap<String, ClientThreadList>();
		lastMessage = new ConcurrentHashMap<String, MessageCarrierXML>();

		launchMemoryWatcher(logger.getLogger());
	}

	private void broadcast(ClientThread ct) {
		broadcast(ct.cm);
	}

	/**
	 * Broadcast an unsigned message to a Client
	 * 
	 * Overrideable if the base class wants to know about the messages.
	 * 
	 * Be sure to call super.broadcast(message) to actually get the message broadcast, though!
	 * 
	 * @param mc
	 *            The message to broadcast
	 */
	protected void broadcast(MessageCarrierXML mc) {
		synchronized (broadcasting) { // Only send one message at a time

			String subject = mc.getMessageRecipient();
			if (subject != null) {
				for (int m = subject.length() - 1; m >= 0; m--) {
					if (subject.charAt(m) == '_') { // Find the trailing "_index" and remove it
						subject = subject.substring(0, m);
						break;
					}
				}
			} else {
				logger.warning(getClass().getSimpleName() + " - Unexpected null subject in a message.  Message=[" + mc + "]");
			}
			lastMessage.put(subject, mc);

			ClientThreadList ctl = subjectListeners.get(subject);
			if (ctl != null) {
				// Write this message to this client
				for (ClientThread ct : ctl)
					if (MessageCarrierXML.isUsernameMatch(mc.getMessageRecipient(), ct.username))
						if (!ct.writeUnsignedMsg(mc)) {
							remove(ct);
							logger.warning(getClass().getSimpleName() + " - broadcast() FAILED.  mc=" + mc);
							numRemovedBadWriteSeen++;
						}
			} else if (mc.getMessageValue() instanceof YesIAmAliveMessage) {
				// We get a lot of these - ignore it
				return;
			} else if (!subject.equalsIgnoreCase("Server Message")) {
				logger.warning(getClass().getSimpleName() + " -  Hmm.  We have a message on the topic '" + subject
						+ "' but the list of clients interested in this is empty.  The message is of type "
						+ mc.getMessageValue().getClass().getCanonicalName());
				System.err.println("Subjects are:");
				for (String S : subjectListeners.keySet())
					System.err.println("\t" + S);
			} else {
				numberOfServerMessagesReceived++;
			}
		}
	}

	private void remove(final ClientThread ct) {
		listOfMessagingClients.remove(ct);
		for (String subject : subjectListeners.keySet()) {
			ClientThreadList ctl = subjectListeners.get(subject);
			if (ctl != null) {
				while (ctl.remove(ct))
					// Is it really possible that a list will contain two entries that are the same??
					;
			}
		}
		logger.fine("Disconnected Client " + ct.username + " removed from list.");
	}

	// We have abandoned this idea in favor of java.util.logging.Logger. It is better to have the calls to
	// logger in the code since that facility writes out where it was called from.
	//
	// /**
	// * Display a message to the console or the GUI
	// */
	// protected void logger.fine(String msg) {
	// synchronized (oneMessageAtATime) { // Only print one message at a time
	// // String time = sdf.format(new Date()) + " - " + msg;
	// // System.out.println(time);
	// logger.fine(msg);
	// }
	// }
	//
	// protected void logger.warning(String msg) {
	// synchronized (oneMessageAtATime) { // Only print one message at a time
	// // String time = sdf.format(new Date()) + " ERROR - " + msg;
	// // System.err.println(time);
	// logger.warning(msg);
	// }
	// }
	//
	// protected void exceptionPrint(exceptionString(Exception e) {
	// logger.warning(exceptionString(e));
	// }

	protected String exceptionString(Exception e) {
		if (e == null)
			return "NULL Exception!";

		String matchPackage = getClass().getName().substring(0, 12);
		StackTraceElement[] trace = e.getStackTrace();
		String retval = e.getClass().getCanonicalName() + ": " + (e.getMessage() == null ? "" : e.getMessage());
		if (trace != null) {
			for (StackTraceElement T : trace) {
				if (T.toString().contains(matchPackage))
					retval += "\n\t" + T;
				else
					retval += " [.] "; // Make the log file smaller by removing lines that are almost useless
			}
		} else {
			retval += " -- Stack trace is null!?";
		}
		return retval;
	}

	protected void performDiagnostics(boolean show) {
		// check for old clientS (using ClientThread.lastSeen)

		String oldestName = "";
		long oldestTime = System.currentTimeMillis() + FIFTEEN_MINUTES;
		for (ClientThread CT : listOfMessagingClients) {
			if (show && CT.getLastSeen() < oldestTime) {
				oldestTime = CT.getLastSeen();
				oldestName = CT.username + " (" + CT.getRemoteIPAddress() + ", " + CT.id + "), last seen "
						+ new Date(CT.getLastSeen());
			}
			if ((System.currentTimeMillis() - CT.getLastSeen()) > tooOldTime) {

				// Give this client a second chance.

				if (CT.isOnNotice()) {
					remove(CT);
					CT.close();
					logger.fine("Removed Client [" + CT.username + "] because its last response was more than "
							+ (tooOldTime / 1000L) + " seconds ago: " + new Date(CT.getLastSeen()));
					numRemovedForPings1++;
					CT = null; // Not really necessary, but it makes me feel better.
				} else {
					logger.fine("Client [" + CT.username + "] is now 'on notice' because its last response was more than "
							+ (tooOldTime / 1000L) + " seconds ago: " + new Date(CT.getLastSeen()));
					CT.setOnNotice(true);
					numClientsPutOnNotice++;
				}
			}
		}
		if (show) {
			long aliveTime = System.currentTimeMillis() - startTime;
			long days = aliveTime / ONE_DAY;
			long hours = (aliveTime % ONE_DAY) / ONE_HOUR;
			long mins = (aliveTime % ONE_HOUR) / ONE_MINUTE;
			long secs = (aliveTime % ONE_MINUTE) / ONE_SECOND;

			String subjectInfo = "*There are " + subjectListeners.size() + " subjects*\n";
			String LL = " listeners";
			String NN = "no";
			for (String subject : subjectListeners.keySet()) {
				ClientThreadList ctl = subjectListeners.get(subject);
				subjectInfo += "     " + subject + "\t[" + (ctl == null ? NN : ctl.size()) + LL + "]\n";
				LL = "";
				NN = "none";
			}
			// String spacesForStatus = " - ";
			String message = MessagingServer.class.getSimpleName() + " has been alive " + days + "D " + hours + ":" + mins + ":"
					+ secs + " (" + aliveTime + " msec)" //
					+ "\n         " + numConnectionsSeen + " connections accepted, " + listOfMessagingClients.size() + " client"
					+ (listOfMessagingClients.size() != 1 ? "s" : "") + " connected right now, " + totalMesssagesHandled
					+ " messages handled" //
					+ "\n         Oldest client is " + oldestName //
					+ "\n         Reasons (NC = Num Clients)              Count" //
					+ "\n         Num apparent port scanner connections: " + numPortScannerAttempts //
					+ "\n         Number of 'Server Message' messages:   " + numberOfServerMessagesReceived //
					+ "\n         NC put 'on notice':                    " + numClientsPutOnNotice //
					+ "\n         NC removed due to lack of response:    " + numRemovedForPings1 + " & " + numRemovedForPings2//
					+ "\n         NC rmvd for 'bad write':               " + numRemovedBadWriteSeen //
					+ "\n         NC rmvd for 'null client thread':      " + numRemovedNullClientThread //
					+ "\n         NC rmvd for null username:             " + numRemovedNullUsername //
					+ "\n         NC rmvd for null date:                 " + numRemovedNullDate //
					+ "\n         NC rmvd by exiting forever loop:       " + numRemovedExitedForeverLoop //
					+ "\n         NC rmvd for duplicate name:            " + numRemovedDuplicateUsername //
					+ "\n         NC rmvd for unexpected closed sockets: " + numUnexpectedClosedSockets1 + " & "
					+ numUnexpectedClosedSockets2 //
					+ "\n         NC rmvd for unexpected duplicate clients: " + numDuplicateClients //
					+ "\n    " + subjectInfo;
			logger.fine(message);
		}
	}

	private VersionInformation versionInfo = VersionInformation.getVersionInformation();

	private synchronized void updateStatus(String statusMessage) {
		SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

		try {
			long uptime = System.currentTimeMillis() - startTime;
			String query = "UPDATE MessagingServerStatus SET " //
					+ "Time='" + sqlFormat.format(new Date()) + "'," //
					+ "NumConnections=" + numConnectionsSeen + "," //
					+ "NumConnected=" + listOfMessagingClients.size() + "," //
					+ "MessagesHandled=" + totalMesssagesHandled + "," //
					+ "Version='" + versionInfo.getVersionString() + "'," //
					+ "UpTime=" + uptime + ",";

			String status = statusMessage.replace("'", "");

			if (status.length() > MAX_STATUS_MESSAGE_SIZE) {
				String extra = " ... Unexpected excess message length of " + status.length()
						+ ".\nThis message has been truncated.";
				status = status.substring(0, MAX_STATUS_MESSAGE_SIZE - extra.length() - 2) + extra;
			}

			query += "Status='" + status + "' WHERE MessagingServerID=" + myDB_ID;
			Connection connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {
					int numRows = stmt.executeUpdate(query);
					if (numRows == 0 || numRows > 1) {
						println(getClass(),
								" Problem while updating status of Messaging Server: Expected to modify exactly one row, but  modified "
										+ numRows + " rows instead. SQL='" + query + "'");
					}
					stmt.close();
				} catch (Exception ex) {
					logger.warning(exceptionString(ex));
					// ex.printStackTrace();
				}
			}
		} catch (Exception e) {
			logger.warning(exceptionString(e));
		}
	}

	// Big STATIC block here, run before any class object are initialized.

	static {
		Connection connection = null;
		try {
			connection = ConnectionToDatabase.getDbConnection();
		} catch (DatabaseNotVisibleException e1) {
			e1.printStackTrace();
			System.err.println("\nNo connection to the Signage/Displays database.");
			System.exit(SIMPLE_RECOVERABLE_ERROR);
		}

		synchronized (connection) {
			// Use ARM to simplify these try blocks.
			try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {

				InetAddress ip = InetAddress.getLocalHost();
				String myIPName = ip.getCanonicalHostName().replace(".dhcp", "");

				String query = "SELECT MessagingServerID FROM MessagingServerStatus where IPName='" + myIPName + "'";

				try (ResultSet rs2 = stmt.executeQuery(query);) {
					if (rs2.first())
						while (!rs2.isAfterLast()) {
							try { // Move to first returned row (there can be more than one; not sure how to deal with that yet)
								myDB_ID = rs2.getInt("MessagingServerID");
								System.out.println("The messaging serverID is " + myDB_ID);
							} catch (Exception e) {
								e.printStackTrace();
								System.exit(UNRECOVERABLE_ERROR);
							}
							rs2.next();
						}
				}
			} catch (SQLException e) {
				e.printStackTrace();
				System.exit(-3);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(-4);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-5);
			}
		}
	}

	/**
	 * Start the messaging server
	 */
	public void start() {
		startPinger();
		startDBStatusThread();
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try {
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);

			// infinite loop to wait for connections
			while (keepGoing) {
				// format message saying we are waiting
				logger.fine("Waiting for Clients on port " + port + ".");
				// ClientThread aNewClientThread = null;
				try {
					Socket socket = serverSocket.accept(); // accept connection if I was asked to stop

					final ClientThread aNewClientThread = new ClientThread(socket); // make the thread object for this potential
																					// client

					new Thread("InitializeClientThread") {
						public void run() {

							// Finish the initialization for this potential client
							aNewClientThread.initialize();

							if (aNewClientThread.username != null) {
								if (listOfMessagingClients.add(aNewClientThread)) { // save it in the ArrayList
									try {
										String subject = aNewClientThread.username.substring(0,
												aNewClientThread.username.length() - ("_" + aNewClientThread.id).length());
										ClientThreadList ctl = subjectListeners.get(subject);
										if (ctl == null) {
											ctl = new ClientThreadList();
											subjectListeners.put(subject, ctl);
										}
										ctl.add(aNewClientThread);
										numConnectionsSeen--; // Duplication of this parameter within the class ClientThreadList.
																// oops

										// START THE THREAD to listen to this client
										aNewClientThread.start();

									} catch (Exception e) {
										logger.warning(getClass().getSimpleName() + "\n" + exceptionString(e) + "\n"
												+ " - Unexpected exception in messaging server \n\nTHIS SHOULD BE INVESTIGATED AND FIXED.\n\n");
									}
								} else {

									// This code is never reached since the add() basically never fails (10/21/15)

									logger.fine("Error! Duplicate username requested, '" + aNewClientThread.username + "'");
									showAllClientsConnectedNow();
									aNewClientThread.start();
									aNewClientThread.writeUnsignedMsg(MessageCarrierXML.getErrorMessage(
											SPECIAL_SERVER_MESSAGE_USERNAME, aNewClientThread.username, "A client with the name "
													+ aNewClientThread.username + " has already connected.  Disconnecting now."));
									numDuplicateClients++;
									aNewClientThread.close();

									// We should probably remove BOTH clients from the permanent list. There are two cases seen so
									// far where this happens:

									// 1. The user tries to start a second ChannelSelector from the same place;

									// 2. Something I don't understand happens to a client and (a) we don't drop it here and (b) it
									// tries to reconnect. In both cases, the real client will eventually try to reconnect.

									int index = 0;
									for (ClientThread CT : listOfMessagingClients) {
										if (CT == null || CT.username == null) {
											logger.fine(
													"start(): Unexpected null ClientThread at index=" + index + ": [" + CT + "]");
											remove(CT); // How did THIS happen??
											numRemovedNullUsername++;
										} else if (CT.username.equals(aNewClientThread.username)) {
											logger.fine(
													"start(): Removing duplicate client " + CT.username + " from the client list");
											remove(CT); // Duplicate username is not allowed!
											numRemovedDuplicateUsername++;
										}
										index++;
									}

									// continue; No, I think (now) that fall through is appropriate.
								}
							} else {
								numPortScannerAttempts++;
								logger.warning(getClass().getSimpleName()
										+ " - A client attempted to connect, but the username it presented is null."
										+ "\n\t\tThis usually means that a port scanner has tried to connect.");
								// This null check is not necessary since this is the else clause of if(t.username!=null).
								// But it makes me feel better.
								if (aNewClientThread != null)
									aNewClientThread.close(); // Close it
								// aNewClientThread = null; // Forget it
								// continue;
							}

							// When this thread exits, either the ClientThread object is for a valid client and it stored, or it is
							// forgotten and garbage collected.

						}
					}.start();
				} catch (Exception e) {
					logger.warning(getClass().getSimpleName() + " - Unexpected exception when listening to port " + port
							+ ".  Let's try again.\n" + exceptionString(e));
					continue;
				}

				if (showClientList == null) {

					// This block is to print out the current set of clients. Rather than printing it out every time
					// a client connects, we wait 3 seconds from the first connect to do this. Usually, a lot of clients
					// connect at the same time--either the Channel Selector or when all the Displays reboot.

					showClientList = new Thread("ShowClientList") {
						long WAIT_FOR_ALL_TO_CONNECT_TIME = 4000L;

						public void run() {
							try {
								catchSleep(WAIT_FOR_ALL_TO_CONNECT_TIME);
								showAllClientsConnectedNow();
								performDiagnostics(true);
							} catch (Exception e) {
								logger.warning(exceptionString(e));
							}
							showClientList = null;
						}
					};
					showClientList.start();
					// } else { not really necessary to show this print statement every time!
					// logger.fine("Already waiting to show the client list");
				}
			}
			// I was asked to stop
			logger.fine("Closing the server port");
			try {
				// if (serverSocket != null) Compiler says it cannot be null here
				serverSocket.close();
				for (ClientThread tc : listOfMessagingClients) {
					try {
						if (tc != null) {
							if (tc.sInput != null)
								tc.sInput.close();
							if (tc.sOutput != null)
								tc.sOutput.close();
							if (tc.socket != null)
								tc.socket.close();
						}
					} catch (Exception ee) {
						// not much I can do
						logger.warning(exceptionString(ee));
					}
				}
			} catch (Exception e) {
				logger.warning("Exception closing the server and clients: " + e + "\n" + exceptionString(e));
			}
		}
		// something went wrong
		catch (IOException e) {
			logger.warning("IOException on new ServerSocket: " + e + "\n" + exceptionString(e));
		} catch (Exception e) {
			logger.warning("Exception on new ServerSocket: " + e + "\n" + exceptionString(e));
		}
		logger.warning(getClass().getSimpleName() + " - Exiting SERVER thread for listening to the messaging socket on port " + port
				+ ".  THIS SHOULD ONLY HAPPEN when the program exits.");
	}

	private void startDBStatusThread() {
		new Thread("DBStatusUpdate") {
			public void run() {
				while (true) {
					catchSleep(15 * ONE_SECOND);
					try {
						String m = "";
						if (listOfMessagingClients.size() == 0) {
							m = "No clients are connected.\n";
						} else {
							m = listOfMessagingClients.size() + " clients:\n";
							String un = "Name=";
							String ip = "Address=";
							String idn = "Local ID=";
							for (ClientThread CT : listOfMessagingClients) {
								m += "[" + un + CT.username.replace(".fnal.gov", "") + "|" + ip + CT.getRemoteIPAddress() + "|"
										+ idn + CT.id + "]\n";
								un = ip = idn = "";
							}
						}
						String stats = "\nNum Subjects: " + subjectListeners.size() + ".\nOther stats: "
								+ numRemovedExitedForeverLoop + ", " + numRemovedForPings1 + ", " + numberOfServerMessagesReceived
								+ ", " + numUnexpectedClosedSockets1 + ", " + numUnexpectedClosedSockets2 + ", "
								+ numDuplicateClients + " (" + numClosedCallsSeen + "), " + numClientsPutOnNotice + ", "
								+ numRemovedBadWriteSeen + ", " + numRemovedNullClientThread + ", " + numRemovedNullUsername + ", "
								+ numRemovedNullDate + ", " + numRemovedDuplicateUsername + ", " + numLogoutsSeen + ", "
								+ numPortScannerAttempts;

						if (m.length() + stats.length() > MAX_STATUS_MESSAGE_SIZE) {
							String extra = " (exceeds " + MAX_STATUS_MESSAGE_SIZE + " bytes)";
							m = m.substring(0, MAX_STATUS_MESSAGE_SIZE - stats.length() - extra.length() - 2) + extra;
						}
						updateStatus(m + stats);
					} catch (Exception e) {
						logger.warning("Exception in DB status update thread: " + e + "\n" + exceptionString(e));
					}
				} // end of forever loop.
			}
		}.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					updateStatus("** OFF LINE **");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		});
	}

	protected void showAllClientsConnectedNow() {
		String m = "List of connected clients:\n";
		for (ClientThread CT : listOfMessagingClients) {
			String p = CT.username + " (" + CT.getRemoteIPAddress() + ")";
			if (p.length() < 40)
				p += "\t";
			m += "\t" + p + "\tLast seen " + sdf.format(new Date(CT.getLastSeen())) + " ID=" + CT.id + "\n";
		}
		logger.fine(m);
	}

	private ClientThread lastOldestClientName = null;

	/**
	 * Ping each client from time to time to assure that it is (still) alive
	 */
	private void startPinger() {
		new Thread("Pinger") {
			private int		nextClient			= 0;
			private long	lastPrint			= System.currentTimeMillis();
			private long	diagnosticPeriod	= 2 * ONE_MINUTE;

			public void run() {
				catchSleep(15000L); // Wait a bit before starting the pinging and the diagnostics
				int counter = 0;

				while (keepGoing)
					try {
						catchSleep(sleepPeriodBtwPings); // Defaults: 60 seconds is "too old"; Two seconds between pings

						if (listOfMessagingClients.size() == 0)
							continue;

						// This sleep period assures that each client is ping'ed at least three times before it is "too old"
						sleepPeriodBtwPings = Math.min(tooOldTime / listOfMessagingClients.size() / 3L, 10000L);
						// Note: For 50 clients, the sleep is 400 msec.

						if (sleepPeriodBtwPings < 100L) { // Idiot check
							if (sleepPeriodBtwPings <= 1L)
								logger.fine(
										"DANGER: The time between pings in 'startPinger()' is 1 msec, probably because there are "
												+ listOfMessagingClients.size() + " clients.  This is new, untested territory!");
							else
								logger.fine("CAUTION: The time between pings in 'startPinger()' is rather short: "
										+ sleepPeriodBtwPings + " msec");
						}

						/*
						 * This boolean turns on the diagnostic message (in ClientThread.run()) that echos when a client says
						 * "I am alive". Turn it on for two minutes, just before the diagnostic is printed, which would show about
						 * 60 such messages (at a frequency of 0.5Hz). today, there are ~40 clients connected, so we'll see them all
						 */
						showAliveMessages = (lastPrint + 13 * ONE_MINUTE) < System.currentTimeMillis();

						// Figure out which client(s) to ping
						List<ClientThread> clist = new ArrayList<ClientThread>();

						long oldestTime = System.currentTimeMillis();
						ClientThread oldestClientName = null;
						// Ping any client that is "on notice", plus the oldest one that is not on notice. 200 is about 9 minutes;
						// 500 is about 22 minutes
						boolean printMe = (++counter % (diagnosticPeriod / sleepPeriodBtwPings)) < 2;
						String diagnostic = "Diagnostic update\n---- Cycle no. " + counter;

						int i = 0;
						for (ClientThread CT : listOfMessagingClients) {
							i++;
							if (printMe || CT.numOutstandingPings > 1) {
								String firstPart = "---- " + i + " " + CT.username.replace(".fnal.gov", "") + " ";
								int initialLength = firstPart.length();
								for (int m = initialLength; m < 40; m++)
									firstPart += ' ';
								diagnostic += "\n" + (firstPart + (new Date(CT.lastSeen)).toString().substring(4, 19) + ", "
										+ (System.currentTimeMillis() - CT.lastSeen) / 1000L + " secs ago"
										+ (CT.numOutstandingPings > 0 ? "; num pings=" + CT.numOutstandingPings : ""));
							}
							if (CT.isOnNotice())
								clist.add(CT);
							else if (CT.getLastSeen() < oldestTime && !CT.equals(lastOldestClientName)) {
								oldestTime = CT.getLastSeen();
								oldestClientName = CT;
							}
						}

						if (printMe)
							logger.fine(diagnostic);

						clist.add(oldestClientName);
						lastOldestClientName = oldestClientName;

						for (ClientThread CT : clist) {
							if (CT == null)
								continue;
							catchSleep(1);
							if (printMe)
								logger.fine("Sending isAlive message to " + CT);
							MessageCarrierXML mc = MessageCarrierXML.getIsAlive(SPECIAL_SERVER_MESSAGE_USERNAME, CT.username);
							if (CT.incrementNumOutstandingPings()) {
								logger.warning("Too many pings (" + CT.numOutstandingPings + ") to the client " + CT.username
										+ "; removing it!");
								remove(CT);
								CT.close();
								numRemovedForPings2++;
								CT = null; // Not really necessary, but it makes me feel better.
								continue;
							}
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
						logger.warning(exceptionString(e));
					}
			}
		}.start();
	}

	/*
	 * For the GUI to stop the server
	 */
	@SuppressWarnings("resource")
	protected void stop() {
		keepGoing = false;
		// connect to myself as Client to exit statement
		// Socket socket = serverSocket.accept();
		try {
			// Huh? (11/04/2014)
			new Socket("localhost", port);
		} catch (Exception e) {
			// nothing I can really do
			logger.warning(exceptionString(e));
		}
	}

	@Override
	public void javaHasChanged() {
		println(getClass(), "The Java version has changed.");
	}

}
