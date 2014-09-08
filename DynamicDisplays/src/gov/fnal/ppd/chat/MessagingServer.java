package gov.fnal.ppd.chat;

import static gov.fnal.ppd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.signage.util.Util.launchMemoryWatcher;

import gov.fnal.ppd.signage.xml.HeartBeat;
import gov.fnal.ppd.signage.xml.MyXMLMarshaller;
import gov.fnal.ppd.signage.xml.Ping;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.bind.JAXBException;

/**
 * The server that can be run both as a console application or a GUI
 * 
 * Taken from http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/ on 5/12/2014
 */
public class MessagingServer {
	// a unique ID for each connection
	private static int							uniqueId;
	// an ArrayList to keep the list of the Client
	// private ArrayList<ClientThread> al; This Template is supposed to help with ConcurrentModificationException
	private CopyOnWriteArrayList<ClientThread>	al;

	// to display time (Also used as a synchronization object for writing messages to the local terminal/GUI
	protected SimpleDateFormat					sdf;
	// the port number to listen for connection
	private int									port;
	// the boolean that will be turned of to stop the server
	private boolean								keepGoing;
	private boolean								addTimeStamp	= false;

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
		al = new CopyOnWriteArrayList<ClientThread>() {
			private static final boolean	REQUIRE_UNIQUE_USERNAMES	= false;

			private static final long		serialVersionUID			= 2919140620801861217L;

			public boolean add(ClientThread ct) {
				if (ct == null)
					return false;
				if (REQUIRE_UNIQUE_USERNAMES)
					for (ClientThread CT : this)
						if (CT.username.equals(ct.username))
							return false; // Duplicate username is not allowed!

				// TODO -- Is a bad thing to have duplicate usernames? When/if the server directs messages to the intended user,
				// this WILL be necessary. But having a fairly anonymous clientelle works just fine.

				return super.add(ct);
			}
		};
		launchMemoryWatcher();
		new Thread("InternalCounts") {
			public void run() {
				while (true) {
					long time = FIFTEEN_MINUTES / 512;
					double sq2 = Math.sqrt(2.0);
					while (true) {
						try {
							sleep(time);
						} catch (InterruptedException e) {
						}
						if (time < FIFTEEN_MINUTES) {
							time *= sq2;
							if (time >= FIFTEEN_MINUTES) {
								time = FIFTEEN_MINUTES;
							}
						}
						System.out.println("Number of members of object al: " + al.size());
					}
				}
			}
		}.start();

