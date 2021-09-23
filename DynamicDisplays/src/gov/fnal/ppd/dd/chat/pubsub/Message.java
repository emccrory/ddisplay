package gov.fnal.ppd.dd.chat.pubsub;

/**
 * Retrieved from https://riptutorial.com/design-patterns/example/6498/publish-subscribe-in-java in Sept 2021
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class Message {

	private Topic topic;
	private String contents;

	public Message(Topic t, String contents){
		this.setTopic(t);
		this.setContents(contents);
	}

	public Topic getTopic() {
		return topic;
	}

	public void setTopic(Topic topic) {
		this.topic = topic;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}
	
	
}
