package gov.fnal.ppd.dd.channel;

import gov.fnal.ppd.dd.signage.Channel;

import java.util.List;

/**
 * Mechanism for the JTable class to communicate with the GUI class.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 *
 */
public interface ChannelListHolder {
	/**
	 * @return the list of channels as we know it now.
	 */
	public List<Channel> getList();

	// theListOfAllChannels.channelList.clear();
	// theListOfAllChannels.labelList.clear();

	/**
	 * Reset/clear the GUI for the adding of new channels to the list
	 */
	public void clear();

	/**
	 * @param c
	 *            the channel to add
	 */
	public void channelAdd(Channel c);
	
	/**
	 * Not really used anymore, but was once used to fix up the internal list (in some way that I cannot recall)
	 */
	public void fix();
}
