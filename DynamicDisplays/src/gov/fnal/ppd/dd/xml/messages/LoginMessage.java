package gov.fnal.ppd.dd.xml.messages;

import javax.xml.bind.annotation.XmlElement;

import gov.fnal.ppd.dd.xml.ClientInformation;
import gov.fnal.ppd.dd.xml.MessagingDataXML;

/**
 * Encapsulate an error message into an XML class
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@SuppressWarnings("javadoc")
public class LoginMessage extends MessagingDataXML {
	private ClientInformation client;

	public LoginMessage() {
		this(new ClientInformation("no client")); // The time information is not particularly important here.
	}

	public LoginMessage(String name) {
		this(new ClientInformation(name));
	}

	public LoginMessage(ClientInformation name) {
		this.client = name;
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
		return "Login for " + client;
	}
	@Override
	public int hashCode() {
		final int prime = 51;
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
		LoginMessage other = (LoginMessage) obj;
		if (client == null) {
			if (other.client != null)
				return false;
		} else if (!client.equals(other.client))
			return false;
		return true;
	}

}
