package gov.fnal.ppd.dd.xml;

public class AreYouAliveMessage extends MessagingDataXML {
	public AreYouAliveMessage() {
	}

	@Override
	public boolean willNotChangeAnything() {
		return true;
	}
}
