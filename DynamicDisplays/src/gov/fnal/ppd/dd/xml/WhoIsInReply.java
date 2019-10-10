package gov.fnal.ppd.dd.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

/**
 * Encapsulate the reply to a "WHOISIN" request
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@SuppressWarnings("javadoc")
public class WhoIsInReply extends MessagingDataXML {
	private List<ClientInformation> clients = new ArrayList<ClientInformation>();

	public WhoIsInReply() {
	}

	@XmlElement
	public ClientInformation[] getClient() {
		return clients.toArray(new ClientInformation[0]);
	}

	public void setClient(ClientInformation[] clients) {
		for (ClientInformation C : clients)
			this.clients.add(C);
	}

	@Override
	public boolean willNotChangeAnything() {
		return true;
	}
}
