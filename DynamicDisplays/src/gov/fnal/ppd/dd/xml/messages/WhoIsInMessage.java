package gov.fnal.ppd.dd.xml.messages;

import gov.fnal.ppd.dd.xml.MessagingDataXML;

/**
 * This message is used to get a list of who is logged in to the messaging server.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class WhoIsInMessage extends MessagingDataXML {
	public WhoIsInMessage() {
	}

	@Override
	public boolean willNotChangeAnything() {
		return true;
	}
	
}
