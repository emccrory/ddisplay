package gov.fnal.ppd.signage.xml;

import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.changer.ChannelCategory;
import gov.fnal.ppd.signage.channel.PlainURLChannel;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Message from a Control Panel to the Display Server to change the thing being shown on a Display
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012
 */
@XmlRootElement
public class ChangeChannel extends EncodedCarrier {

	protected SignageContent	content;
	protected int				displayNum;

	@XmlElement
	public ChannelSpec getChannelSpec() {
		ChannelSpec cs = new ChannelSpec();
		cs.setContent(content);
		return cs;
	}

	@XmlElement
	public int getDisplayNumber() {
		return displayNum;
	}

	public void setDisplayNumber(int d) {
		displayNum = d;
	}
	
	
	public void setContent(SignageContent s) {
		content = s;
	}

	public void setChannelSpec(ChannelSpec s) {
		try {
			ChannelCategory category = s.getCategory();
			String name = s.getName();
			URI uri = s.getUri();
			PlainURLChannel channel = new PlainURLChannel(uri.toURL());
			channel.setCategory(category);
			channel.setName(name);
			content = channel;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
