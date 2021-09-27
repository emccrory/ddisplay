package gov.fnal.ppd.dd.chat.pubsub;

import gov.fnal.ppd.dd.chat.pubsub.simple.SubscriberClient;

public interface ContentServer {

	/**
	 * Send a message, for the specified Topic, to all the clients who care
	 * 
	 * @param t - The topic of this message
	 * @param m - The message
	 * @return - Did a client care about this message?
	 */
	boolean sendMessage(Topic t, Message m);

	/**
	 * Register that a client is interested in a topic
	 * 
	 * @param s - The client (subscriber) interested in this topic
	 * @param t - The topic of interest
	 */
	void registerSubscriber(SubscriberClient s, Topic t);

}