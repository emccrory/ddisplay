/*
 * MessageCarrier
 *
 * Taken from the Internet on 5/12/2014. @see <a
 * href="http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/" dreamincode.net</a>
 * 
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.xml;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import gov.fnal.ppd.dd.emergency.EmergencyMessage;
import gov.fnal.ppd.dd.xml.messages.AreYouAliveMessage;
import gov.fnal.ppd.dd.xml.messages.ChangeChannelReply;
import gov.fnal.ppd.dd.xml.messages.EmergencyMessXML;
import gov.fnal.ppd.dd.xml.messages.ErrorMessage;
import gov.fnal.ppd.dd.xml.messages.LoginMessage;
import gov.fnal.ppd.dd.xml.messages.WhoIsInMessage;
import gov.fnal.ppd.dd.xml.messages.YesIAmAliveMessage;

/**
 * This class holds the messages that will be exchanged between the Clients and the Server.
 * 
 * This is a replacement that is in full XML. Streamed objects in Java are about to be deprecated, so we are streaming pure-ascii
 * XML documents.
 * 
 * Here is what an XML message that carries all the information looks like.
 * 
 * <pre>
 
  <messageCarrier type="TYPE"> # The value of the type is the sort of object that is being transmitted in the tag, messageValue
    <messageRecipient> -- name of the recipient of this message -- </messageRecipient> 
    <messageOriginator> -- name of the sender of this message -- </messageOriginator> 
   	<messageTimeStamp>Jan  1, 2019 01:23:45 CST</messageTimeStamp>
    <messageValue>  
    
    # This is an XML representation of the transmitted object        

    </messageValue> 
    <signature> 
       Gobbledygook  
    </signature>
  </messageCarrier>
 * 
 * </pre>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@XmlRootElement
public class MessageCarrierXML implements Serializable {

	protected static final long	serialVersionUID	= 1112122200L;

	// private MessageTypeXML messType;
	private MessagingDataXML	theMessage;
	private String				to;
	private String				from;
	private long				creationDate;

	private boolean validSignature;

	/**
	 * @param name
	 * @return A logout message for the messaging server
	 */
	public static MessageCarrierXML getLogout(final String name) {
		LoginMessage lm = new LoginMessage(name);
		return new MessageCarrierXML("", name, lm);
	}

	/**
	 * @param username
	 * @return A login message for the messaging server
	 */
	public static MessageCarrierXML getLogin(final String name) {
		LoginMessage lm = new LoginMessage(name);
		return new MessageCarrierXML("", name, lm);
	}

	/**
	 * @param from
	 * @return A message for the messaging server asking for all the clients that are connected
	 */
	public static MessageCarrierXML getWhoIsIn(final String from, final String to) {
		WhoIsInMessage wiim = new WhoIsInMessage();
		return new MessageCarrierXML(from, to, wiim);
	}

	/**
	 * @param from
	 * @param to
	 *            The name of the client for whom this message is intended
	 * @return A message for the messaging server asking if a client is alive
	 */
	public static MessageCarrierXML getIsAlive(final String from, final String to) {
		AreYouAliveMessage iam = new AreYouAliveMessage();
		return new MessageCarrierXML(from, to, iam);
	}

	/**
	 * @param from
	 * @param to
	 * @param message
	 * @return A message to the messaging server that says, "Yes, I am alive"
	 */
	public static MessageCarrierXML getIAmAlive(final String from, final String to, long time) {
		ClientInformation client = new ClientInformation(from, time);
		YesIAmAliveMessage aam = new YesIAmAliveMessage(client);
		return new MessageCarrierXML(from, to, aam);
	}

	public static MessageCarrierXML getIAmAlive(String username, String serverName) {
		return getIAmAlive(username, serverName, System.currentTimeMillis());
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
		ErrorMessage em = new ErrorMessage(message);
		return new MessageCarrierXML(from, to, em);
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
	public static MessageCarrierXML getEmergencyMessage(final String from, final String to, final EmergencyMessage message) {
		EmergencyMessXML em = new EmergencyMessXML(message);
		return new MessageCarrierXML(from, to, em);
	}

	/**
	 * @param from
	 *            - Who is this message from
	 * @param to
	 *            - Who is this message to
	 * @param replyMessage
	 *            - The contents of the message
	 * @return A message object for replying to something or other
	 */
	public static MessageCarrierXML getReplyMessage(String from, String to, ChangeChannelReply replyMessage) {
		return new MessageCarrierXML(from, to, replyMessage);
	}

	/**
	 * @param messType
	 *            -- Which type of message is this?
	 * @param message
	 *            -- The body of the message (a simple String)
	 */
	public MessageCarrierXML(final String from, final String to, final MessagingDataXML message) {
		this.theMessage = message;
		this.from = from;
		this.to = to;
		creationDate = System.currentTimeMillis();
	}

	/**
	 * 
	 */
	public MessageCarrierXML() {
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
	public String getMessageOriginator() {
		return from;
	}

	/**
	 * @param from
	 *            Who is this message from
	 */
	public void setMessageOriginator(final String from) {
		this.from = from;
	}

	/**
	 * @return the carrier
	 */
	public MessagingDataXML getMessageValue() {
		return theMessage;
	}

	/**
	 * @param message
	 *            the carrier to set
	 */
	public void setMessageValue(MessagingDataXML message) {
		this.theMessage = message;
	}

	@XmlElement
	public long getTimeStamp() {
		return creationDate;
	}

	public void setTimeStamp(long creationDate) {
		this.creationDate = creationDate;
	}

	public boolean isReadOnly() {
		return theMessage.willNotChangeAnything();
	}

	@Override
	public String toString() {
		return "Type=" + theMessage.getClass().getSimpleName() + ", sent from [" + from + "] to [" + to + "] message=[" + theMessage
				+ "]";
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

	// private SignedXMLDocument sXMLD = null;
	// public boolean isSignatureValid() {
	// if (sXMLD == null)
	// try {
	// sXMLD = new SignedXMLDocument(this);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// return sXMLD.getSignedDocument() != null;
	// }
	
	public boolean isSignatureValid() {
		return validSignature;
	}

	public void setSignatureIsValid(boolean signatureValid) {
		validSignature = signatureValid;		
	}
}
