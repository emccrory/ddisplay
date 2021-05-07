package gov.fnal.ppd.dd.xml.messages;

import gov.fnal.ppd.dd.xml.MessagingDataXML;

/**
 * A simple implementation of MesssagingDataXML, which is used by the server to see if a client is alive.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class AreYouAliveMessage extends MessagingDataXML {
	public AreYouAliveMessage() {
	}

	@Override
	public boolean willNotChangeAnything() {
		return true;
	}
}
