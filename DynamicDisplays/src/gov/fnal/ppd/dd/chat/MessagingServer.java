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
import static gov.fnal.ppd.dd.GlobalVariables.checkSignedMessages;
import static gov.fnal.ppd.dd.GlobalVariables.setLogger;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.launchMemoryWatcher;
import static gov.fnal.ppd.dd.util.Util.println;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SignedObject;
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
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.util.ObjectSigning;
import gov.fnal.ppd.dd.util.version.VersionInformation;

/**
 * The server that can be run both as a console application or a GUI
 * 
 * TODO: Improve the architecture
 * 
 * <p>
 * According to http://docs.oracle.com/cd/E19957-01/816-6024-10/apa.htm (the first hit on Google for "Messaging Server Architecture"
 * ), a well-built messaging server has these six components:
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
 * <li>String to; -- The name of the client who is supposed to get this message.
 * <em>This could be changed to a <em>"subscription topic."</em></li>
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
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 */
public class MessagingServer {

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
		int					numOutstandingPings	= 0;

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
		private InetAddress	theIPAddress;
		private Thread		myShutdownHook;

		// Constructor
		ClientThread(Socket socket) {
			super("ClientThread_of_MessagingServer_" + MessagingServer.uniqueId);
			this.myShutdownHook = new Thread("ShutdownHook_of_MessagingServer_" + MessagingServer.uniqueId) {
				public void run() {
					close(true);
				}
			};
			Runtime.getRuntime().addShutdownHook(this.myShutdownHook);
			// a unique id
			this.id = MessagingServer.uniqueId.getAndIncrement();
			this.socket = socket;

			theIPAddress = socket.getInetAddress();

			Object read = null;
			/* Creating both Data Stream */
			// System.out.println("Thread trying to create Object Input/Output Streams");
			try {
				// create output first
				/*
				 * FIXME ??? --- On June 2, 2015, 15:16:33 (approximately) I connected a display server with a mixed-up IP address.
				 * Its internal reckoning of its IP address was different from what DNS said. I was able to connect to the server
				 * (and this is the spot where that connection happens on the server), and everything seemed to work. However, at
				 * that precise moment, the messaging server lost track of ALL of its clients, and all the clients lost track of the
				 * server. I do not see how this could have happened, unless it is something within the "socket" mechanism of Java.
				 * So, future self, be warned that this could happen again and foul everything up.
				 */

				logger.fine("Starting client at uniqueID=" + id + " connected to " + theIPAddress);
				this.sOutput = new ObjectOutputStream(socket.getOutputStream());
				this.sInput = new ObjectInputStream(socket.getInputStream());

				// read the username -- the first message from the new connection should be a login message
				read = this.sInput.readObject();
				if (read instanceof MessageCarrier) {
					if (((MessageCarrier) read).getType() != MessageType.LOGIN) {
						logger.warning("Expected LOGIN message from " + ((MessageCarrier) read).getMessage() + ", but got a "
								+ ((MessageCarrier) read).getType() + " message");
						return;
					}
					String un = ((MessageCarrier) read).getMessage();
					this.username = un + "_" + id;
					logger.fine("'" + this.username + "' has connected.");
					setName("ClientThread_of_MessagingServer_" + username);
				} else {
					logger.warning("Unexpected response from client '" + ((MessageCarrier) read).getFrom() + ": Type="
							+ read.getClass().getCanonicalName());
					return;
				}
			} catch (IOException e) {
				// Usually, it seems we fall here when something tries to connect to us but is not one of the Java clients.
				// At Fermilab, this happens when this port gets scanned.
				logger.warning(
						"Exception creating new Input/output streams on socket (" + socket + ") due to this exception: " + e);
				return;
			} catch (Exception e) {
				if (read == null) {
					logger.warning("Expecting a String but got nothing (null)");
				} else
					logger.warning("Expecting a String but got a " + read.getClass().getSimpleName() + " [" + read + "]");
				logger.warning(exceptionString(e));
			}
			this.date = new Date().toString();

		}

		// try to close everything
		protected void close() {
			close(false);
		}

