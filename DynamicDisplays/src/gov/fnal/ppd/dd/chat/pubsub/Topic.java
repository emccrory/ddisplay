package gov.fnal.ppd.dd.chat.pubsub;

public class Topic {

	// Maybe this class is not necessary - maybe only the Enum is required.

	private TopicEnum name;

	public Topic(TopicEnum name) {
		this.name = name;
	}

	public TopicEnum getName() {
		return name;
	}
}
