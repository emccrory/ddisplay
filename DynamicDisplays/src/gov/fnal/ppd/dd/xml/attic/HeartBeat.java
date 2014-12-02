package gov.fnal.ppd.dd.xml.attic;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A simple heartbeat class for the Messaging Server to send out from time to time
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
@SuppressWarnings("javadoc")
@XmlRootElement
public class HeartBeat {
	private long	timeStamp;

	public long getTimeStamp() {
		return timeStamp;
	}

	@XmlElement
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
}
