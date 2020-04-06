package gov.fnal.ppd.dd.xml;

import static gov.fnal.ppd.dd.util.PackageUtilities.MY_NAME;
import static gov.fnal.ppd.dd.util.PackageUtilities.MY_URL;

import java.net.URI;
import java.net.URISyntaxException;

import gov.fnal.ppd.dd.channel.ChannelImpl;

/**
 * An empty channel for use by these XML classes
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation.
 * 
 */
public class XmlDerivedChannel extends ChannelImpl {
	private static final long	serialVersionUID	= -419375552528263824L;

	/**
	 * Create an Empty Channel
	 * 
	 * @throws URISyntaxException
	 *             (In the unlikely case that the default web page is no longer a valid URI)
	 */
	public XmlDerivedChannel() throws URISyntaxException {
		this(MY_NAME);

	}

	public XmlDerivedChannel(String string) throws URISyntaxException {
		super(string, "This channel is " + string, new URI(MY_URL), 0, 0);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		System.out.println(MY_URL);
	}
}
