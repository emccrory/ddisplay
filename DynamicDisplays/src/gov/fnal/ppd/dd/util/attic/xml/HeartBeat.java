/*
 * HeartBeat
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.attic.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A simple heartbeat class for the Messaging Server to send out from time to time
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
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
