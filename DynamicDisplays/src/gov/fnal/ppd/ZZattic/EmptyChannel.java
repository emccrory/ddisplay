package gov.fnal.ppd.ZZattic;

import static gov.fnal.ppd.dd.util.Util.MY_NAME;
import static gov.fnal.ppd.dd.util.Util.MY_URL;
import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.channel.ChannelImpl;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An empty channel for use by these XML classes - This used to be the name of the class, but it was changed to XmlDerivedChannel.
 * This class has been reintroduced so that channels stored in the table DefaultChannels will still work.
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation.
 * 
 * @deprecated
 */
public class EmptyChannel extends ChannelImpl {
	private static final long serialVersionUID = -419375552528263824L;

	/**
	 * Create an Empty Channel
	 * 
	 * @throws URISyntaxException
	 *             (In the unlikely case that the default web page is no longer a valid URI)
	 */
	public EmptyChannel() throws URISyntaxException {
		this(MY_NAME, ChannelClassification.MISCELLANEOUS);

	}

	public EmptyChannel(String string, ChannelClassification categ) throws URISyntaxException {
		super(string, categ, "This channel is " + string, new URI(MY_URL), 0, 0);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		System.out.println(MY_URL);
	}
}