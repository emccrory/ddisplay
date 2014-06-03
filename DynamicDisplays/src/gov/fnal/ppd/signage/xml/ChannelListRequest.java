package gov.fnal.ppd.signage.xml;

import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.changer.ChannelCategory;
import gov.fnal.ppd.signage.changer.ChannelCatalogFactory;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request from a Control Panel to the Display Server for the list of Channels. Also is the reply
 * message from the Server that contains the list of Channels
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012
 */
@XmlRootElement
public class ChannelListRequest extends EncodedCarrier {
	private Map<String, SignageContent> list = ChannelCatalogFactory.getInstance().getPublicChannels();

	private ChannelCategory category = ChannelCategory.PUBLIC;

	@XmlElement
	public ChannelCategory getChannelCategory() {
		return category;
	}

	public void setChannelCategory( ChannelCategory c ) {
		category = c;
	}
}
