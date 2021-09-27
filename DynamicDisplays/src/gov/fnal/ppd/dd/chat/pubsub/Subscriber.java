package gov.fnal.ppd.dd.chat.pubsub;

public interface Subscriber {

	void addTopicSubscription(Topic t);

	void receivedMessage(Topic t, Message m);

}