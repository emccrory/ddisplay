package gov.fnal.ppd.chat;


import java.io.Serializable;
/**
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server. 
 * When talking from a Java Client to a Java Server a lot easier to pass Java objects, no 
 * need to count bytes or to wait for a line feed at the end of the frame
 * 
 * Taken from http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/ on 5/12/2014
 */
public class MessageCarrier implements Serializable {

	protected static final long serialVersionUID = 1112122200L;

	// The different types of message sent by the Client
	/**
	 * WHOISIN to receive the list of the users connected
	 */
	public static final int WHOISIN = 0;
	/**
	 * MESSAGE an ordinary message
	 */
	public static final int MESSAGE = 1;
	/**
	 * LOGOUT to disconnect from the Server
	 */
	public static final int LOGOUT = 2;
	private int type;
	private String message;
	
	/**
	 * @param type -- Which type of message is this?
	 * @param message -- The body of the message (a simple String)
	 */
	public  MessageCarrier(int type, String message) {
		this.type = type;
		this.message = message;
	}
	
	/**
	 * @return The type of the message
	 */
	public int getType() {
		return type;
	}
	/**
	 * @return The body of the message
	 */
	public String getMessage() {
		return message;
	}
}

