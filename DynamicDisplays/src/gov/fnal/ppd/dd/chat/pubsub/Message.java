package gov.fnal.ppd.dd.chat.pubsub;

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
