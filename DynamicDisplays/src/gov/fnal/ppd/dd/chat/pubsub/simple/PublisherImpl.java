package gov.fnal.ppd.dd.chat.pubsub.simple;

import gov.fnal.ppd.dd.chat.pubsub.Message;
import gov.fnal.ppd.dd.chat.pubsub.Publisher;
import gov.fnal.ppd.dd.chat.pubsub.Topic;

/**
 * Retrieved from https://riptutorial.com/design-patterns/example/6498/publish-subscribe-in-java in Sept 2021
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class PublisherImpl implements Publisher {
	private Topic topic;

	public PublisherImpl(Topic t) {
		this.topic = t;
	}

	/* (non-Javadoc)
	 * @see gov.fnal.ppd.dd.chat.pubsub.Publisher#publish(gov.fnal.ppd.dd.chat.pubsub.Message)
	 */
	@Override
	public void publish(Message m) {
		ContentServerImpl.getInstance().sendMessage(this.topic, m);
	}
}
