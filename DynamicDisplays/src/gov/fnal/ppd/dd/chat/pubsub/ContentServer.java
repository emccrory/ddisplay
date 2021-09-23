package gov.fnal.ppd.dd.chat.pubsub;

import java.util.Hashtable;
import java.util.List;

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
