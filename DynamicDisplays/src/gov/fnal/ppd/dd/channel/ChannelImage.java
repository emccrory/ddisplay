/*
 * ChannelImage
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_BILLION;
import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.net.URI;

/**
 * Simple extension to ChannelImpl to add an image.  This is depicted as a tiny, low-res thumbnail and a image title.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
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
	public ChannelImage(final String name, final String description, final URI uri, final int number, final String exp) {
		super(name, ChannelClassification.IMAGE, description, uri, number, -1);
		this.exp = exp;
	}

	/**
	 * @param c
	 */
	public ChannelImage(final SignageContent c) {
		super(c);
		if (c instanceof ChannelImage)
			this.exp = ((ChannelImage) c).exp;
	}

	/**
	 * @return The experiment this image associates to.
	 */
	public String getExp() {
		return exp;
	}

	/**
	 * 
	 * @param i
	 *            the Portfolio image number of this image
	 */
	public void setNumber(int i) {
		this.number = ONE_BILLION + i;

	}
}
