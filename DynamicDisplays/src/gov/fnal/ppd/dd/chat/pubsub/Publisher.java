package gov.fnal.ppd.dd.chat.pubsub;

/**
 * Retrieved from https://riptutorial.com/design-patterns/example/6498/publish-subscribe-in-java in Sept 2021
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class Publisher {
	private Topic topic;

	public Publisher(Topic t) {
		this.topic = t;
	}

	public void publish(Message m) {
		ContentServer.getInstance().sendMessage(this.topic, m);
	}
}
