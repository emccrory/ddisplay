package gov.fnal.ppd.dd.display;

import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.GlobalVariables.getFullSelectorName;
import static gov.fnal.ppd.dd.GlobalVariables.getMessagingServerName;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;

import gov.fnal.ppd.dd.changer.DisplayChangeEvent;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.chat.DCProtocol;
import gov.fnal.ppd.dd.chat.ErrorProcessingMessage;
import gov.fnal.ppd.dd.chat.MessagingClient;
import gov.fnal.ppd.dd.emergency.EmergencyMessage;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.EmergencyCommunication;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.PropertiesFile;
import gov.fnal.ppd.dd.util.TemporaryDialogBox;
import gov.fnal.ppd.dd.xml.AreYouAliveMessage;
import gov.fnal.ppd.dd.xml.ChangeChannel;
import gov.fnal.ppd.dd.xml.ChangeChannelByNumber;
import gov.fnal.ppd.dd.xml.ChangeChannelList;
import gov.fnal.ppd.dd.xml.ChangeChannelReply;
import gov.fnal.ppd.dd.xml.EmergencyMessXML;
import gov.fnal.ppd.dd.xml.ErrorMessage;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MessagingDataXML;
import gov.fnal.ppd.dd.xml.WhoIsInReply;

/**
 * On the computer with the channel selector, this is the way you talk to a remote Dynamic Display. This classed is used in the GUI
 * as the Facade between the GUI and the actual display. That actual display sits on the other side of the network connection,
 * through the messaging server.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, First version: 2013
 * 
 */
public class DisplayFacade extends DisplayImpl {

	private static final boolean	CHANNEL_NUMBER_ONLY			= PropertiesFile.getBooleanProperty("UseChanNumsOnly", false);
	/**
	 * 
	 */
	public static boolean			tryToConnectToDisplaysNow	= false;
	/**
	 * 
	 */
	public static AtomicBoolean		alreadyWaiting				= new AtomicBoolean(false);
	private AtomicBoolean			ready						= new AtomicBoolean(false);
	private AtomicBoolean			waiting						= new AtomicBoolean(false);
	private String					myExpectedName;
	private int						locCode;
	private boolean					receivedAReply				= false;

	static private int				lastLocCode					= Integer.MAX_VALUE;
	static private boolean			multipleLocations			= false;

	/**
	 * Internal client for handling the direct messaging connection to our Display. There is one connection (through the Messaging
	 * Server) and one instance of this private, inner class to each Display. That is, if there are ten Displays handled by the
	 * ChannelSelector GUI, there will be ten sockets (connected through the Messaging Server) within the GUI to the ten individual
	 * Displays.
	 * 
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
			super(server, port, getFullSelectorName());
			setReadOnly(false);
		}

		public static void registerClient(final String server, final int port, final String lookingFor,
				final DisplayFacade client) {
			if (me == null)
				me = new FacadeMessagingClient(server, port);
			me.clients.put(lookingFor, client);
		}

		@Override
		public void receiveIncomingMessage(final MessageCarrierXML message) {
			// if (message.getFrom().equals(SPECIAL_SERVER_MESSAGE_USERNAME) && message.getType() == MessageType.ERROR) {
			Class<? extends MessagingDataXML> clazz = message.getMessageValue().getClass();
			if (clazz.equals(ErrorMessage.class)) {
				println(getClass(), ".receiveIncomingMessage(): Got an error message -- " + message);
				dcp.errorHandler(message);
			} else if (clazz.equals(WhoIsInReply.class)) {
				DisplayFacade DF = clients.get(message.getMessageOriginator());
				if (DF != null) {
					DF.informListeners(DisplayChangeEvent.DisplayChangeType.CHANGE_COMPLETED, "Channel change succeeded");
					// println(getClass(), ".receiveIncomingMessage(): Yay! Got a real confirmation that the channel was changed.");
				} else
					println(getClass(),
							".receiveIncomingMessage(): No client named " + message.getMessageOriginator()
									+ " to which to send the REPLY.\n" + "t\tThe clients are called: "
									+ Arrays.toString(clients.keySet().toArray()));
			} else if (clazz.equals(AreYouAliveMessage.class)) {
				sendMessage(MessageCarrierXML.getIAmAlive(getMyName(), message.getMessageOriginator()));
			} else {
				// DisplayFacade d = clients.get(message.getFrom());
				try {
					if (!dcp.processInput(message)) {
						sendMessage(MessageCarrierXML.getErrorMessage(message.getMessageRecipient(), message.getMessageOriginator(),
								dcp.getErrorMessageText()));
					}
				} catch (ErrorProcessingMessage e) {
					// Ignore this error on the GUI side
					e.printStackTrace();
				}
			}
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
		public static void sendAMessage(MessageCarrierXML msg) {
			me.sendMessage(msg);
		}

		public static void addListener(DisplayFacade displayFacade) {
			me.dcp.addListener(displayFacade);
		}
	}

	/**
	 * @param locCode
	 *            -- The location code of this display
	 * @param ipName
	 *            The name of the display
	 * @param vNumber
	 *            The virtual display number (for local consumption)
	 * @param dbNumber
	 *            The database index for this display
	 * @param screenNumber
	 *            The screen number (like 0 or 1)
	 * @param location
	 *            A description of the location of this display
	 * @param color
	 *            the highlight color for this display
	 */
	public DisplayFacade(final int locCode, final String ipName, final int vNumber, final int dbNumber, final int screenNumber,
			final String location, final Color color) {
		super(ipName, vNumber, dbNumber, screenNumber, location, color);

		this.locCode = locCode;
		if (lastLocCode == Integer.MAX_VALUE)
			lastLocCode = locCode;
		if (lastLocCode != locCode)
			multipleLocations = true;
		// try {
		// In UNICODE, this is spelled "FA\u00c7ADE"
		// myExpectedName += " -- " + InetAddress.getLocalHost().getCanonicalHostName() + " Façade".toUpperCase();
		myExpectedName = ipName + ":" + screenNumber + " (" + vNumber + ")";
		// if (!"ChannelSelector".equals(PROGRAM_NAME))
		// myExpectedName += " " + PROGRAM_NAME; // Used by "IdentifyAll" to connect to the Displays in parallel

		// myExpectedName = InetAddress.getLocalHost().getCanonicalHostName() + " FA\u00c7ADE";
		// } catch (UnknownHostException e1) {
		// e1.printStackTrace(); // Really? This should never happen.
		// }
		println(DisplayFacade.class,
				": The messaging name of Display " + vNumber + "/" + dbNumber + " is expected to be '" + myExpectedName + "'");
		FacadeMessagingClient.registerClient(getMessagingServerName(), MESSAGING_SERVER_PORT, myExpectedName, this);
		FacadeMessagingClient.addListener(this);
		// TODO -- It would be better to defer the start of these messaging clients until the whole GUI creation is finished. Not
		// sure how to make THAT happen
		FacadeMessagingClient.doStart();
	}

