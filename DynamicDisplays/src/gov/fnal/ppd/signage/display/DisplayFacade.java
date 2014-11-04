package gov.fnal.ppd.signage.display;

import static gov.fnal.ppd.GlobalVariables.MESSAGING_SERVER_NAME;
import static gov.fnal.ppd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.GlobalVariables.PROGRAM_NAME;
import gov.fnal.ppd.chat.MessageCarrier;
import gov.fnal.ppd.chat.MessagingClient;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.channel.ChannelPlayList;
import gov.fnal.ppd.signage.comm.DCProtocol;
import gov.fnal.ppd.signage.comm.DDMessage;
import gov.fnal.ppd.signage.xml.ChangeChannel;
import gov.fnal.ppd.signage.xml.ChangeChannelList;
import gov.fnal.ppd.signage.xml.EncodedCarrier;
import gov.fnal.ppd.signage.xml.MyXMLMarshaller;
import gov.fnal.ppd.signage.xml.Ping;

import java.awt.Color;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBException;

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
	public static boolean			tryToConnectToDisplaysNow	= false;
	/**
	 * 
	 */
	public static AtomicBoolean		alreadyWaiting				= new AtomicBoolean(false);
	private AtomicBoolean			ready						= new AtomicBoolean(false);
	private AtomicBoolean			waiting						= new AtomicBoolean(false);

	private FacadeMessagingClient	messagingClient				= null;

	private class FacadeMessagingClient extends MessagingClient {

		private String		lookingFor;
		private DCProtocol	dcp	= new DCProtocol();

		public FacadeMessagingClient(String lookingFor, String server, int port, String username) {
			super(server, port, username);
			this.lookingFor = lookingFor;
		}

		@Override
		public void displayIncomingMessage(final MessageCarrier message) {
			dcp.processInput(message, getNumber());
		}

		public String getTargetName() {
			return lookingFor;
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
	public DisplayFacade(final int portNumber, final String ipName, final int number,final int screenNumber, 
			final String location, final Color color, final SignageType type) {
		super(ipName, number, screenNumber, location, color, type);

		try {
			// In UNICODE, this is spelled "FA\u00c7ADE"
			myName += " -- " + InetAddress.getLocalHost().getCanonicalHostName() + " Façade".toUpperCase();
			if (!"ChannelSelector".equals(PROGRAM_NAME))
				myName += " " + PROGRAM_NAME; // Used by "IdentifyAll" to connect to the Displays in parallel

			// myName = InetAddress.getLocalHost().getCanonicalHostName() + " FA\u00c7ADE";
		} catch (UnknownHostException e1) {
			e1.printStackTrace(); // Really? This should never happen.
		}
		System.out.println(DisplayFacade.class.getSimpleName() + ": My messaging name is [" + myName + "]");
		messagingClient = new FacadeMessagingClient(ipName + ":" + screenNumber + " (" + number + ")", MESSAGING_SERVER_NAME,
				MESSAGING_SERVER_PORT, myName);

		messagingClient.start();
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
			messagingClient.sendMessage(MessageCarrier.getMessage(messagingClient.getName(), messagingClient.getTargetName(), xmlMessage));
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
	public String getStatus() {
		return super.getStatus() + ", " + (ready.get() ? "Is ready" : "Is not ready") + " and "
				+ (waiting.get() ? "waiting." : "not waiting.");
	}

	@Override
	public void disconnect() {
		alreadyWaiting.set(false);
		messagingClient.disconnect();
	}
}
