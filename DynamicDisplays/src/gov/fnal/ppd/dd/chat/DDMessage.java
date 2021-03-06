package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import javax.xml.bind.JAXBException;

import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;
import gov.fnal.ppd.dd.xml.messages.ChangeChannel;
import gov.fnal.ppd.dd.xml.messages.ChangeChannelByNumber;
import gov.fnal.ppd.dd.xml.messages.ChangeChannelList;

/**
 * The class for the messages, using the correct protocol, between and among the clients and the server(s). This class has an
 * awareness of the structure of the message. Right now, that means that it peeks into the XML to see what kind of message it is.
 * 
 * It was originally imagined that there would be a variety of messages, including something like a "Ping" and a "Pong" message.
 * However, it turned out that the only sorts of messages this class contains are the ones that instruct a display to change the
 * channel.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2014-21
 * 
 */
public class DDMessage {
	private String				rawMessage;

	private Object				receivedMessage;
	private static final String	XMLPRE				= "<?xml";
	private static final String	CHANGE_CHANNEL		= "<changeChannel ";
	private static final String	CHANGE_CHANNEL_LIST	= "<changeChannelList ";
	private static final String	CHANNEL_SPEC		= "<channelSpec ";
	private static final String	CHANNEL_SPEC_NUMBER	= "<changeChannelByNumber ";

	/**
	 * Take a raw message from the messaging system and interpret into the sort of command it represents.
	 * 
	 * @param raw
	 *            The body of the message to create
	 * @throws ErrorProcessingMessage
	 *             When the message cannot be interpreted properly.
	 */
	public DDMessage(final String raw) throws ErrorProcessingMessage {
		if (raw != null && raw.length() > 0) {
			// remove all the non-printable characters, but put back in the newlines
			String resultString = raw.replaceAll("[^\\x20-\\x7F]", "").replaceAll(" +", " ").replace("><", ">\n<").replace("> <",
					">\n<");

			// System.out.println(DDMessage.class.getSimpleName() + ".ctor(): Message received [" + resultString + "]");

			this.rawMessage = resultString;
			if (rawMessage == null || rawMessage.length() == 0) {
				// Nothing there!
				System.err.println(DDMessage.class + ": No characters received!");
			} else {
				if (rawMessage.startsWith(XMLPRE)) {
					// We have to have one of the XML classes defined here: Ping, Pong, ChangeChannel, ChangeChannelList, or
					// ChannelSpec
					try {
						// These sorts of XML messages are not used
						// if (rawMessage.contains(PING)) {
						// receivedMessage = MyXMLMarshaller.unmarshall(Ping.class, rawMessage);
						// } else if (rawMessage.contains(PONG)) {
						// receivedMessage = MyXMLMarshaller.unmarshall(Pong.class, rawMessage);
						// } else if (rawMessage.contains(HEARTBEAT)) {
						// receivedMessage = MyXMLMarshaller.unmarshall(HeartBeat.class, rawMessage);
						// } else

						if (rawMessage.contains(CHANGE_CHANNEL)) {
							receivedMessage = MyXMLMarshaller.unmarshall(ChangeChannel.class, rawMessage);
						} else if (rawMessage.contains(CHANGE_CHANNEL_LIST)) {
							receivedMessage = MyXMLMarshaller.unmarshall(ChangeChannelList.class, rawMessage);
						} else if (rawMessage.contains(CHANNEL_SPEC)) {
							receivedMessage = MyXMLMarshaller.unmarshall(ChannelSpec.class, rawMessage);
						} else if (rawMessage.contains(CHANNEL_SPEC_NUMBER)) {
							receivedMessage = MyXMLMarshaller.unmarshall(ChangeChannelByNumber.class, rawMessage);
						} else {
							throw new ErrorProcessingMessage(
									"Unknown XML data type within this XML document: [Unrecognized XML message received.");
						}
					} catch (JAXBException e) {
						e.printStackTrace();
					}
					if (receivedMessage != null) {
						println(getClass(), ".decode(): Incoming message interpreted as an object of type "
								+ receivedMessage.getClass().getCanonicalName());
					} else if (rawMessage != null)
						throw new ErrorProcessingMessage("Unknown XML data type within this XML document: [" + rawMessage + "]");
				}
			}
		} else {
			rawMessage = "nothing";
			receivedMessage = null;
		}
	}

	// /**
	// * @return The URL that this message specifies
	// */
	// public String getURL() {
	// if (receivedMessage instanceof ChangeChannelList)
	// // FIXME -- Have to increment this index sometime
	// return ((ChangeChannelList) receivedMessage).getChannelSpec()[0].getContent().getURI().toASCIIString();
	// if (receivedMessage instanceof ChangeChannel)
	// return ((ChangeChannel) receivedMessage).getChannelSpec().getContent().getURI().toASCIIString();
	// return null;
	// }

	/**
	 * @return The message that should be sent on the wire
	 */
	public String getOutputMessage() {
		// if (command != null)
		// return "Command=" + command;
		// return URL + url + "\t" + SCREEN + display;
		return rawMessage;
	}

	/**
	 * @return The received message from the wire
	 */
	public Object getMessage() {
		return receivedMessage;
	}

	@Override
	public String toString() {
		return "XML=[\n\t" + rawMessage.replace("\n", "\n\t") + "\n] received type="
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
