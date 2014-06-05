package gov.fnal.ppd.signage.channel;

import gov.fnal.ppd.signage.changer.ChannelCategory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An empty channel that can be held by a display during startup (to avoid null pointer violation elsewhere)
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation.
 * 
 */
public class EmptyChannel extends ChannelImpl {
	private static final long	serialVersionUID	= -419375552528263824L;

	/**
	 * Create an Empty Channel
	 * 
	 * @throws URISyntaxException
	 *             (In the unlikely case that the main XOC web page is no longer a valid URI)
	 */
	public EmptyChannel() throws URISyntaxException {
		super("EmptyChannel", ChannelCategory.PUBLIC, "This channel is undefined", new URI("http://xoc.fnal.gov"), 0);

	}

}
