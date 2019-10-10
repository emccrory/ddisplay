package gov.fnal.ppd.dd.xml;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Message from a Control Panel to the Display Server to change the thing being shown on a Display, by sending only the channel
 * number
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012-16
 */
@SuppressWarnings("javadoc")
@XmlRootElement
public class ChangeChannelByNumber extends MessagingDataXML implements Serializable {

	private static final long serialVersionUID = 5381672365828058188L;
	protected int	channel;
	protected int	displayNum;
	protected int	screenNum;
	private long	checksum;

	@XmlElement
	public int getChannelNumber() {
		return channel;
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

	public void setChannelNumber(int s) {
		channel = s;
	}
	
	@XmlElement
	public long getChecksum() {
		return checksum;
	}

	public void setChecksum(long checksum) {
		this.checksum = checksum;
	}

	@Override
	public boolean willNotChangeAnything() {
		return false;
	}
}
