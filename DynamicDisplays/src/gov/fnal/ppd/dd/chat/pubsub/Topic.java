package gov.fnal.ppd.dd.chat.pubsub;

import java.util.ArrayList;
import java.util.List;

/**
 * This class has been changed from the RipTutorial version a little
 *
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class Topic {
	private final String				title;

	private static final List<Topic>	allTopics	= new ArrayList<Topic>();

	public Topic(String t) {
		this.title = t;
		allTopics.add(this);
	}

	@Override
	public String toString() {
		return title;
	}

	@Override
	public int hashCode() {
		final int prime = 43;
		int result = 1;
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Topic other = (Topic) obj;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	public static List<Topic> getTopics() {
		return allTopics;
	}
}
