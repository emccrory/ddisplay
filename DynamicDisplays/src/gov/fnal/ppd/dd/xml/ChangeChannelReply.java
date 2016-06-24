/*
 * ChangeChannelReply
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.xml;

import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.EncodedCarrier;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The reply message to a change-channel request.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
@XmlRootElement
public class ChangeChannelReply extends EncodedCarrier {
	private ChannelSpec	showing;
	private int			displayNum;

	/**
	 * @return The currently showing channel spec
	 */
	@XmlElement
	public ChannelSpec getChannelSpec() {
		return showing;
	}

	/**
	 * @param spec
	 */
	public void setChannelSpec(final ChannelSpec spec) {
		showing = spec;
	}

	/**
	 * @return the display number
	 */
	public int getDisplayNum() {
		return displayNum;
	}

	/**
	 * @param displayNum
	 */
	@XmlElement
	public void setDisplayNum(final int displayNum) {
		this.displayNum = displayNum;
	}

}
