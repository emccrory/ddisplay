package gov.fnal.ppd.chat;

import java.io.Serializable;

/**
 * This class defines the different type of messages that will be exchanged between the Clients and the Server. When talking from a
 * Java Client to a Java Server a lot easier to pass Java objects, no need to count bytes or to wait for a line feed at the end of
 * the frame
 * 
 * Taken from http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/ on 5/12/2014
 */
public class MessageCarrier implements Serializable {

	protected static final long	serialVersionUID	= 1112122200L;

	// The different types of message sent by the Client
	// /**
	// * WHOISIN to receive the list of the users connected
	// */
	// static final int WHOISIN = 0;
	// /**
	// * MESSAGE an ordinary message
	// */
	// static final int MESSAGE = 1;
	// /**
	// * LOGOUT to disconnect from the Server
	// */
	// static final int LOGOUT = 2;
	// private int type;
	private MessageType			type;
	private String				message;

	/**
	 * @param message
	 *            The ASCII message to send
	 * @return the object to send over the wire
	 */
	public static MessageCarrier getMessage(final String message) {
		return new MessageCarrier(MessageType.MESSAGE, message);
	}

	/**
	 * @return A logout message for the messaging server
	 */
	public static MessageCarrier getLogout() {
		return new MessageCarrier(MessageType.LOGOUT, "");
	}

	/**
	 * @return A message for the messaging server asking for all the clients that are connected
	 */
	public static MessageCarrier getWhoIsIn() {
		return new MessageCarrier(MessageType.WHOISIN, "");
	}

	/**
	 * @param clientName
	 *            The name of the client for whom this message is intended
	 * @return A message for the messaging server asking for all the clients that are connected
	 */
	public static MessageCarrier getAlive(String clientName) {
		return new MessageCarrier(MessageType.ALIVE, clientName);
	}

	/**
	 * @param type
	 *            -- Which type of message is this?
	 * @param message
	 *            -- The body of the message (a simple String)
	 */
	private MessageCarrier(MessageType type, String message) {
		this.type = type;
		this.message = message;
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

	@Override
	public String toString() {
		return "Type=" + type + ", message=[" + message + "]";
	}
}
