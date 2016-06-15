/*
 * MessageType
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.chat;

/**
 * The sorts of messages that the Messaging Server can understand
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
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
	ERROR,

	/**
	 * This is a message that specifies an emergency situation. It is not completely clear yet what this means, but it is likely to
	 * be something from the Office of Communications that needs to go to every client in the system (especially the displays).
	 */
	EMERGENCY,

	/**
	 * This is a read-only message that is a reply to a non-read-only message
	 */
	REPLY;

	/**
	 * @return -- Is this sort of message read-only?
	 */
	public boolean isReadOnly() {
		return this != MESSAGE && this != EMERGENCY;
	}
}
