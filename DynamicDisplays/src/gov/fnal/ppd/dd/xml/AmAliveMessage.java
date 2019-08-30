package gov.fnal.ppd.dd.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Encapsulate information about a client node in the Dynamic Displays system
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@SuppressWarnings("javadoc")
public class AmAliveMessage extends MessagingDataXML {
	private ClientInformation client;

	public AmAliveMessage() {
		this(new ClientInformation("unspecified", System.currentTimeMillis()));
	}

	public AmAliveMessage(ClientInformation client) {
		this.client = client;
	}

	@XmlElement
	public ClientInformation getClient() {
		return client;
	}

	public void setClient(ClientInformation name) {
		this.client = name;
	}

	@Override
	public boolean willNotChangeAnything() {
		return true;
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
		AmAliveMessage other = (AmAliveMessage) obj;
		if (client == null) {
			if (other.client != null)
				return false;
		} else if (!client.equals(other.client))
			return false;
		return true;
	}

}
