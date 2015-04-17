package gov.fnal.ppd.dd.xml;

import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.channel.PlainURLChannel;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.net.MalformedURLException;
import java.net.URI;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Message from a Control Panel to the Display Server to change the thing being shown on a Display
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012-14
 */
@SuppressWarnings("javadoc")
@XmlRootElement
public class ChangeChannel extends EncodedCarrier {

	protected SignageContent	content;
	protected int				displayNum;
	protected int				screenNum;

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

	@XmlElement
	public int getScreenNumber() {
		return screenNum;
	}

	public void setScreenNumber(int s) {
		screenNum = s;
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
			channel.setCode(s.getCode());
			channel.setTime(s.getTime());
			channel.setFrameNumber(s.getFrameNumber());
			content = channel;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