		protected void close(boolean duringShutdown) {
			try {
				logger.fine("Closing client " + username);
				thisSocketIsActive = false;
				numClosedCallsSeen++;
				try {
					if (this.sOutput != null)
						this.sOutput.close();
				} catch (Exception e) {
					logger.warning(exceptionString(e));
				}
				try {
					if (this.sInput != null)
						this.sInput.close();
				} catch (Exception e) {
					logger.warning(exceptionString(e));
				}
				try {
					if (this.socket != null)
						this.socket.close();
				} catch (Exception e) {
					logger.warning(exceptionString(e));
				}
				this.sInput = null;
				this.sOutput = null;
				this.socket = null;

				if (!duringShutdown && this.myShutdownHook != null) {
					Runtime.getRuntime().removeShutdownHook(this.myShutdownHook);
					this.myShutdownHook = null;
				}
			} catch (Exception e) {
				logger.warning(exceptionString(e));
			}
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
		 * This is the heart of the messaging server: The place where all of the incoming messages are received and processed. each
		 * instance of this inner class corresponds, one-to-one, to a specific client.
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

			// TODO -- Streaming Java objects is great and all, but when the class definition changes, one has to update the
			// clients and this server at the same time. When we change to streamed XML documents, this will go away.

			this.thisSocketIsActive = true;
			while (this.thisSocketIsActive) {
				// Create the read Object here so it can be seen by the catch blocks
				Object resetObj = new Object() {
					public String toString() {
						return "An empty object.";
					}
				};
				Object read = null;
				try {
					read = resetObj;
					read = this.sInput.readObject();
					setLastSeen();

					// Make all the other incoming messages wait until the processing here is done.
					if (read instanceof MessageCarrier) {
						this.cmSigned = null;
						this.cm = (MessageCarrier) read;

						// Idiot check: (Rarely seen, and never seen if the "username" is not null)
						if (!MessageCarrier.isUsernameMatch(username, cm.getFrom()))
							logger.warning("Oops (a).  My assumption is wrong.  This thread is for '" + username
									+ "' and the from field on the message is '" + cm.getFrom() + "'");

					} else if (read instanceof SignedObject) {
						this.cmSigned = (SignedObject) read;
						this.cm = (MessageCarrier) cmSigned.getObject();
						// Idiot check: (Rarely seen, and never seen if the "username" is not null)
						if (!MessageCarrier.isUsernameMatch(username, cm.getFrom()))
							logger.warning("Oops (b).  My assumption is wrong.  This thread is for '" + username
									+ "' and the from field on the message is '" + cm.getFrom() + "'");

						String signatureString = this.cm.verifySignedObject(cmSigned);
						if (signatureString == null)
							logger.warning("Message is properly signed: " + this.cm);
						else {
							logger.warning("Message is NOT PROPERLY SIGNED: [" + this.cm + "]; reason = '" + signatureString
									+ "' -- ignoring this message and sending an error message back to the client.");

							// Reply to the client that this message was rejected!
							writeUnsignedMsg(MessageCarrier.getErrorMessage(SPECIAL_SERVER_MESSAGE_USERNAME, this.cm.getFrom(),
									"The message to display '" + this.cm.getTo()
											+ "' does not have a correct cryptographic signature; it has been rejected."));
							continue;
						}
					} else if (read instanceof MessageType) {
						// Not seen as of 3/2017
						logger.warning(this.username + ": Unexpectedly received message of type MessageType, value='" + read + "'");
						continue;
					} else {
						logger.warning(this.username + ": An object of type " + read.getClass().getCanonicalName()
								+ " received; message is -- [" + read + "].\n\t\t**Ignored**\n" + exceptionString(
										new Exception("unexpected object seen of type " + read.getClass().getCanonicalName())));
						break; // End the while(this.thisSocketIsActive) loop
					}
				} catch (SocketException | EOFException e) {
					String theType = read.getClass().getCanonicalName();
					if (resetObj.toString().equals(read.toString())) {
						logger.warning("Client " + this.username + ": " + e.getClass().getName()
								+ " reading input stream\n          Received an empty object. " + exceptionString(e));
						break;
					}
					String dis = "Client " + this.username + ": " + e.getClass().getName() + " reading input stream\n          "
							+ "The received message was '" + read + "'\n          (type=" + theType + ") ";
					logger.warning(dis + exceptionString(e));
					break;
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
							frm = this.cm.getFrom();
							typ = this.cm.getType().toString();
							tto = this.cm.getTo();
						}
						// Reply to the client that this message was rejected!
						String errorMessage = "Your message of type " + typ + " to client, " + tto
								+ " was not processed because there was an internal error in the messaging server; exception type is "
								+ e.getClass().getCanonicalName();
						logger.fine("Sending error message to client: [[[" + errorMessage + "]]]");
						if (!frm.equals("null"))
							writeUnsignedMsg(MessageCarrier.getErrorMessage(SPECIAL_SERVER_MESSAGE_USERNAME, frm, errorMessage));
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

				// The client that sent this message is still alive *** This is the wrong way to view this!!! (10/20/15) ***
				//
				// markClientAsSeen(this.cm.getFrom());
				// markClientAsSeen(username);
				//
				// What is really going on here is that this thread is uniquely connected to a specific client (remember, this
				// thread runs as part of the messaging server). So when this thread gets a message, that means that this client is
				// alive (and nothing else.)
				//

				totalMesssagesHandled++;

				if (this.cm.getType() == MessageType.AMALIVE && SPECIAL_SERVER_MESSAGE_USERNAME.equals(this.cm.getTo())) {
					if (showAliveMessages)
						logger.fine("Alive msg: " + cm.getFrom().trim().replace(".fnal.gov", ""));
					continue; // That's all for this while-loop iteration. Go read the socket again...
				}

				if (showAllIncomingMessages)
					logger.fine("MessagingServer.ClientThread: Got message: " + cm);

				// Switch for the type of message receive
				switch (this.cm.getType()) {

				case ISALIVE:
				case AMALIVE:
				case ERROR:
				case REPLY:
					broadcast(this.cm);
					break;

				case MESSAGE:
					//
					// The message, received from a client, is relayed here to the client of its choosing
					// (unless that client is not authorized)
					//
					if (isAuthorized(this.cm.getFrom(), this.cm.getTo())) {
						broadcast(this);
						// broadcast(this.cmSigned);
					} else {
						logger.warning("Message rejected!  '" + this.username + "' asked to send message of type "
								+ this.cm.getType() + " to '" + this.cm.getTo()
								+ "'\n\t\tbut it is not authorized to send a message to this client");

						// Reply to the client that this message was rejected!
						writeUnsignedMsg(MessageCarrier.getErrorMessage(SPECIAL_SERVER_MESSAGE_USERNAME, this.cm.getFrom(),
								"You are not authorized to change the channel on the display called " + this.cm.getTo()
										+ ".\nThis directive has been rejected."));
					}

					break;

				// ---------- Other types of messages are interpreted here by the server. ---------
				case LOGOUT:
					logger.fine(this.username + " disconnected with a LOGOUT message.");
					this.thisSocketIsActive = false;
					numLogoutsSeen++;
					break;

				case WHOISIN:
					// writeMsg("WHOISIN List of the users connected at " + sdf.format(new Date()) + "\n");
					// scan all the users connected
					boolean purge = false;
					// The iterator (implicitly used here) makes a thread-safe copy of the list prior to traversing it.
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

							writeUnsignedMsg(MessageCarrier.getIAmAlive(ct.username, this.cm.getFrom(), "since " + ct.date));
						} else {
							logger.fine("Talking to " + this.username + " socket " + this.socket.getLocalAddress()
									+ ". Error!  Have a null client");
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
					break;

				case EMERGENCY:
					if (isAuthorized(this.cm.getFrom(), this.cm.getTo()) && isClientAnEmergencySource(this.cm.getFrom())) {
						broadcast(this);
						// broadcast(this.cmSigned);
					} else {
						String mess = "Message rejected!  '" + this.username + "' asked to send message of type "
								+ this.cm.getType() + " to '" + this.cm.getTo() + "'\n\t\tbut ";
						if (isClientAnEmergencySource(this.cm.getFrom())) {
							mess += "it is not authorized to send any sort of message to this client";

						} else {
							mess += "it is not authorized to generate an emergency message";
						}
						logger.warning(mess);

						// Reply to the client that this message was rejected!
						writeUnsignedMsg(MessageCarrier.getErrorMessage(SPECIAL_SERVER_MESSAGE_USERNAME, this.cm.getFrom(),
								"You are not authorized to put an emergency message on the display called " + this.cm.getTo()
										+ ".\nThis directive has been rejected."));
					}
					break;

				case SUBSCRIBE:
					String subject = this.cm.getMessage();
					ClientThreadList ctl = subjectListeners.get(subject);
					if (ctl == null) {
						ctl = new ClientThreadList();
						subjectListeners.put(subject, ctl);
					}
					ctl.add(this);
					break;

				case LOGIN:
					// Not relevant here, but included for completeness.
					break;
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

			logger.fine(this.getClass().getSimpleName() + ": No. remaining clients: " + listOfMessagingClients.size());
			close();
		}

		@Override
		public String toString() {
			return this.username + ", socket=[" + socket + "], date=" + date;
		}

		/**
		 * Write an unsigned object to the Client output stream
		 */
		boolean writeUnsignedMsg(MessageCarrier msg) {
			// System.out.println(new Date() + " " + getClass().getSimpleName() + ".writeUnsignedMsg() for " + username
			// + ": message is [" + msg + "]");
			if (socket == null) {
				logger.warning("Socket object is null--cannot send the message " + msg);
				return false;
			}
			// if Client is still connected send the message to it
			if (!socket.isConnected() || socket.isClosed() || socket.isOutputShutdown() || socket.isInputShutdown()) {
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
				// System.out.println(" Writing uinsigned message to client: " + msg);
				sOutput.writeObject(msg);
				sOutput.reset();
				return true;
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				logger.warning("IOException sending message to " + this.username + "\n" + e + "\n" + exceptionString(e));
			} catch (Exception e) {
				logger.warning(e.getLocalizedMessage() + ", Error sending message to " + this.username + "\n" + exceptionString(e));
			}
			logger.warning("Position 3 (failure)");
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
				logger.warning("IOException sending signed message to " + this.username + "\n" + e + "\n" + exceptionString(e));
			} catch (Exception e) {
				logger.warning(e.getLocalizedMessage() + ", Error sending signed message to " + this.username + e + "\n"
						+ exceptionString(e));
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
			if (!checkSignedMessages() || cm.getType().isReadOnly())
				return true;
			if (cmSigned != null)
				return ObjectSigning.isClientAuthorized(from, to);

			return false;
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
				logger.fine("Adding a client named " + ct.username + " gives an error from CopyOnWriteArrayList.add()");
			}
			return retval;
		}

	}

