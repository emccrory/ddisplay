package gov.fnal.ppd.dd.channel;

import java.net.URI;

import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

/**
 * Extend the Channel class to hold a sequence number.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2016-18
 * 
 * 
 */
public class ChannelInList extends ChannelImpl {
	private static final long	serialVersionUID	= -8761023254052704106L;
	private int					sequenceNumber;

	/**
	 * @param c
	 */
	public ChannelInList(final SignageContent c) {
		super(c);
		if (c instanceof ChannelInList)
			setSequenceNumber(((ChannelInList) c).getSequenceNumber());
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
	public ChannelInList(final String name, final ChannelCategory category, final String description, final URI uri,
			final int number, final long dwellTime) {
		super(name, category, description, uri, number, dwellTime);
		setSequenceNumber(-1);
	}

	/**
	 * @param c
	 * @param seqNum
	 * @param dwell
	 */
	public ChannelInList(final Channel c, final int seqNum, final long dwell) {
		this(c);
		setSequenceNumber(seqNum);
		setTime(dwell);
	}

	/**
	 * @param c
	 * @param seqNum
	 */
	public ChannelInList(final Channel c, final int seqNum) {
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