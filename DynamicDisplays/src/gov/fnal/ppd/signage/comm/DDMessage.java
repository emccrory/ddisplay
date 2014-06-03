package gov.fnal.ppd.signage.comm;

import gov.fnal.ppd.signage.xml.ChangeChannel;
import gov.fnal.ppd.signage.xml.ChangeChannelList;
import gov.fnal.ppd.signage.xml.ChannelSpec;
import gov.fnal.ppd.signage.xml.MyXMLMarshaller;
import gov.fnal.ppd.signage.xml.Ping;
import gov.fnal.ppd.signage.xml.Pong;

import javax.management.RuntimeErrorException;
import javax.xml.bind.JAXBException;

/**
 * Interprets the messages within the protocol for the client and the server
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2014
 * 
 */
public class DDMessage {
	private String				rawMessage;

	private int					screenNumber;
	private Object				receivedMessage;
	// private final static String SCREEN = "Screen:";
	private static final String	XMLPRE				= "<?xml";
	private static final String	PING				= "<ping ";
	private static final String	PONG				= "<pong ";
	private static final String	CHANGE_CHANNEL		= "<changeChannel ";
	private static final String	CHANGE_CHANNEL_LIST	= "<changeChannelList ";
	private static final String	CHANNEL_SPEC		= "<channelSpec ";

	public DDMessage(String raw) {
		this.screenNumber = 0;
		if (raw != null && raw.length() > 0) {
			// remove all the non-printable characters, but put back in the newlines
			String resultString = raw.replaceAll("[^\\x20-\\x7F]", "").replaceAll(" +", " ").replace("><", ">\n<")
					.replace("> <", ">\n<");

			// System.out.println(DDMessage.class.getSimpleName() + ".ctor(): Message received [" + resultString + "]");

			this.rawMessage = resultString;
			if (rawMessage == null || rawMessage.length() == 0) {
				// Nothing there!
				System.err.println(DDMessage.class + ": No characters received!");
			} else
				decode();
		} else {
			rawMessage = "nothing";
			receivedMessage = null;
		}
	}

	private void decode() {
		if (rawMessage.startsWith(XMLPRE)) {
			// We have to have one of the XML classes defined here: Ping, Pong, ChangeChannel, ChangeChannelList, or ChannelSpec
			try {
				if (rawMessage.contains(PING)) {
					receivedMessage = MyXMLMarshaller.unmarshall(Ping.class, rawMessage);
				} else if (rawMessage.contains(PONG)) {
					receivedMessage = MyXMLMarshaller.unmarshall(Pong.class, rawMessage);
				} else if (rawMessage.contains(CHANGE_CHANNEL)) {
					receivedMessage = MyXMLMarshaller.unmarshall(ChangeChannel.class, rawMessage);
				} else if (rawMessage.contains(CHANGE_CHANNEL_LIST)) {
					receivedMessage = MyXMLMarshaller.unmarshall(ChangeChannelList.class, rawMessage);
				} else if (rawMessage.contains(CHANNEL_SPEC)) {
					receivedMessage = MyXMLMarshaller.unmarshall(ChannelSpec.class, rawMessage);
				} else {
					System.err.println(getClass().getSimpleName() + ".decode(): Unrecognized XML message received.");
				}
			} catch (JAXBException e) {
				e.printStackTrace();
			}
			if (receivedMessage != null)
				System.out.println(getClass().getSimpleName() + ".decode(): Incoming message interpreted as an object of type "
						+ receivedMessage.getClass().getCanonicalName());
			else if (rawMessage != null)
				throw new RuntimeErrorException(new Error("Unknown XML data type within this XML document: [" + rawMessage + "]"));
		}
	}

	public String getURL() {
		if (receivedMessage instanceof ChangeChannelList)
			return ((ChangeChannelList) receivedMessage).getChannelSpec()[0].getUri().toASCIIString(); // FIXME -- Have to
																										// increment this index
																										// sometime
		if (receivedMessage instanceof ChangeChannel)
			return ((ChangeChannel) receivedMessage).getChannelSpec().getUri().toASCIIString();
		return null;
	}

	public int getDisplay() {
		return screenNumber;
	}

	public String getOutputMessage() {
		// if (command != null)
		// return "Command=" + command;
		// return URL + url + "\t" + SCREEN + display;
		return rawMessage;
	}

	public Object getMessage() {
		return receivedMessage;
	}

	@Override
	public String toString() {
		return "display=[" + screenNumber + "] XML=[\n\t" + rawMessage.replace("\n", "\n\t") + "\n] received type="
				+ receivedMessage.getClass().getCanonicalName();
	}

	// @Override
	// public boolean equals(Object s) {
	// if (s == null)
	// return false;
	// if (s instanceof DDMessage)
	// return ((DDMessage) s).url.equals(url) && ((DDMessage) s).display == display;
	// if (s instanceof String)
	// return ((String) s).equalsIgnoreCase(command);
	// return false;
	// }
}
