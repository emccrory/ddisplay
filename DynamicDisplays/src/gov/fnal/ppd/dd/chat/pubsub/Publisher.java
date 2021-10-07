package gov.fnal.ppd.dd.chat.pubsub;

public interface Publisher {

	/**
	 * @return The topic that this publisher handles.
	 */
	public Topic getTopic();

	/**
	 * Publish a message to all the clients who acare about this topic
	 * 
	 * @param m - the message
	 */
	void publish(Message m);

}