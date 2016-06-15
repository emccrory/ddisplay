package gov.fnal.ppd.dd.xml;

import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request from a Control Panel to the Display Server for the list of Channels. Also is the reply
 * message from the Server that contains the list of Channels
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012
 */
@SuppressWarnings("javadoc")
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
			ChannelSpec cs = new ChannelSpec(list.get(key));
			retval[i++] = cs;
		}
		return retval;
	}

	public void setChannelList( String [] list ) {}

	public void setRequest( boolean b ) {
		isARequest = b;
	}

}
