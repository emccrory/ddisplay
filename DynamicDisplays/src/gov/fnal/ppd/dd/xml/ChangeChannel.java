package gov.fnal.ppd.dd.xml;

import gov.fnal.ppd.dd.signage.SignageContent;

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
			content = s.getContent();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
