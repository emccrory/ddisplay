package gov.fnal.ppd.signage.xml;

import gov.fnal.ppd.signage.SignageContent;
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
public class ChannelListResponse extends EncodedCarrier {
	private Map<String, SignageContent> list = ChannelCatalogFactory.getInstance().getPublicChannels();

	private boolean isARequest;

	@XmlElement
	public ChannelSpec [] getChannelSpec() {
		if (isARequest)
			return null;
		ChannelSpec [] retval = new ChannelSpec [list.size()];
		int i = 0;
		for (String key: list.keySet()) {
			ChannelSpec cs = new ChannelSpec();
			cs.setContent(list.get(key));
			retval[i++] = cs;
		}
		return retval;
	}

	public void setChannelList( String [] list ) {}

	public void setRequest( boolean b ) {
		isARequest = b;
	}

}
