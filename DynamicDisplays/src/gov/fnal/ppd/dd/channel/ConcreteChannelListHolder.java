/**
 * 
 */
package gov.fnal.ppd.dd.channel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation copyright 2018
 * 
 */
public class ConcreteChannelListHolder implements ChannelListHolder {
	List<ChannelInList>	channelList	= new ArrayList<ChannelInList>() {
										private static final long	serialVersionUID	= 5139683996976896075L;

										public boolean add(ChannelInList chan) {
											if (chan.getSequenceNumber() < 0) {
												int lastSeq = -1;
												for (ChannelInList CIL : this)
													if (CIL.getSequenceNumber() > lastSeq)
														lastSeq = CIL.getSequenceNumber();
												chan.setSequenceNumber(lastSeq + 1);
											}
											return super.add(chan);
										}
									};

	/**
	 * 
	 */
	public ConcreteChannelListHolder() {
		// nothing to do here
	}

	@Override
	public List<ChannelInList> getList() {
		return channelList;
	}

	@Override
	public void clear() {
		channelList.clear();
	}

	@Override
	public void channelAdd(ChannelInList c) {
		channelList.add(c);
	}

	@Override
	public void fix() {
		// Nothing to do here.
	}
}