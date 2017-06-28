package gov.fnal.ppd.dd.xml;

import gov.fnal.ppd.dd.signage.SignageContent;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Message from a Control Panel to the Display Server to change the thing being shown on a Display
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012-14
 */
@SuppressWarnings("javadoc")
@XmlRootElement
public class EnhancedChangeChannel extends EnhancedEncodedCarrier implements Serializable {

	private static final long	serialVersionUID	= -1131806965385199857L;
	protected SignageContent	content;
	protected int				displayNum;
	protected int				screenNum;

	@XmlElement
	public ChannelSpec getChannelSpec() {
		ChannelSpec cs = new ChannelSpec(content);
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
			// ChannelCategory category = s.getCategory();
			// String name = s.getName();
			// URI uri = s.getUri();
			// PlainURLChannel channel = new PlainURLChannel(uri.toURL());
			// channel.setCategory(category);
			// channel.setName(name);
			// channel.setCode(s.getCode());
			// channel.setTime(s.getTime());
			// channel.setExpiration(s.getExpiration());
			// channel.setFrameNumber(s.getFrameNumber());
			// content = channel;
			content = s.getContent();
			// } catch (MalformedURLException e) {
			// e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}