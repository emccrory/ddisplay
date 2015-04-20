/*
 * PlainURLChannel
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import gov.fnal.ppd.dd.changer.ChannelCategory;

import java.net.URL;

/**
 * Used in XML marshalling to create a simple, URL-based Channel
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class PlainURLChannel extends ChannelImpl {
	private static final long	serialVersionUID	= -7448049700623614405L;

	/**
	 * @param theURL
	 *            The URL that is this Channel
	 * @throws Exception
	 *             In the case that the URL passed here is not a valid URI
	 */
	public PlainURLChannel(final URL theURL) throws Exception {
		super(theURL.toString(), ChannelCategory.PUBLIC, "This is a URL that is '" + theURL + "'", theURL.toURI(), 0, 0);
	}

}
