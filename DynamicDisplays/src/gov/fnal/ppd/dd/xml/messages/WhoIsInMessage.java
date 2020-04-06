package gov.fnal.ppd.dd.xml.messages;

import gov.fnal.ppd.dd.xml.MessagingDataXML;

public class WhoIsInMessage extends MessagingDataXML {
	public WhoIsInMessage() {
	}

	@Override
	public boolean willNotChangeAnything() {
		return true;
	}
	
}
