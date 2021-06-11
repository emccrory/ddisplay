/**
 * 
 */
package gov.fnal.ppd.dd.channel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A simple implementation of the interface.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, copyright 2018-21
 * 
 */
public class ConcreteChannelListHolder implements ChannelListHolder {
	private Date		lastModTime	= new Date();
	List<ChannelInList>	channelList	= new ArrayList<ChannelInList>() {
										private static final long serialVersionUID = 5139683996976896075L;

										public boolean add(ChannelInList chan) {
											if (chan.getSequenceNumber() < 0) {
												int lastSeq = -1;
												for (ChannelInList CIL : this)
													if (CIL.getSequenceNumber() > lastSeq)
														lastSeq = CIL.getSequenceNumber();
												chan.setSequenceNumber(lastSeq + 1);
											}
											lastModTime = new Date();
											return super.add(chan);
										}
									};
	private String name;

	/**
	 * @param lsc 
	 * @param contentName 
	 * 
	 */
	public ConcreteChannelListHolder() {
		name = "Content created at " + lastModTime;
	}

	public ConcreteChannelListHolder(String contentName, List<ChannelInList> cil) {
		this.name = contentName;
		for (ChannelInList C : cil)
			channelList.add(C);
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

	public String getName() {
		return name;
	}
}
