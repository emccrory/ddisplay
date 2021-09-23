package gov.fnal.ppd.dd.chat.pubsub;

public class Topic {

	private TopicEnum name;

	public Topic(TopicEnum name) {
		this.name = name;
	}

	public TopicEnum getName() {
		return name;
	}
}
