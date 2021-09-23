package gov.fnal.ppd.dd.chat.pubsub;

import java.util.Hashtable;
import java.util.List;

/**
 * The bare-bones implementation of a Server that will distribute the messages to the clients subscribed to the topics.
 * 
 * This server can only exist within the JVM where everything is running. The implementation in the Dynamic Displays system would
 * need to have this class be in a separate JVM, on a different node on the Internet.
 * 
 * The way I would probably implement this is the same way that Displays were implemented - with a Facade within the JVM that the
 * client connects to, and them that Facade will have the logic to contact the server on the Internet.
 * 
 * Retrieved from https://riptutorial.com/design-patterns/example/6498/publish-subscribe-in-java in Sept 2021
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class ContentServer {
	private Hashtable<Topic, List<Subscriber>>	subscriberLists;

	private static ContentServer				serverInstance;

	public static ContentServer getInstance() {
		if (serverInstance == null) {
			serverInstance = new ContentServer();
		}
		return serverInstance;
	}

	private ContentServer() {
		this.subscriberLists = new Hashtable<>();
	}

	public boolean sendMessage(Topic t, Message m) {
		List<Subscriber> subs = subscriberLists.get(t);
		for (Subscriber s : subs) {
			s.receivedMessage(t, m);
		}
		return true;
	}

	public void registerSubscriber(Subscriber s, Topic t) {
		subscriberLists.get(t).add(s);
	}
}
