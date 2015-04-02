package gov.fnal.ppd.dd.chat;

/**
 * The sorts of messages that the Messaging Server can understand
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public enum MessageType {
	/**
	 * Who is connected to the server?
	 */
	WHOISIN,

	/**
	 * A regular message that the server should send to (at this time) everyone
	 */
	MESSAGE,

	/**
	 * A request from the client to login the messaging server. Also provides a username
	 */
	LOGIN,

	/**
	 * A request from the client to logout from the messaging server
	 */
	LOGOUT,

	/**
	 * Is this client actually alive? [Or do I use "PING"?]
	 */
	ISALIVE,

	/**
	 * This client is actually alive [Or do I use "PONG"?]
	 */
	AMALIVE,
	
	/**
	 * There is an error of some sort here.
	 */
	ERROR;

	/**
	 * @return -- Is this sort of message read-only?
	 */
	public boolean isReadOnly() {
		return this != MESSAGE;
	}
}
