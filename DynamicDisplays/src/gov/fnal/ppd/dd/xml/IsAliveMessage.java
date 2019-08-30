package gov.fnal.ppd.dd.xml;

public class IsAliveMessage extends MessagingDataXML {
	public IsAliveMessage() {
	}
	@Override
	public boolean willNotChangeAnything() {
		return true;
	}
}
