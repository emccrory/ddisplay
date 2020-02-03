/*
 * MessageType
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.ZZattic;

/**
 * The sorts of messages that the Messaging Server can understand
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @deprecated - user MessageTypeXML, or nothing at all (mostly, this is done via instanceof, now)
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
	REPLY,
	
	/*
	 * TODO - New message types of SubscribeTo and UnsubscribeFrom 
	 * 
	 * (Well, I can't really think of a use case for UnsubscribeFrom, but it should probably be there anyway - for completeness)
	 * 
	 * Implementing Subscriptions means that the Emergency message type is irrelevant (and maybe some other stuff, too).
	 * 
	 * Clients can subscribe to various levels of emergency messages, e.g.,
	 * /emergency/* -- all emergency messages
	 * /emergency/[high|medium|low] - or something like that.
	 * 
	 * And then regular message subscription titles can be of the form
	 * /change/[location number]/[display number] - Maybe?
	 * 
	 * or
	 * 
	 * /change/[display index number]
	 * 
	 * I'll have to think about this more.  (3/23/17)
	 * 
	 * One interim solution is to change the way the server sorts and distributes these messages.  Instead of having a solitary list of connected clients,
	 * add a second list that has the possible subscription phrases.  In this initial implementation, that phrase will be, simply, the name of the client.
	 * 
	 */

	/**
	 * This message tells the recipient (presumably the server) which subject they want to subscribe to.
	 */
	SUBSCRIBE;
	
	/**
	 * @return -- Is this sort of message read-only?
	 */
	public boolean isReadOnly() {
		return this != MESSAGE && this != EMERGENCY;
	}
}