	/**
	 * The name we use for the messaging server, when it is sending a message out
	 */
	public static final String					SPECIAL_SERVER_MESSAGE_USERNAME	= "Server Message";

	private static int							myDB_ID							= 0;

	/*
	 * ************************************************************************************************************************
	 * Begin the definition of the class MessagingServer
	 * 
	 * Static stuff
	 */

	// a unique ID for each connection. It is atomic just in case two ask to connect at the same time (we have never seen this).
	static AtomicInteger						uniqueId						= new AtomicInteger(0);

	// private void markClientAsSeen(String from) {
	// for (ClientThread CT : listOfMessagingClients)
	// if (from.equals(CT.username) || CT.username.startsWith(from) || from.startsWith(CT.username)) {
	// logger.fine("Marking client " + from + "/" + CT.username + " as seen");
	// CT.setLastSeen();
	// }
	// }

	// an ArrayList to keep the list of all the Client
	ClientThreadList							listOfMessagingClients;

	ConcurrentHashMap<String, ClientThreadList>	subjectListeners;

	// A hash of the most recent message published on each subject
	ConcurrentHashMap<String, MessageCarrier>	lastMessage;

	// A synchronization object
	private Object								broadcasting					= "Object 1 for Java synchronization";
	// the boolean that will be turned of to stop the server
	private boolean								keepGoing;

