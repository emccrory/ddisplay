package gov.fnal.ppd.dd.chat.pubsub;

public interface Subscriber {

	/**
	 * 
	 * @param t - The topic of interest
	 */
	void addTopicSubscription(Topic t);

	/**
	 * This is called when a message is received
	 * @param t - The topic
	 * @param m - The message
	 */
	void receivedMessage(Topic t, Message m);

}