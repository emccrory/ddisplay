package gov.fnal.ppd.dd.chat.pubsub;

/**
 * Retrieved from https://riptutorial.com/design-patterns/example/6498/publish-subscribe-in-java in Sept 2021
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class Subscriber {
	
	// Note this syntax for passing variable arguments.
	public Subscriber(Topic... topics) {
		for (Topic t : topics) {
			ContentServer.getInstance().registerSubscriber(this, t);
		}
	}

	public void receivedMessage(Topic t, Message m) {
		switch (t) {
		case TopicOne:
			break;
		case TopicTwo:
			break;
		}
	}
}