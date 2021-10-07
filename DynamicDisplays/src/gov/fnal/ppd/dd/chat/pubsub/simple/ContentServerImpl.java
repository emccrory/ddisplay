package gov.fnal.ppd.dd.chat.pubsub.simple;

import java.util.Hashtable;
import java.util.List;

import gov.fnal.ppd.dd.chat.pubsub.ContentServer;
import gov.fnal.ppd.dd.chat.pubsub.Message;
import gov.fnal.ppd.dd.chat.pubsub.Subscriber;
import gov.fnal.ppd.dd.chat.pubsub.Topic;

/**
 * The bare-bones implementation of a server that will distribute messages to clients that are subscribed to topics.
 * 
 * This version of the server can only exist within the JVM where everything is running. The implementation in the Dynamic Displays
 * system would need to have this class be in a separate JVM, on a different node on the Internet.
 * 
 * The way I would probably implement this is the same way that Displays were implemented - with a Facade within the JVM that the
 * client connects to, and them that Facade will have the logic to contact the server on the Internet.
 * 
 * https://riptutorial.com/design-patterns/example/6498/publish-subscribe-in-java (Retrieved AWpt 2021)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class ContentServerImpl implements ContentServer {
	private Hashtable<Topic, List<SubscriberClient>>	subscriberLists;

	private static ContentServer						serverInstance;

	public static ContentServer getInstance() {
		if (serverInstance == null) {
			serverInstance = new ContentServerImpl();
		}
		return serverInstance;
	}

	private ContentServerImpl() {
		this.subscriberLists = new Hashtable<>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.fnal.ppd.dd.chat.pubsub.ContentServer#sendMessage(gov.fnal.ppd.dd.chat.pubsub.Topic,
	 * gov.fnal.ppd.dd.chat.pubsub.Message)
	 */
	@Override
	public int sendMessage(Topic t, Message m) {
		int retval = 0;
		List<SubscriberClient> subs = subscriberLists.get(t);
		for (Subscriber s : subs) {
			s.receivedMessage(t, m);
			retval++; // Someone was interested in this topic.
		}
		return retval;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.fnal.ppd.dd.chat.pubsub.ContentServer#registerSubscriber(gov.fnal.ppd.dd.chat.pubsub.Subscriber,
	 * gov.fnal.ppd.dd.chat.pubsub.Topic)
	 */
	@Override
	public void registerSubscriber(SubscriberClient s, Topic t) {
		subscriberLists.get(t).add(s);
	}
}
