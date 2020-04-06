package gov.fnal.ppd.dd.xml.messages;

import javax.xml.bind.annotation.XmlElement;

import gov.fnal.ppd.dd.xml.ClientInformation;
import gov.fnal.ppd.dd.xml.MessagingDataXML;

/**
 * Encapsulate information about a client node in the Dynamic Displays system
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@SuppressWarnings("javadoc")
public class YesIAmAliveMessage extends MessagingDataXML {
	private ClientInformation client;

	public YesIAmAliveMessage() {
		this(new ClientInformation("unspecified", System.currentTimeMillis()));
	}

	public YesIAmAliveMessage(ClientInformation client) {
		this.client = client;
	}

	@XmlElement
	public ClientInformation getClient() {
		return client;
	}

	public void setClient(ClientInformation client) {
		this.client = client;
	}

	@Override
	public boolean willNotChangeAnything() {
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": client=[" + client + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 61;
		int result = 1;
		result = prime * result + ((client == null) ? 0 : client.hashCode());
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
		YesIAmAliveMessage other = (YesIAmAliveMessage) obj;
		if (client == null) {
			if (other.client != null)
				return false;
		} else if (!client.equals(other.client))
			return false;
		return true;
	}

}
