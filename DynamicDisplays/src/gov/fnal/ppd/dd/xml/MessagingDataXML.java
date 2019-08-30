package gov.fnal.ppd.dd.xml;

import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso(value = { ChangeChannel.class, ChangeChannelList.class, ChangeChannelByNumber.class, AmAliveMessage.class,
		EmergencyMessXML.class, ErrorMessage.class, IsAliveMessage.class, LoginMessage.class, SubscriptionSubject.class,
		WhoIsInMessage.class, WhoIsInReply.class })
public class MessagingDataXML {

	public MessagingDataXML() {
	}

	/**
	 * Does this message change something, for example, change the channel?
	 * 
	 * @return false - this will, by default, change something.
	 */
	public boolean willNotChangeAnything() {
		return false;
	}
}
