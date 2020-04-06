package gov.fnal.ppd.dd.xml;

import javax.xml.bind.annotation.XmlSeeAlso;

import gov.fnal.ppd.dd.xml.messages.AreYouAliveMessage;
import gov.fnal.ppd.dd.xml.messages.ChangeChannel;
import gov.fnal.ppd.dd.xml.messages.ChangeChannelByNumber;
import gov.fnal.ppd.dd.xml.messages.ChangeChannelList;
import gov.fnal.ppd.dd.xml.messages.ChangeChannelReply;
import gov.fnal.ppd.dd.xml.messages.EmergencyMessXML;
import gov.fnal.ppd.dd.xml.messages.ErrorMessage;
import gov.fnal.ppd.dd.xml.messages.LoginMessage;
import gov.fnal.ppd.dd.xml.messages.WhoIsInMessage;
import gov.fnal.ppd.dd.xml.messages.WhoIsInReply;
import gov.fnal.ppd.dd.xml.messages.YesIAmAliveMessage;

@XmlSeeAlso(value = { ChangeChannel.class, ChangeChannelList.class, ChangeChannelByNumber.class, YesIAmAliveMessage.class,
		EmergencyMessXML.class, ErrorMessage.class, AreYouAliveMessage.class, LoginMessage.class, ChangeChannelReply.class,
		SubscriptionSubject.class, WhoIsInMessage.class, WhoIsInReply.class })

public class MessagingDataXML {

	public MessagingDataXML() {
	}

	/**
	 * Does this message change something, for example, change the channel?
	 * 
	 * @return false - this will, by default, change something;
	 */
	public boolean willNotChangeAnything() {
		return true;
	}
}
