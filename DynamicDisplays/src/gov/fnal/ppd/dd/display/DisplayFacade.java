package gov.fnal.ppd.dd.display;

import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.GlobalVariables.THIS_IP_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.THIS_IP_NAME_INSTANCE;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.chat.DCProtocol;
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.chat.MessagingClient;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.xml.ChangeChannel;
import gov.fnal.ppd.dd.xml.ChangeChannelList;
import gov.fnal.ppd.dd.xml.EncodedCarrier;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * On the computer with the channel selector, this is the way you talk to a remote Dynamic Display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2013
 * 
 */
public class DisplayFacade extends DisplayImpl {

	// private static final boolean USE_MESSAGING = true;
	// private static final boolean USE_CLIENT_SERVER = false;

	/**
	 * 
	 */
	public static boolean		tryToConnectToDisplaysNow	= false;
	/**
	 * 
	 */
	public static AtomicBoolean	alreadyWaiting				= new AtomicBoolean(false);
	private AtomicBoolean		ready						= new AtomicBoolean(false);
	private AtomicBoolean		waiting						= new AtomicBoolean(false);
	private String				myExpectedName;

	/**
	 * Internal client for handling the direct messaging connection to our Display. There is one connection (through the Messaging
	 * Server) and one instance of this private, inner class to each Display. That is, if there are ten Displays handled by the
	 * ChannelSelector GUI, there will be ten sockets (connected through the Messaging Server) within the GUI to the ten individual
	 * Displays.
	 * 
	 * TODO -- It might be necessary to reduce the number of messaging clients here on the GUI side; it seems like it would be
	 * possible to reduce it to a single client. The messages being used in this system contain a "to" field, so it is not (really)
	 * necessary to have the one-to-one connection between the Facade object (here) and the actual Display client out in the field.
	 * Breaking that might be tricky, and it will definitely make the implementation more complicated. but in the era when we have
	 * (ahem!) hundreds of GUIs out there trying to connect to (ahem!) thousands of Displays, it would save a lot of sockets on the
	 * server.
	 * 
	 * This is a real concern if we get thousands of clients. See
	 * http://stackoverflow.com/questions/1575453/how-many-socket-connections-can-a-typical-server-handle --
	 * 
	 * "The port number is a 16-bit unsigned integer, so 65536 is a hard limit on the maximum number of TCP/IP connections. This
	 * varies from system to system, but a more realistic number is 'several thousand'. For web sites, the actual number of sessions
	 * can be much greater than the maximum number of simultaneous sockets because a web browser disconnects after transferring the
	 * data and reconnects later as necessary."
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * @copyright 2014
	 * 
	 */
	private static class FacadeMessagingClient extends MessagingClient {

		private Map<String, DisplayFacade>		clients		= new HashMap<String, DisplayFacade>();
		// private String lookingFor;
		private DCProtocol						dcp			= new DCProtocol();
		private static FacadeMessagingClient	me			= null;
		private static boolean					notStarted	= true;

		private FacadeMessagingClient(String server, int port) {
			super(server, port, THIS_IP_NAME + " selector " + THIS_IP_NAME_INSTANCE);
			setReadOnly(false);
		}

		public static void registerClient(final String server, final int port, final String lookingFor, final DisplayFacade client) {
			if (me == null)
				me = new FacadeMessagingClient(server, port);
			me.clients.put(lookingFor, client);
		}

		@Override
		public void displayIncomingMessage(final MessageCarrier message) {
			dcp.processInput(message, clients.get(message.getFrom()).getNumber());
		}

		/**
		 * What is the target Display for this client?
		 * 
		 * @param client
		 *            This client
		 * @return The target Display name
		 */
		public static String getTargetName(DisplayFacade client) {
			for (String entry : me.clients.keySet()) {
				if (client.equals(me.clients.get(entry))) {
					return entry;
				}
			}
			return null;
		}

		public static String getMyName() {
			return me.getName();
		}

		/**
		 * Start the messaging client
		 */
		public static void doStart() {
			if (notStarted)
				me.start();
			notStarted = false;
		}

		/**
		 * Direct the messaging client to send the message
		 * 
		 * @param msg
		 *            The message to send
		 */
		public static void sendAMessage(MessageCarrier msg) {
			me.sendMessage(msg);
		}
	}

	/**
	 * @param portNumber
	 *            -- Ignored
	 * @param ipName
	 * @param screenNumber
	 * @param number
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayFacade(final int portNumber, final String ipName, final int number, final int screenNumber,
			final String location, final Color color, final SignageType type) {
		super(ipName, number, screenNumber, location, color, type);

		// try {
		// In UNICODE, this is spelled "FA\u00c7ADE"
		// myExpectedName += " -- " + InetAddress.getLocalHost().getCanonicalHostName() + " Façade".toUpperCase();
		myExpectedName = ipName + ":" + screenNumber + " (" + number + ")";
		// if (!"ChannelSelector".equals(PROGRAM_NAME))
		// myExpectedName += " " + PROGRAM_NAME; // Used by "IdentifyAll" to connect to the Displays in parallel

		// myExpectedName = InetAddress.getLocalHost().getCanonicalHostName() + " FA\u00c7ADE";
		// } catch (UnknownHostException e1) {
		// e1.printStackTrace(); // Really? This should never happen.
		// }
		System.out.println(DisplayFacade.class.getSimpleName() + ": The messaging name of the Display is expected to be ["
				+ myExpectedName + "]");
		FacadeMessagingClient.registerClient(MESSAGING_SERVER_NAME, MESSAGING_SERVER_PORT, ipName + ":" + screenNumber + " ("
				+ number + ")", this);
		FacadeMessagingClient.doStart();
	}

	public void localSetContent() {
		try {
			SignageContent content = getContent();
			EncodedCarrier cc = null;
			if (content instanceof ChannelPlayList) {
				System.out.println(getClass().getSimpleName() + ": Have a ChannelPlayList to deal with!");
				cc = new ChangeChannelList();
				((ChangeChannelList) cc).setDisplayNumber(getNumber());
				((ChangeChannelList) cc).setContent(getContent());
			} else {
				System.out.println(getClass().getSimpleName() + ": Have a simple channel");
				cc = new ChangeChannel();
				((ChangeChannel) cc).setDisplayNumber(getNumber());
				((ChangeChannel) cc).setContent(getContent());
			}

			String xmlMessage = MyXMLMarshaller.getXML(cc);
			FacadeMessagingClient.sendAMessage(MessageCarrier.getMessage(FacadeMessagingClient.getMyName(),
					FacadeMessagingClient.getTargetName(this), xmlMessage));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return Is there really a display connected to us, here?
	 */
	public boolean isConnected() {
		// TODO This return value should be true only if we have seen a message from our connected Display. Punt for now.
		return true;
		// return false;
	}

	@Override
	public String getMessagingName() {
		// return FacadeMessagingClient.getMyName();
		return myExpectedName;
	}

	@Override
	public String getStatus() {
		return super.getStatus() + ", " + (ready.get() ? "Is ready" : "Is not ready") + " and "
				+ (waiting.get() ? "waiting." : "not waiting.");
	}

	@Override
	public void disconnect() {
		alreadyWaiting.set(false);
		FacadeMessagingClient.me.disconnect();
	}
}
