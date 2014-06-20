package gov.fnal.ppd.signage.display;

import gov.fnal.ppd.chat.MessageCarrier;
import gov.fnal.ppd.chat.MessagingClient;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.channel.ChannelPlayList;
import gov.fnal.ppd.signage.comm.DDMessage;
import gov.fnal.ppd.signage.util.Util;
import gov.fnal.ppd.signage.xml.ChangeChannel;
import gov.fnal.ppd.signage.xml.ChangeChannelList;
import gov.fnal.ppd.signage.xml.EncodedCarrier;
import gov.fnal.ppd.signage.xml.MyXMLMarshaller;
import gov.fnal.ppd.signage.xml.Ping;

import java.awt.Color;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
	// private DCClient dcc;
	private AtomicBoolean		ready						= new AtomicBoolean(false);
	private AtomicBoolean		waiting						= new AtomicBoolean(false);
	// private Thread initThread;

	// We should be able to make this static, but the ChannelSelector does not see all the "alive" messages from the far-away
	// Displays (it has to do with the name that this client has.)
	private MessagingClient		messagingClient				= null;

	/**
	 * @param portNumber
	 * @param ipName
	 * @param screenNumber
	 * @param number
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayFacade(final int portNumber, final String ipName, final int screenNumber, final int number,
			final String location, final Color color, final SignageType type) {
		super(ipName, screenNumber, number, location, color, type);

		try {
			// That last bit is FACADE, with the curly thing under the C
			myName += " -- " + InetAddress.getLocalHost().getCanonicalHostName() + " FA\u00c7ADE";
			// myName = InetAddress.getLocalHost().getCanonicalHostName() + " FA\u00c7ADE";
		} catch (UnknownHostException e1) {
			e1.printStackTrace(); // Really? This should never happen.
		}
		System.out.println(" My messaging name is [" + myName + "]");
		messagingClient = new MessagingClient(Util.MESSAGING_SERVER_NAME, Util.MESSAGING_SERVER_PORT, myName);
		messagingClient.start();
	}

	public void localSetContent() {
		try {
			// if (dcc != null) { // && (ready.get() || sync())) {
			SignageContent content = getContent();
			EncodedCarrier cc = null;
			if (content instanceof ChannelPlayList) {
				System.out.println(getClass().getSimpleName() + ": Have a ChannelPlayList to deal with!");
				cc = new ChangeChannelList();
				((ChangeChannelList) cc).setContent(getContent());
			} else {
				System.out.println(getClass().getSimpleName() + ": Have a simple channel");
				cc = new ChangeChannel();
				((ChangeChannel) cc).setContent(getContent());
			}

			String xmlMessage = MyXMLMarshaller.getXML(cc);
			messagingClient.sendMessage(MessageCarrier.getMessage(xmlMessage));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send the Display a Ping message
	 */
	public void ping() {
		try {
			// if (dcc != null) { // && (ready.get() || sync())) {
			DDMessage message = new DDMessage(MyXMLMarshaller.getXML(new Ping()));
			// if (USE_CLIENT_SERVER) {
			// dcc.send(message.getOutputMessage());
			// System.out.println(getClass().getSimpleName() + " -- Ping -- Sent '" + message.getOutputMessage() + "'");
			// sync(DisplayChangeEvent.Type.ALIVE);
			// }
			// if (USE_MESSAGING) {
			messagingClient.sendMessage(MessageCarrier.getMessage(message.getOutputMessage()));
			// }

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return Is there really a display connected to us, here?
	 */
	public boolean isConnected() {
		// if (USE_CLIENT_SERVER)
		// return dcc != null && dcc.isConnected();
		// if (USE_MESSAGING)
		return true; // TODO This return value should be true only if we have seen a message from out connected Display. Punt
						// for now.
		// return false;
	}

	@Override
	public String getStatus() {
		return super.getStatus() + ", " + (ready.get() ? "Is ready" : "Is not ready") + " and "
				+ (waiting.get() ? "waiting." : "not waiting.");
	}
}
