package gov.fnal.ppd.chat;

/**
 * The sorts of messages that the Messaging Server can understand
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
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
	 * A request from the client to logout from the messaging server
	 */
	LOGOUT
}