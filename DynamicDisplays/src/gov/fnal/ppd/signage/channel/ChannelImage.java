package gov.fnal.ppd.signage.channel;

import gov.fnal.ppd.signage.changer.ChannelCategory;

import java.net.URI;

/**
 * Simple extension to ChannelImpl to add an experiment attribute.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class ChannelImage extends ChannelImpl {

	private static final long	serialVersionUID	= -3975506541172871671L;
	private String				exp;

	/**
	 * @param name
	 *            Name of the channel
	 * @param category
	 *            Channel category
	 * @param description
	 *            Long (-ish) description of the channel
	 * @param uri
	 *            The URL of the channel
	 * @param number
	 *            Channel number
	 * @param exp
	 *            The experiment that this image associates to.
	 */
	public ChannelImage(final String name, final ChannelCategory category, final String description, final URI uri,
			final int number, final String exp) {
		super(name, category, description, uri, number);
		this.exp = exp;
	}

	/**
	 * @return The experiment this image associates to.
	 */
	public String getExp() {
		return exp;
	}
}
