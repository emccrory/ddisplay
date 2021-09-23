package gov.fnal.ppd.dd.chat.pubsub;

public class Subscriber {
	public Subscriber(Topic... topics) {
		for (Topic t : topics) {
			ContentServer.getInstance().registerSubscriber(this, t);
		}
	}

	public void receivedMessage(Topic t, Message m) {
		switch (t.getName()) {
		case TopicOne:
			break;
		case TopicTwo:
			break;
		}
	}
}