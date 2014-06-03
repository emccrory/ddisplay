package gov.fnal.ppd.signage.channel;

import gov.fnal.ppd.signage.changer.ChannelCategory;

import java.net.URL;

public class PlainURLChannel extends ChannelImpl {
private static final long	serialVersionUID	= -7448049700623614405L;

	public PlainURLChannel(URL theURL) throws Exception {
		super(theURL.toString(), ChannelCategory.PUBLIC, "This is just a URL", theURL.toURI(), 0);
	}

}