		new Thread("PingAllUsers") {
			public void run() {
				while (true) {
					long sleepTime = 30000L; // Being by sending a heartbeat shortly after the server starts
					try {
						sleep(sleepTime);
						sleepTime = FIFTEEN_MINUTES; // Then once every 15 minutes after that
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					try {
						HeartBeat hb = new HeartBeat();
						hb.setTimeStamp(System.currentTimeMillis());
						String message = MyXMLMarshaller.getXML(hb);

						for (int i = 0; i < al.size(); ++i) {
							ClientThread ct = al.get(i);
							broadcast(ct.username + ": " + message);
							// System.out.println("Sent heartbeat to messaging client number " + i + " at " + new Date());

						}
					} catch (JAXBException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	/**
	 * Start the messaging server
	 */
	public void start() {
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try {
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);

			// infinite loop to wait for connections
			while (keepGoing) {
				// format message saying we are waiting
				display("Server waiting for Clients on port " + port + ".");

				Socket socket = serverSocket.accept(); // accept connection if I was asked to stop
				if (!keepGoing)
					break;
				ClientThread t = new ClientThread(socket); // make a thread of it
				if (al.add(t)) // save it in the ArrayList
					t.start();
				else
					display("Error! Duplicate username requested, '" + t.username + "'");
			}
			// I was asked to stop
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
		// something went bad
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
	protected synchronized void broadcast(String message) {
		// TODO (possibly) -- Be more selective about who gets this message
		String messageLf;
		if (addTimeStamp) {
			// add HH:mm:ss and \n to the message
			String time = sdf.format(new Date());
			messageLf = time + " " + message + "\n";
		} else {
			messageLf = message;
		}

		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for (int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			// try to write to the Client if it fails remove it from the list
			if (!ct.writeMsg(messageLf)) {
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
				// Observation, 9/8/2014: This never happens. If it does, this would probably lead to a "concurrent modification"
				// exception.
			}
		}
	}

	// for a client who logs off using the LOGOUT message
	private synchronized void remove(int id) {
		synchronized (al) {
			// scan the array list until we found the Id
			for (int i = 0; i < al.size(); ++i) {
				ClientThread ct = al.get(i);
				// found it
				if (ct.id == id) {
					al.remove(i);
					return;
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
		int portNumber = 1500;
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

	/** One instance of this thread will run for each client */
	class ClientThread extends Thread {
		// the socket where to listen/talk
		Socket				socket;
		ObjectInputStream	sInput;
		ObjectOutputStream	sOutput;
		// my unique id (easier for deconnection)
		int					id;
		// the Username of the Client
		String				username;
		// the only type of message a will receive
		MessageCarrier		cm;
		// the date I connect
		String				date;

		// Constructor
		ClientThread(Socket socket) {
			super("ClientThread_of_MessagingServer");
			// a unique id
			id = ++uniqueId;
			this.socket = socket;
			/* Creating both Data Stream */
			// System.out.println("Thread trying to create Object Input/Output Streams");
			try {
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String) sInput.readObject();
				display(username + " just connected.");
			} catch (IOException e) {
				display("Exception creating new Input/output streams on socket (" + socket + ") due to this exception: " + e);
				return;
			} catch (Exception e) {
				// have to catch ClassNotFoundException but I read a String, I am sure it will work
				e.printStackTrace();
			}
			date = new Date().toString() + "\n";
		}

		// what will run forever
		Object	read	= new Object();

		public void run() {
			// to loop until LOGOUT or we hit an unrecoverable exception
			boolean keepGoing = true;
			while (keepGoing) {
				// read a String (which is an object)
				try {
					read = sInput.readObject();
					cm = (MessageCarrier) read;
				} catch (EOFException e) {
					display(username + " disconnected -- " + e);
					break;
				} catch (IOException e) {
					display(username + ": An I/O Exception -- " + e);
					e.printStackTrace();
					break;
				} catch (ClassNotFoundException e) {
					display(username + ": A class not found exception -- " + e + ". returned object of type "
							+ read.getClass().getCanonicalName());
					e.printStackTrace();
					break;
				} catch (NullPointerException e) {
					display(username + ": Null pointer exception, attribute 'sInput' = '" + sInput + "'");
					e.printStackTrace();
				} catch (Exception e) {
					display(username + ": Exception reading Streams -- " + e + "; will try to continue.");
					e.printStackTrace();
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					continue; // Go to the next iteration of this loop.
				}
				// the message part of the ChatMessage
				String message = cm.getMessage();

				// Switch on the type of message receive
				switch (cm.getType()) {

				case MESSAGE:
					//
					// The message is broadcast here!
					//
					broadcast(username + ": " + message);
					break;

				case LOGOUT:
					display(username + " disconnected with a LOGOUT message.");
					keepGoing = false;
					break;

				case WHOISIN:
					// writeMsg("WHOISIN List of the users connected at " + sdf.format(new Date()) + "\n");
					// scan all the users connected
					boolean purge = false;
					for (int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						// TODO Make this into an XML document
						if (ct != null && ct.username != null && ct.date != null)
							writeMsg("WHOISIN [" + ct.username + "] since " + ct.date);
						else {
							// writeMsg("WHOISIN " + (i + 1) + ": Error!  Have a null client");
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
								display("Removing null ClientThread username [" + AL + "]");
								al.remove(AL);
							} else if (AL.date == null) {
								display("Removing null ClientThread date [" + AL + "]");
								al.remove(AL);
							}
						}
					break;
				}
			}
			// remove myself from the arrayList containing the list of the
			// connected Clients
			display("Have exited 'forever' loop (keepGoing=" + keepGoing + ") Removing client " + id + ", name='" + username + "'");
			remove(id);
			close();
		}

		// try to close everything
		private void close() {
			// try to close the connection
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
			;
			try {
				if (socket != null)
					socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if (!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}
	}
}
