package gov.fnal.ppd.dd.channel;

import java.util.List;

/**
 * Anything that is a list of channels implements this interface.
 * 
 * This is not a channel, but it is the container that holds a list of channels. The classes ChannelPlayList and ChannelListGUI have
 * objects that implement this interface as an attribute.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public interface ChannelListHolder {
	/**
	 * @return the list of channels as we know it now.
	 */
	public List<ChannelInList> getList();

	/**
	 * Reset/clear the GUI for the adding of new channels to the list
	 */
	public void clear();

	/**
	 * @param c
	 *            the channel to add
	 */
	public void channelAdd(ChannelInList c);

	/**
	 * Fix up the internal list after the list has been constructed - useful in the GUI
	 */
	public void fix();

	/**
	 * Get a name for this list
	 */
	public String getName();
}
