/*
 * MessageCarrier
 *
 * Taken from the Internet on 5/12/2014. @see <a
 * href="http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/" dreamincode.net</a>
 * 
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.ZZattic;

import static gov.fnal.ppd.dd.GlobalVariables.PRIVATE_KEY_LOCATION;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.SignedObject;

import gov.fnal.ppd.dd.chat.NoPrivateKeyException;
import gov.fnal.ppd.dd.util.ObjectSigning;

/**
 * This class holds the messages that will be exchanged between the Clients and the Server. When talking from a Java Client to a
 * Java Server, it is a lot easier to stream Java objects, so that is what we do here.
 * 
 * TODO -- replace this class and the XML classes with one, unified message, probably in JSON. It would be best, at that time, to
 * stop streaming a Java object here so that other languages can get into the game--we would just stream ASCII messages.
 * 
 * Off the top of my head, here is what a JSON message that carries all the information might looks like.
 * 
 * <pre>
 * { "message": {
 *     "Type"    : "message",
 *     "To"      : "destinationClientName",
 *     "From"    : "sourceClientName",
 *     "IP"      : "131.225.1.1",
 *     "Date"    : "Jan 1, 2015 12:34:56 CST",
 *     "Display" : "12",
 *     "Screen"  : "0",
 *     "Content" : {
 *     	    "Type" : "Web Page",
 *          "Name" : "Channel Name",
 *          "URI"  : "http://www.fnal.gov",
 *          "Time" : "0",
 *          "Code" : "0",         
 *     }
 *     "Signature" : "82736152635fe62de82ab7fe901ab82",
 *   }
 * }
 * </pre>
 * 
 * As stated above, if/when we change to this JSON scheme, it will be important NOT to stream an object.
 * 
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 * @deprecated in favor of MessageCarrierXML
 */
public class MessageCarrier implements Serializable {

	protected static final long	serialVersionUID	= 1112122200L;

	private MessageType			type;
	private String				message;
	private String				to;
	private String				from;

	/**
	 * @param from
	 * @param to
	 * @param message
	 * @return A reply message from a client back to the requester
	 */
	public static MessageCarrier getReplyMessage(final String from, final String to, final String message) {
		return new MessageCarrier(MessageType.REPLY, from, to, message);
	}

	/**
	 * @return A logout message for the messaging server
	 */
	public static MessageCarrier getLogout() {
		return new MessageCarrier(MessageType.LOGOUT, "");
	}

	/**
	 * @param username
	 * @return A login message for the messaging server
	 */
	public static MessageCarrier getLogin(final String username) {
		return new MessageCarrier(MessageType.LOGIN, username);
	}

	/**
	 * @param from
	 * @return A message for the messaging server asking for all the clients that are connected
	 */
	public static MessageCarrier getWhoIsIn(final String from) {
		return new MessageCarrier(MessageType.WHOISIN, from, "NULL", "");
	}

	/**
	 * @param from
	 * @param to
	 *            The name of the client for whom this message is intended
	 * @return A message for the messaging server asking if a client is alive
	 */
	public static MessageCarrier getIsAlive(final String from, final String to) {
		return new MessageCarrier(MessageType.ISALIVE, from, to, "");
	}

	/**
	 * @param from
	 * @param to
	 * @param message
	 * @return A message to the messaging server that says, "Yes, I am alive"
	 */
	public static MessageCarrier getIAmAlive(final String from, final String to, String message) {
		return new MessageCarrier(MessageType.AMALIVE, from, to, message);
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
	public static MessageCarrier getErrorMessage(final String from, final String to, final String message) {
		return new MessageCarrier(MessageType.ERROR, from, to, message);
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
	public static MessageCarrier getEmergencyMessage(final String from, final String to, final String message) {
		// Stream the Emergency Message
		return new MessageCarrier(MessageType.EMERGENCY, from, to, message);
	}

	/**
	 * @param from
	 * @param to
	 * @param subject
	 * @return the message itself that contains the subscription subject
	 */
	public static MessageCarrier getSubscribeMessage(final String from, final String to, final String subject) {
		return new MessageCarrier(MessageType.SUBSCRIBE, from, to, subject);
	}

	private MessageCarrier(final MessageType type, final String message) {
		this(type, "NULL", "NULL", message);
	}

	/**
	 * @param type
	 *            -- Which type of message is this?
	 * @param message
	 *            -- The body of the message (a simple String)
	 */
	private MessageCarrier(final MessageType type, final String from, final String to, final String message) {
		this.type = type;
		this.message = message;
		this.from = from;
		this.to = to;
		if (type.isReadOnly())
			return;
		if (!initializeSignature()) {

		}
	}

	/**
	 * @return The type of the message
	 */
	public MessageType getMessageType() {
		return type;
	}

	/**
	 * @return The body of the message
	 */
	public String getMessageValue() {
		return message;
	}

	/**
	 * @return Who is this message intended to be received by?
	 */
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

	@Override
	public String toString() {
		return "Type=" + type + ", sent from [" + from + "] to [" + to + "] message=[" + message + "]";
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
	 * @param username
	 *            The user name to check
	 * @return Is this message intended for this user?
	 */
	// public boolean isThisForMe(final String username) {
	// boolean rv = to == null || to.equalsIgnoreCase("NULL") || to.equals(username) || to.startsWith(username);
	//
	// if ((!rv) && username.startsWith(to)) {
	// System.out.println("MessageCarrier.isForMe(): Things are backwards, msg.to=[" + to + "], username=[" + username + "]");
	// new Exception().printStackTrace();
	// }
	//
	// return rv;
	// }

	/**
	 * Read the private key so we can initialize out signature
	 * 
	 * @return Did we properly initialize the signature?
	 */
	public static boolean initializeSignature() {
		try {
			ObjectSigning.getInstance().loadPrivateKey(PRIVATE_KEY_LOCATION);
			return true;
		} catch (Exception e) {
			System.out.println("Problems loading private key for new client");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * @return -- A Signed version of this MessageCarrier object
	 * @throws NoPrivateKeyException
	 *             -- If the originator of this message does not have a private key, it cannot generator a signed object.
	 */
	public SignedObject getSignedObject() throws NoPrivateKeyException {
		try {
			return ObjectSigning.getInstance().getSignedObject(this);
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param signedObject
	 *            -- the object that (should be) the holder of "this"
	 * @return -- Has the signedObject been properly signed?
	 * @deprecated
	 */
	public String verifySignedObject(final SignedObject signedObject) {
			// Second, verify that the message in the signedObject is me
		try {
			MessageCarrier signedMessage = (MessageCarrier) signedObject.getObject();

			if (!signedMessage.equals(this))
				// 2. The object being tested is not equal to this!
				return "The message being checked has been modified";
		} catch (IOException e) {
			e.printStackTrace();
			// 3. Casting exception!
			return "There was an IO exception in reading the message";
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return "The returned message was not of type " + MessageCarrier.class.getCanonicalName();
		}

		// Now verify the signature
		ObjectSigning signing = ObjectSigning.getPublicSigning(getMessageOriginator());
		if (signing == null)
			// 4. No public key found
			return "There is no public key for client '" + getMessageOriginator() + "'";

		// 5. Does the signature actually match?
		return signing.verifySignature(signedObject);
	}

	// Eclipse-generated hashCode and equals
	@Override
	public int hashCode() {
		final int prime = 41;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		MessageCarrier other = (MessageCarrier) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

}