	private int									numConnectionsSeen				= 0;
	// the port number to listen for connection
	private int									port;

	// to display time (Also used as a synchronization object for writing messages to the local terminal/GUI
	protected SimpleDateFormat					sdf;
	private Thread								showClientList					= null;
	private long								startTime						= System.currentTimeMillis();

	private long								sleepPeriodBtwPings				= 2 * ONE_SECOND;
	// "Too Old Time" is one minute
	private long								tooOldTime						= ONE_MINUTE;

	protected int								totalMesssagesHandled			= 0;

	private int									numRemovedForPings				= 0;
	private int									numClientsPutOnNotice			= 0;
	public int									numRemovedBadWriteSeen			= 0;
	public int									numRemovedNullClientThread		= 0;
	public int									numRemovedNullUsername			= 0;
	public int									numRemovedNullDate				= 0;
	public int									numRemovedExitedForeverLoop		= 0;
	private int									numRemovedDuplicateUsername		= 0;
	private int									numClosedCallsSeen				= 0;
	private int									numLogoutsSeen					= 0;

	// private Object oneMessageAtATime = new String("For synchronization");

	protected Logger							logger;

	/**
	 * server constructor that receive the port to listen to for connection as parameter in console
	 * 
	 * @param port
	 */
	public MessagingServer(int port) {
		this.port = port;
		try {
			logger = Logger.getLogger(MessagingServer.class.getName());

			FileHandler fileTxt = new FileHandler("../../log/messagingServer.log");
			SimpleFormatter sfh = new SimpleFormatter();
			fileTxt.setFormatter(sfh);
			logger.addHandler(fileTxt);
			logger.setLevel(Level.FINER);

			setLogger(logger);

		} catch (Exception e) {
			logger.warning(exceptionString(e));
		}

		sdf = new SimpleDateFormat("dd MMM HH:mm:ss");

		// ArrayList of the Clients
		listOfMessagingClients = new ClientThreadList();
		subjectListeners = new ConcurrentHashMap<String, ClientThreadList>();
		lastMessage = new ConcurrentHashMap<String, MessageCarrier>();

		launchMemoryWatcher(logger);
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
	 * @param mc
	 *            The message to broadcast
	 */
	protected void broadcast(MessageCarrier mc) {
		synchronized (broadcasting) { // Only send one message at a time

			String subject = mc.getTo();
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
			if (mc.getType() == MessageType.MESSAGE) {
				lastMessage.put(subject, mc);
			}

			ClientThreadList ctl = subjectListeners.get(subject);
			if (ctl != null) {
				// Write this message to this client
				for (ClientThread ct : ctl)
					if (MessageCarrier.isUsernameMatch(mc.getTo(), ct.username))
						if (!ct.writeUnsignedMsg(mc)) {
							remove(ct);
							logger.warning(getClass().getSimpleName() + " - broadcast() FAILED.  mc=" + mc);
							numRemovedBadWriteSeen++;
						}
			} else {
				logger.warning(getClass().getSimpleName() + " -  Hmm.  We have an unsigned message on '" + subject
						+ "' but the list of interested clients is empty");
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
				String subject = mc.getTo();
				for (int m = subject.length() - 1; m >= 0; m--) {
					if (subject.charAt(m) == '_') { // Find the trailing "_index" and remove it
						subject = subject.substring(0, m);
						break;
					}
				}
				if (mc.getType() == MessageType.MESSAGE) {
					lastMessage.put(subject, mc);
				}

				ClientThreadList ctl = subjectListeners.get(subject);
				if (ctl != null) {
					// would write this message to this client
					for (ClientThread ct : ctl)
						if (MessageCarrier.isUsernameMatch(ct.username, mc.getTo())) {
							if (!ct.writeMsg(message)) {
								remove(ct);
								logger.fine("Disconnected Client " + ct.username + " removed from list.");
								numRemovedBadWriteSeen++;
							}
						}
				} else {
					logger.warning(getClass().getSimpleName() + " - Hmm.  We have an SIGNED message on '" + subject
							+ "' but the list of interested clients is empty");
				}

			}
		} catch (ClassNotFoundException | IOException e) {
			logger.warning(exceptionString(e));
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
		String retval = (e.getMessage() == null ? "" : e.getMessage()) + "\n\t";
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
					numRemovedForPings++;
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

			String subjectInfo = "*There are " + subjectListeners.size() + " subjects*  They are:\n";
			String LL = " listeners";
			String NN = "no";
			for (String subject : subjectListeners.keySet()) {
				ClientThreadList ctl = subjectListeners.get(subject);
				subjectInfo += "     " + subject + "\t[" + (ctl == null ? NN : ctl.size()) + LL + "]\n";
				LL = "";
				NN = "none";
			}
			String spaces = "XZXZxzxz";
			String spacesForLog = "\n                       ";
			// String spacesForStatus = " - ";
			String message = MessagingServer.class.getSimpleName() + " has been alive " + days + " d " + hours + " hr " + mins
					+ " min " + secs + " sec (" + aliveTime + " msec)" //
					+ spaces + numConnectionsSeen + " connections accepted, " + listOfMessagingClients.size() + " client"
					+ (listOfMessagingClients.size() != 1 ? "s" : "") + " connected right now, " + totalMesssagesHandled
					+ " messages handled" //
					+ spaces + "Oldest client is " + oldestName //
					+ spaces + "No. clients put 'on notice': " + numClientsPutOnNotice //
					+ spaces + "No. clients removed due to lack of response: " + numRemovedForPings //
					+ spaces + "No. clients removed for 'bad write': + numRemovedBadWriteSeen	" //
					+ spaces + "No. clients removed for 'null client thread': " + numRemovedNullClientThread //
					+ spaces + "No. clients removed for null username: " + numRemovedNullUsername //
					+ spaces + "No. clients removed for null date: " + numRemovedNullDate //
					+ spaces + "No. clients removed by exiting forever loop: " + numRemovedExitedForeverLoop //
					+ spaces + "No. clients removed for duplicate name: " + numRemovedDuplicateUsername //
					+ "\n" + subjectInfo;
			logger.fine("\n                       " + message.replace(spaces, spacesForLog));
			// updateStatus(message.replace(spaces, spacesForStatus));
		}
	}

	private VersionInformation versionInfo = VersionInformation.getVersionInformation();

	private synchronized void updateStatus(String statusMessage) {
		SimpleDateFormat sqlFormat = new SimpleDateFormat("YYYY/MM/dd HH:mm:ss");

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
					ex.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static {
		Connection connection = null;
		try {
			connection = ConnectionToDatabase.getDbConnection();
		} catch (DatabaseNotVisibleException e1) {
			e1.printStackTrace();
			System.err.println("\nNo connection to the Signage/Displays database.");
			System.exit(-1);
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
								System.exit(-2);
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
				ClientThread t = null;
				try {
					Socket socket = serverSocket.accept(); // accept connection if I was asked to stop
					// if (!keepGoing) break;
					t = new ClientThread(socket); // make a thread of it
				} catch (Exception e) {
					logger.warning(getClass().getSimpleName() + " - Unexpected exception when listening to port " + port
							+ ".  Let's try again.\n" + exceptionString(e));
					continue;
				}
				if (t.username != null) {
					if (listOfMessagingClients.add(t)) { // save it in the ArrayList
						try {
							// Interim subject, equal to the client name without the appended id number.
							String subject = t.username.substring(0, t.username.length() - ("_" + t.id).length());
							ClientThreadList ctl = subjectListeners.get(subject);
							if (ctl == null) {
								ctl = new ClientThreadList();
								subjectListeners.put(subject, ctl);
							}
							ctl.add(t);
							t.start();
						} catch (Exception e) {
							logger.warning(getClass().getSimpleName() + "\n" + exceptionString(e) + "\n"
									+ " - Unexpected exception in messaging server \n\nTHIS SHOULD BE INVESTIGATED AND FIXED.\n\n");
						}
					} else {

						// This code is never reached, I think (10/21/15)

						logger.fine("Error! Duplicate username requested, '" + t.username + "'");
						showAllClientsConnectedNow();
						t.start();
						t.writeUnsignedMsg(MessageCarrier.getErrorMessage(SPECIAL_SERVER_MESSAGE_USERNAME, t.username,
								"A client with the name " + t.username + " has already connected.  Disconnecting now."));
						t.close();

						// We should probably remove BOTH clients from the permanent list. There are two cases seen so far
						// where this happens: 1. The user tries to start a second ChannelSelector from the same place; 2. Something
						// I don't understand happens to a client and (a) we don't drop it here and (b) it tries to reconnect. In
						// both cases, the real client will eventually try to reconnect.

						int index = 0;
						for (ClientThread CT : listOfMessagingClients) {
							if (CT == null || CT.username == null) {
								logger.fine("start(): Unexpected null ClientThread at index=" + index + ": [" + CT + "]");
								remove(CT); // How did THIS happen??
								numRemovedNullUsername++;
							} else if (CT.username.equals(t.username)) {
								logger.fine("start(): Removing duplicate client " + CT.username + " from the client list");
								remove(CT); // Duplicate username is not allowed!
								numRemovedDuplicateUsername++;
							}
							index++;
						}

						// continue; No, I think (now) that fall through is appropriate.
					}
				} else {
					logger.warning(
							getClass().getSimpleName() + " - A client attempted to connect, but the username it presented is null."
									+ "/n/t/tThis usually means that a port scanner has tried to connect.");
					// This null check is not necessary since this is the else clause of if(t.username!=null).
					// But it makes me feel better.
					if (t != null)
						t.close(); // Close it
					t = null; // Forget it
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
								+ numRemovedExitedForeverLoop + ", " + numRemovedForPings + ", " + numClientsPutOnNotice + ", "
								+ numRemovedBadWriteSeen + ", " + numRemovedNullClientThread + ", " + numRemovedNullUsername + ", "
								+ numRemovedNullDate + ", " + numRemovedDuplicateUsername + ", " + numClosedCallsSeen + ", "
								+ numLogoutsSeen;

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
			m += "\t" + CT.username + " (" + CT.getRemoteIPAddress() + ")\tLast seen " + sdf.format(new Date(CT.getLastSeen()))
					+ " ID=" + CT.id + "\n";
		}
		logger.fine(m);
	}

	private ClientThread lastOldestClientName = null;

	/**
	 * Ping each client from time to time to assure that it is (still) alive
	 */
	private void startPinger() {
		new Thread("Pinger") {
			private int		nextClient	= 0;
			private long	lastPrint	= System.currentTimeMillis();

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
						boolean printMe = (++counter % 200) < 2;
						if (printMe)
							logger.fine("---- Cycle no. " + counter);

						int i = 0;
						for (ClientThread CT : listOfMessagingClients) {
							i++;
							if (printMe || CT.numOutstandingPings > 1) {
								String firstPart = "---- " + i + " " + CT.username.replace(".fnal.gov", "") + " ";
								int initialLength = firstPart.length();
								for (int m = initialLength; m < 40; m++)
									firstPart += ' ';
								logger.fine(firstPart + (new Date(CT.lastSeen)).toString().substring(4, 19) + ", "
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

						clist.add(oldestClientName);
						lastOldestClientName = oldestClientName;

						for (ClientThread CT : clist) {
							if (CT == null)
								continue;
							catchSleep(1);
							if (printMe)
								logger.fine("Sending isAlive message to " + CT);
							MessageCarrier mc = MessageCarrier.getIsAlive(SPECIAL_SERVER_MESSAGE_USERNAME, CT.username);
							if (CT.incrementNumOutstandingPings()) {
								logger.warning("Too many pings (" + CT.numOutstandingPings + ") to the client " + CT.username
										+ "; removing it!");
								remove(CT);
								CT.close();
								numRemovedForPings++;
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

}
