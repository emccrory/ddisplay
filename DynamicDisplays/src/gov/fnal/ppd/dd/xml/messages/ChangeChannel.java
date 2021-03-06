package gov.fnal.ppd.dd.xml.messages;

import javax.xml.bind.annotation.XmlElement;

import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.MessagingDataXML;

/**
 * Message from a Control Panel to the Display Server to change the thing being shown on a Display
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012-14
 */
@SuppressWarnings("javadoc")
public class ChangeChannel extends MessagingDataXML {

	protected SignageContent	content;
	protected int				displayNum;
	protected int				screenNum;
	protected int				channelNumber;

	@XmlElement
	public ChannelSpec getChannelSpec() {
		ChannelSpec cs = new ChannelSpec(content);
		if (getChannelNumber() != 0)
			cs.setNumber(getChannelNumber());
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
	public int getChannelNumber() {
		if ( channelNumber == 0 && content instanceof Channel ) 
			return ((Channel) content).getNumber();
		return channelNumber;
	}

	public void setChannelNumber(int c) {
		channelNumber = c;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + displayNum;
		result = prime * result + screenNum;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChangeChannel other = (ChangeChannel) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (displayNum != other.displayNum)
			return false;
		if (screenNum != other.screenNum)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return getDisplayNumber() + " " + getChannelNumber() + " " + getChannelSpec();
	}

	@Override
	public boolean willNotChangeAnything() {
		return false;
	}

}
