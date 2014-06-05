package gov.fnal.ppd.signage.display;

import gov.fnal.ppd.chat.MessageCarrier;
import gov.fnal.ppd.chat.MessagingClient;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.channel.ChannelPlayList;
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

/**
 * On the computer with the channel selector, this is the way you talk to a remote Dynamic Display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2013
 * 
 */
public class DisplayFacade extends DisplayImpl {
	private static final int	MESSAGING_SERVER_PORT		= 1500;
	private static final String	MESSAGING_SERVER_NAME		= System.getProperty("signage.dbserver", "mccrory.fnal.gov");

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

	private MessagingClient		messagingClient;

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

		// if (USE_CLIENT_SERVER) {
		// connect(ipName, portNumber);
		//
		// new Thread("DisplayFacadeInit." + ipName) { // make sure the connection continues
		// public void run() {
		// while (true) {
		// try {
		// sleep(2000);
		// } catch (InterruptedException e) {
		// }
		// if (dcc == null || !dcc.isConnected()) {
		// dcc = null;
		// connect(ipName, portNumber);
		// }
		// }
		// }
		// }.start();
		// }

		try {
			// That last bit is FACADE, with the curly thing under the C
			myName += " -- " + InetAddress.getLocalHost().getCanonicalHostName() + " FA\u00c7ADE";
		} catch (UnknownHostException e1) {
			e1.printStackTrace(); // Really? This should never happen.
		}
		System.out.println(" My messaging name is [" + myName + "]");
		messagingClient = new MessagingClient(MESSAGING_SERVER_NAME, MESSAGING_SERVER_PORT, myName);
		messagingClient.start();
	}

	// protected void connect(final String ipName, final int portNumber) {
	// if (initThread != null && initThread.isAlive()) {
	// // Already trying to connect this object to a real Display out there
	// return;
	// }
	//
	// initThread = new Thread("DisplayFacadeInit" + ipName) {
	// public void run() {
	// try {
	// dcc = new DCClient(ipName, portNumber);
	// dcc.addListener(DisplayFacade.this);
	// if (dcc.isConnected())
	// sync(DisplayChangeEvent.Type.ALIVE);
	// else {
	// // No connection at this time. Try again in a moment (and keep trying!)
	// dcc = null;
	// ready.set(false);
	// for (int c = 0; c < 10; c++) { // This look controls how long to wait between attempts to reconnect
	// sleep(500);
	// if (tryToConnectToDisplaysNow) {
	// // Another agent could ask us, asynchronously, to try to reconnect
	// if (!alreadyWaiting.get()) {
	// alreadyWaiting.set(true);
	// new Thread() {
	// public void run() {
	// try {
	// sleep(1000); // Wait for all of the other instances of me to get here
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// tryToConnectToDisplaysNow = false;
	// alreadyWaiting.set(false);
	// }
	// }.start();
	// }
	// connect(ipName, portNumber); // recurse
	// break;
	// }
	// }
	// connect(ipName, portNumber); // recurse
	//
	// // FIXME -- How do we tell the button attached to this Display that we are alive now?
	//
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	// };
	// initThread.start();
	// }

	// private boolean sync(DisplayChangeEvent.Type dce) throws IOException {
	// if (!waiting.get()) {
	// if (dcc != null) {
	// waiting.set(true);
	// ready.set(dcc.waitForServer());
	// if (dcc != null && ready.get() && dcc.getTheReply() instanceof Pong) {
	// ChannelSpec spec = ((Pong) dcc.getTheReply()).getChannelSpec();
	// Channel content = (Channel) getContent();
	// content.setCategory(spec.getCategory());
	// content.setName(spec.getName());
	// content.setURI(spec.getUri());
	// }
	// informListeners(dce);
	// waiting.set(false);
	// } else {
	// // FIXME There is no dcc object! This happens when we have not managed to connect to the actual Display yet.
	// System.err.println(getClass().getSimpleName() + ": Internal problem, baby! dcc is null");
	// informListeners(dce);
	// }
	// }
	// waiting.set(false);
	// return ready.get();
	// }

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

			// DDMessage message = new DDMessage(getContent().getURI().toASCIIString(), getScreenNumber());
			String xmlMessage = MyXMLMarshaller.getXML(cc);
			// DDMessage message = new DDMessage(xmlMessage);
			// if (USE_CLIENT_SERVER & dcc != null) {
			// dcc.send(message.getOutputMessage());
			// System.out.println(getClass().getSimpleName() + " --DEBUG-- Sent '" + message.getOutputMessage() + "'");
			//
			// sync(DisplayChangeEvent.Type.CHANGE_COMPLETED);
			// }
			// if (USE_MESSAGING) {
			// Now send the same thing to the Messaging Server (Test, 5/29/2014)
			messagingClient.sendMessage(new MessageCarrier(1, xmlMessage));
			// }

			// } else {
			// System.out.println(getClass().getSimpleName() + " (screen " + getScreenNumber() + "): Change to '"
			// + getContent().getURI().toASCIIString() + "' not completed.");
			// informListeners(DisplayChangeEvent.Type.CHANGE_COMPLETED);
			// }
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
			messagingClient.sendMessage(new MessageCarrier(1, message.getOutputMessage()));
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
		return true; // TODO This return value should be true only if we have seen a messsage from out connected Display. Punt
						// for now.
		// return false;
	}

	@Override
	public String getStatus() {
		return super.getStatus() + ", " + (ready.get() ? "Is ready" : "Is not ready") + " and "
				+ (waiting.get() ? "waiting." : "not waiting.");
	}
}
