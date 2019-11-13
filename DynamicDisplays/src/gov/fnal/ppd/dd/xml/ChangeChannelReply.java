/*
 * ChangeChannelReply
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.xml;

import javax.xml.bind.annotation.XmlElement;

/**
 * The reply message to a change-channel request.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class ChangeChannelReply extends MessagingDataXML {
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

	/**
	 * This directive will not change anything - it is status (only) information.
	 */
	@Override
	public boolean willNotChangeAnything() {
		return true;
	}
}
