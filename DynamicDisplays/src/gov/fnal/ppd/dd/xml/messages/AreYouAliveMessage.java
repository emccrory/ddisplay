package gov.fnal.ppd.dd.xml.messages;

import gov.fnal.ppd.dd.xml.MessagingDataXML;

public class AreYouAliveMessage extends MessagingDataXML {
	public AreYouAliveMessage() {
	}

	@Override
	public boolean willNotChangeAnything() {
		return true;
	}
}
