package gov.fnal.ppd.dd.channel;

import java.net.URI;

import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

/**
 * Extend the ChannelImpl class to hold a sequence number.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2016-18
 * 
 * 
 */
public class ChannelInListImpl extends ChannelImpl implements ChannelInList {
	private static final long	serialVersionUID	= -8761023254052704106L;
	private int					sequenceNumber;

	/**
	 * @param c
	 */
	public ChannelInListImpl(final SignageContent c) {
		super(c);
		if (c instanceof ChannelInListImpl)
			setSequenceNumber(((ChannelInListImpl) c).getSequenceNumber());
		else
			setSequenceNumber(-1);
	}

	/**
	 * @param name
	 * @param category
	 * @param description
	 * @param uri
	 * @param number
	 * @param dwellTime
	 */
	public ChannelInListImpl(final String name, final ChannelClassification category, final String description, final URI uri,
			final int number, final long dwellTime) {
		super(name, category, description, uri, number, dwellTime);
		setSequenceNumber(-1);
	}

	/**
	 * @param c
	 * @param seqNum
	 * @param dwell
	 */
	public ChannelInListImpl(final Channel c, final int seqNum, final long dwell) {
		this(c);
		setSequenceNumber(seqNum);
		setTime(dwell);
	}

	/**
	 * @param c
	 * @param seqNum
	 */
	public ChannelInListImpl(final Channel c, final int seqNum) {
		this(c);
		setSequenceNumber(seqNum);
	}

	public String toString() {
		return sequenceNumber + ": Channel #" + getNumber() + " [ " + getName() + " ] (" + getTime() + " milliseconds)";
	}

	/**
	 * @return the sequenceNumber
	 */
	public int getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * @param sequenceNumber
	 *            the sequenceNumber to set - If negative, then this will be interpreted elsewhere as being added to the end of the
	 *            list.
	 */
	public void setSequenceNumber(final int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
}