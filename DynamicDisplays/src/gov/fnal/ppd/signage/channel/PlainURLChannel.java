package gov.fnal.ppd.signage.channel;

import gov.fnal.ppd.signage.changer.ChannelCategory;

import java.net.URL;

/**
 * Used in XML marshalling to create a simple, URL-based Channel
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
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
		super(theURL.toString(), ChannelCategory.PUBLIC, "This is just a URL", theURL.toURI(), 0);
	}

}