/*
 * MessageCarrier
 *
 * Taken from the Internet on 5/12/2014. @see <a
 * href="http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/" dreamincode.net</a>
 * 
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.xml;

import static gov.fnal.ppd.dd.GlobalVariables.PRIVATE_KEY_LOCATION;
import static gov.fnal.ppd.dd.GlobalVariables.getFullSelectorName;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.io.Serializable;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import gov.fnal.ppd.dd.chat.MessageType;
import gov.fnal.ppd.dd.xml.signature.SignXMLUsingDSAKeys;
import gov.fnal.ppd.dd.xml.signature.SignedXMLDocument;

/**
 * This class holds the messages that will be exchanged between the Clients and the Server. 
 * 
 * This is a replacement that is in full XML. Streamed objects in Java are about to be deprecated, so we are streaming pure-ascii XML documents.
 * 
 * Here is what an XML message that carries all the information looks like.
 * 
 * <pre>
 
  <messageCarrier type="TYPE"> # The value of the type is the sort of object that is being transmitted in the tag, messageValue
    <messageRecipient> -- name of the recipient of this message -- </messageRecipient> 
    <messageOriginator> -- name of the sender of this message -- </messageOriginator> 
   	<messageTimeStamp>Jan  1, 2019 01:23:45 CST</messageTimeStamp>
    <messageValue>  # This is an XML representation of an EncodedCarrierXML object
        

    </messageValue> 
    <signature> 
       Gobbledygook  
    </signature>
  </messageCarrier> 

 * </pre>
 *  
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@XmlRootElement
public class MessageCarrierXML implements Serializable {

	protected static final long	serialVersionUID	= 1112122200L;

	private static boolean		signatureInitialized;

	private MessageType			messType;
	// private String message;
	private EncodedCarrier		carrier;
	private String				to;
	private String				from;

	private SignedXMLDocument	theMessage;
	private String				theNonXMLMessage;

	/**
	 * @param from
	 *            The originator of the message
	 * @param to
	 *            The intended recipient of the message
	 * @param message
	 *            The ASCII message to send. Generally, this is an XML document.
	 * @return the object to send over the wire
	 */
	public static MessageCarrierXML getMessage(final String from, final String to, final String message) {
		return new MessageCarrierXML(MessageType.MESSAGE, from, to, message);
	}

	/**
	 * @param from
	 *            The originator of the message
	 * @param to
	 *            The recipient of the message
	 * @param cc
	 * @return the appropriate object
	 */
	public static MessageCarrierXML getMessage(String from, String to, EncodedCarrier cc) {
		MessageCarrierXML r = new MessageCarrierXML(MessageType.MESSAGE, from, to, (String) null);
		r.carrier = cc;
		return r;
	}

	/**
	 * @param from
	 * @param to
	 * @param message
	 * @return A reply message from a client back to the requester
	 */
	public static MessageCarrierXML getReplyMessage(final String from, final String to, final String message) {
		return new MessageCarrierXML(MessageType.REPLY, from, to, message);
	}

	/**
	 * @return A logout message for the messaging server
	 */
	public static MessageCarrierXML getLogout() {
		return new MessageCarrierXML(MessageType.LOGOUT, "");
	}

	/**
	 * @param username
	 * @return A login message for the messaging server
	 */
	public static MessageCarrierXML getLogin(final String username) {
		return new MessageCarrierXML(MessageType.LOGIN, username);
	}

	/**
	 * @param from
	 * @return A message for the messaging server asking for all the clients that are connected
	 */
	public static MessageCarrierXML getWhoIsIn(final String from) {
		return new MessageCarrierXML(MessageType.WHOISIN, from, "NULL", "");
	}

	/**
	 * @param from
	 * @param to
	 *            The name of the client for whom this message is intended
	 * @return A message for the messaging server asking if a client is alive
	 */
	public static MessageCarrierXML getIsAlive(final String from, final String to) {
		return new MessageCarrierXML(MessageType.ISALIVE, from, to, "");
	}

	/**
	 * @param from
	 * @param to
	 * @param message
	 * @return A message to the messaging server that says, "Yes, I am alive"
	 */
	public static MessageCarrierXML getIAmAlive(final String from, final String to, String message) {
		return new MessageCarrierXML(MessageType.AMALIVE, from, to, message);
	}

	/**
	 * There has been an error with message communications.
	 * 
	 * @param from
	 *            The originator of the message
	 * @param to
	 *            The recipient of the message
	 * @param message
	 *            The message itself. It is anticipated that this will be just a statement of the error (i.e., not XML)
	 * @return A message to the messaging server that says, "There has been an error"
	 */
	public static MessageCarrierXML getErrorMessage(final String from, final String to, final String message) {
		return new MessageCarrierXML(MessageType.ERROR, from, to, message);
	}

	/**
	 * This is an emergency message for all clients in the system.
	 * 
	 * @param from
	 *            The originator of the message
	 * @param to
	 *            The recipient of the message
	 * @param message
	 *            The message itself. It is anticipated that this will be an HTML-encoded message that can be shown by the client.
	 * @return A message to the messaging server that conveys the emergency
	 */
	public static MessageCarrierXML getEmergencyMessage(final String from, final String to, final String message) {
		// Stream the Emergency Message
		return new MessageCarrierXML(MessageType.EMERGENCY, from, to, message);
	}

	private MessageCarrierXML(final MessageType messType, final String message) {
		this(messType, "NULL", "NULL", message);
	}

	/**
	 * @param messType
	 *            -- Which type of message is this?
	 * @param message
	 *            -- The body of the message (a simple String)
	 */
	private MessageCarrierXML(final MessageType messType, final String from, final String to, final String message) {
		this.messType = messType;
		this.theNonXMLMessage = message;
		this.from = from;
		this.to = to;
		if (messType.isReadOnly())
			return;

		initializeSignature();
		try {
			this.theNonXMLMessage = null;
			theMessage = new SignedXMLDocument(message);
		} catch (Exception e) {
			printlnErr(getClass(), "Problem signing this message: [" + message + "]. This has to be diagnosed and fixed!");
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public MessageCarrierXML() {
	}

	/**
	 * @return The type of the message
	 */
	@XmlElement
	public MessageType getType() {
		return messType;
	}

	/**
	 * @return The body of the message
	 */
	@XmlElement
	public String getMessage() {
		if (theNonXMLMessage != null)
			return theNonXMLMessage;

		String theString = "Invalid - this is a problem!";
		try {
			theString = MyXMLMarshaller.getXML(theMessage.getSignedDocument());
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return theString;
	}

	/**
	 * @return Who is this message intended to be received by?
	 */
	@XmlElement
	public String getMessageRecipient() {
		return to;
	}

	/**
	 * @param to
	 *            Set to whom this message is aimed
	 */
	public void setMessageRecipient(final String to) {
		this.to = to;
	}

	/**
	 * @return who is this message from?
	 */
	@XmlElement
	public String getOriginator() {
		return from;
	}

	/**
	 * @param from
	 *            Who is this message from
	 */
	public void setOriginator(final String from) {
		this.from = from;
	}

	/**
	 * @return the carrier
	 */
	@XmlElement
	public EncodedCarrier getMessageValue() {
		return carrier;
	}

	/**
	 * @param carrier
	 *            the carrier to set
	 */
	public void setMessageValue(EncodedCarrier carrier) {
		this.carrier = carrier;
	}

	/**
	 * @param messType
	 *            the type to set
	 */
	public void setMessageType(MessageType messType) {
		this.messType = messType;
	}

	@Override
	public String toString() {
		return "Type=" + messType + ", sent from [" + from + "] to [" + to + "] message=[" + theMessage + "]";
	}

	/**
	 * @param theNameFromTheServer
	 * @param theNameFromTheClient
	 * @return Do these names match?
	 */
	public static boolean isUsernameMatch(final String theNameFromTheServer, final String theNameFromTheClient) {
		if (theNameFromTheServer == null || theNameFromTheClient == null) {
			System.out.println("MessageCarrier.isUsernameMatch() -- one of the names is null");
			return false;
		}
		if ("NULL".equalsIgnoreCase(theNameFromTheServer) || "NULL".equalsIgnoreCase(theNameFromTheClient)) {
			System.out.println("MessageCarrier.isUsernameMatch() -- one of the names is \"NULL\"");
			return true;
		}
		boolean rv = theNameFromTheServer.equals(theNameFromTheClient) || theNameFromTheServer.startsWith(theNameFromTheClient)
				|| theNameFromTheClient.startsWith(theNameFromTheServer);
		// System.out.println(new Date() + " MessageCarrier.isUsernameMatch() -- the names are [" + theNameFromTheServer + "] and ["
		// + theNameFromTheClient + "] -- returning " + rv);
		// if ((!rv) && theNameFromTheClient.startsWith(theNameFromTheServer)) {
		// System.err.println("BACKWARDS: " + theNameFromTheClient + " " + theNameFromTheServer);
		// new Exception().printStackTrace();
		// }
		return rv;
	}

	/**
	 * Read the private key so we can initialize out signature
	 * 
	 * @return Did we properly initialize the signature?
	 */
	public static void initializeSignature() {
		if (!signatureInitialized)
			try {
				SignXMLUsingDSAKeys.setupDSA(PRIVATE_KEY_LOCATION, getFullSelectorName());
				signatureInitialized = true;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
	}

	// Eclipse-generated hashCode and equals
	@Override
	public int hashCode() {
		final int prime = 41;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((theMessage == null) ? 0 : theMessage.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		result = prime * result + ((messType == null) ? 0 : messType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageCarrierXML other = (MessageCarrierXML) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (theMessage == null) {
			if (other.theMessage != null)
				return false;
		} else if (!theMessage.equals(other.theMessage))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		if (messType != other.messType)
			return false;
		return true;
	}

	private static String convertDocumentToString(Document doc) {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
			// below code to remove XML declaration
			// transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			String output = writer.getBuffer().toString();
			return output;
		} catch (TransformerException e) {
			e.printStackTrace();
		}

		return null;
	}
}
