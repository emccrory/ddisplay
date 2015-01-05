package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.GlobalVariables.PRIVATE_KEY_LOCATION;
import static gov.fnal.ppd.dd.GlobalVariables.checkSignedMessages;
import gov.fnal.ppd.dd.testing.ObjectSigning;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.spec.InvalidKeySpecException;

/**
 * This class defines the different type of messages that will be exchanged between the Clients and the Server. When talking from a
 * Java Client to a Java Server, it is a lot easier to stream Java objects, so that is what we do here.
 * 
 * Taken from the internet on 5/12/2014. @see <a
 * href="http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/" dreamincode.net</a>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
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
	 *            The ASCII message to send
	 * @return the object to send over the wire
	 */
	public static MessageCarrier getMessage(final String from, final String to, final String message) {
		return new MessageCarrier(MessageType.MESSAGE, from, to, message);
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
		initializeSignature();
	}

	/**
	 * @return The type of the message
	 */
	public MessageType getType() {
		return type;
	}

	/**
	 * @return The body of the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return Who is this message intended to be received by?
	 */
	public String getTo() {
		return to;
	}

	/**
	 * @param to
	 *            Set to whom this message is aimed
	 */
	public void setTo(final String to) {
		this.to = to;
	}

	/**
	 * @return who is this message from?
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * @param from
	 *            Who is this message from
	 */
	public void setFrom(final String from) {
		this.from = from;
	}

	@Override
	public String toString() {
		return "Type=" + type + ", sent from [" + from + "] to [" + to + "] message=[" + message + "]";
	}

	/**
	 * @param username
	 *            The user name to check
	 * @return Is this message intended for this user?
	 */
	public boolean isThisForMe(final String username) {
		return to == null || to.equalsIgnoreCase("NULL") || to.equals(username);
	}

	private void initializeSignature() {
		try {
			ObjectSigning.getInstance().loadPrivateKey(PRIVATE_KEY_LOCATION);
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
			System.out.println("Problems loading private key for new client");
			e.printStackTrace();
		}
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
	 */
	public boolean verifySignedObject(final SignedObject signedObject) {
		if (!checkSignedMessages())
			return true;
		// assert (this.equals(signedObject.getObject()));

		ObjectSigning signing = ObjectSigning.getPublicSigning(getFrom());
		if (signing == null)
			return false;
		return signing.verifySignature(signedObject);
	}
}