	public boolean localSetContent() {
		receivedAReply = false;
		informListeners(DisplayChangeEvent.DisplayChangeType.CHANGE_RECEIVED, null);
		try {
			SignageContent content = getContent();
			MessagingDataXML theData = null;
			if (content instanceof ChannelPlayList) {
				println(getClass(), ": Have a ChannelPlayList to deal with!");
				theData = new ChangeChannelList();
				((ChangeChannelList) theData).setDisplayNumber(getVirtualDisplayNumber());
				((ChangeChannelList) theData).setScreenNumber(getScreenNumber());
				((ChangeChannelList) theData).setChannelNumber(((ChannelPlayList) content).getNumber());
				((ChangeChannelList) theData).setContent(getContent());
			} else if (content instanceof EmergencyCommunication) {
				EmergencyMessage em = ((EmergencyCommunication) content).getMessage();
				println(getClass(), ": Request to send an EMERGENCY MESSAGE");
				theData = new EmergencyMessXML(em);
			} else {
				if (CHANNEL_NUMBER_ONLY) {
					println(getClass(), ": Have a simple channel -- sending the channel number only");
					theData = new ChangeChannelByNumber();
					((ChangeChannelByNumber) theData).setDisplayNumber(getVirtualDisplayNumber());
					((ChangeChannelByNumber) theData).setScreenNumber(getScreenNumber());
					((ChangeChannelByNumber) theData).setChannelNumber(((Channel) content).getNumber());
					((ChangeChannelByNumber) theData).setChecksum(content.getChecksum());
				} else {
					println(getClass(), ": Have a simple channel");
					theData = new ChangeChannel();
					((ChangeChannel) theData).setDisplayNumber(getVirtualDisplayNumber());
					((ChangeChannel) theData).setScreenNumber(getScreenNumber());
					if (content instanceof Channel)
						((ChangeChannel) theData).setChannelNumber(((Channel) content).getNumber());
					((ChangeChannel) theData).setContent(content);
				}
			}

			if (theData != null)
				FacadeMessagingClient.sendAMessage(new MessageCarrierXML(FacadeMessagingClient.getMyName(),
						FacadeMessagingClient.getTargetName(this), theData));

			// A reply is expected. It happens in DisplayControllerMessagingAbstract.respondToContentChange().

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
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

	/**
	 * A kludged-up method OF USE ONLY by the status mechanism to set the apparent content, as obtained from elsewhere (like from a
	 * database read).
	 * 
	 * @param content
	 *            -- The apparent content on this display
	 */
	public void setContentValueBackdoor(final SignageContent content) {
		channel = content;
	}

	public void actionPerformed(ActionEvent e) {
		if (e instanceof DisplayChangeEvent) {
			ChangeChannelReply ccr = (ChangeChannelReply) e.getSource();
			if (ccr.getDisplayNum() == getDBDisplayNumber()) {
				receivedAReply = true;
				new TemporaryDialogBox(null, this);
			} // else it is a reply for some other virtual display here in the channel selector JVM
		} else {
			super.actionPerformed(e);
		}
	}

	public boolean hasReceivedAReply() {
		return receivedAReply;
	}

	public String toString() {
		String retval = super.toString();
		if (multipleLocations && retval.startsWith("Display"))
			retval += " (Loc=" + locCode + ")";
		return retval;
	}

	public static String getPresentStatus() {
		return FacadeMessagingClient.me.getPresentStatus();
	}
}
