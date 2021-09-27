package gov.fnal.ppd.dd.chat.pubsub.simple;

import gov.fnal.ppd.dd.chat.pubsub.Message;
import gov.fnal.ppd.dd.chat.pubsub.Subscriber;
import gov.fnal.ppd.dd.chat.pubsub.Topic;

/**
 * Retrieved from https://riptutorial.com/design-patterns/example/6498/publish-subscribe-in-java in Sept 2021
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class SubscriberClient implements Subscriber {

	// Note this syntax for passing variable arguments.
	public SubscriberClient(Topic... topics) {
		for (Topic t : topics) {
			ContentServerImpl.getInstance().registerSubscriber(this, t);
		}
	}

	/* (non-Javadoc)
	 * @see gov.fnal.ppd.dd.chat.pubsub.Subscriber#addTopicSubscription(gov.fnal.ppd.dd.chat.pubsub.Topic)
	 */
	@Override
	public void addTopicSubscription(Topic t) {
		ContentServerImpl.getInstance().registerSubscriber(this, t);
	}

	/* (non-Javadoc)
	 * @see gov.fnal.ppd.dd.chat.pubsub.Subscriber#receivedMessage(gov.fnal.ppd.dd.chat.pubsub.Topic, gov.fnal.ppd.dd.chat.pubsub.Message)
	 */
	@Override
	public void receivedMessage(Topic t, Message m) {
		switch (t) {
		case TopicOne:
			break;
		case TopicTwo:
			break;
		}
	}
}