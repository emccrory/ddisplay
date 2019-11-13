package gov.fnal.ppd.dd.xml;

import javax.xml.bind.annotation.XmlElement;

/**
 * Encapsulate a subscription subject (a.k.a., "topic") into an XML class
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@SuppressWarnings("javadoc")
public class SubscriptionSubject extends MessagingDataXML {

	private String topic;
	
	@XmlElement
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public SubscriptionSubject() {
		this("no topic");
	}

	public SubscriptionSubject(String name) {
		topic = name;
	}

	@Override
	public boolean willNotChangeAnything() {
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 101;
		int result = 1;
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubscriptionSubject other = (SubscriptionSubject) obj;
		if (topic == null) {
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		return true;
	}
	
}
