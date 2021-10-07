package gov.fnal.ppd.dd.xml.messages;

import gov.fnal.ppd.dd.xml.MessagingDataXML;

/**
 * An implementation of MesssagingDataXML, which is used by the server to see if a client is alive.
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

	/*
	 * Note - there is no need to override toString(). The default one, which includes the class name and a hashCode, for example:
	 * 
	 * gov.fnal.ppd.dd.xml.messages.AreYouAliveMessage@7de1b574 
	 * 
	 * is almost perfect already.
	 */
}
