package gov.fnal.ppd.dd.channel;

import gov.fnal.ppd.dd.signage.Channel;

/**
 * Extend the Channel interface to hold a sequence number.  This will be used when creating lists of channels.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public interface ChannelInList extends Channel {

	/**
	 * @return the sequenceNumber
	 */
	public int getSequenceNumber();

	/**
	 * @param sequenceNumber
	 *            the sequenceNumber to set - If negative, then this will be interpreted elsewhere as being added to the end of the
	 *            list.
	 */
	public void setSequenceNumber(final int sequenceNumber);
}