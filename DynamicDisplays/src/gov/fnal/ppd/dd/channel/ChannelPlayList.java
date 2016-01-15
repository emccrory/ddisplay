/*
 * ChannelPlayList
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * This class holds a list of Channels that are to be played on a Display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelPlayList implements Channel {

	// FIXME -- No, the playing of a list of channels has to be handled by the Real Display.

	private static final long		serialVersionUID	= 7785042152443699996L;
	private List<SignageContent>	channels			= new ArrayList<SignageContent>();
	private int						currentChannel		= 0;
	private long					dwell				= 5000l;
	// private boolean running = true;
	// private Thread playThread = null;
	// private Display myDisplay = null;
	private int						frameNumber			= 0;
	private long					expiration			= 0;
	private long					lastAdvanceTime		= 0;

	/**
	 * Create a channel list to play out on the Display.
	 * 
	 * @param list
	 *            - The list of channels to play (must be 2 or greater)
	 * @param millisecondDwellTime
	 *            - How long to wait on each channel (must be 2 seconds or longer)
	 */
	public ChannelPlayList(List<SignageContent> list, long millisecondDwellTime) {
		assert (list.size() > 1);
		assert (millisecondDwellTime > 2000);

		this.dwell = millisecondDwellTime;
		this.channels = list;
	}

	/**
	 * An empty constructor, as required by XML marshalling
	 */
	public ChannelPlayList() {
	}

	@Override
	public String getName() {
		return channels.get(currentChannel).getName() + " (List)";
	}

	// protected void informDisplay() {
	// if (myDisplay != null) {
	// myDisplay.setContent(currentChannel);
	// }
	// }

	// /**
	// * @param d
	// */
	// public void setDisplay(final Display d) {
	// myDisplay = d;
	// }

	@Override
	public String getDescription() {
		return "List of channels, now showing " + channels.get(currentChannel).getDescription();
	}

	@Override
	public ChannelCategory getCategory() {
		return channels.get(currentChannel).getCategory();
	}

	@Override
	public SignageType getType() {
		return channels.get(currentChannel).getType();
	}

	@Override
	public URI getURI() {
		return channels.get(currentChannel).getURI();
	}

	@Override
	public int getNumber() {
		if (channels.get(currentChannel) instanceof Channel)
			return ((Channel) channels.get(currentChannel)).getNumber();
		return 0;
	}

	/**
	 * Start playing this list of Channels
	 */
	// public void start() {
	// new Thread("PlayingChannelList") {
	// public void run() {
	// catchSleep(dwell);
	// while (running) {
	// if (chanIterator.hasNext()) {
	// currentChannel = chanIterator.next();
	// } else {
	// chanIterator = channels.listIterator();
	// currentChannel = chanIterator.next();
	// }
	// informDisplay();
	// catchSleep(dwell);
	// }
	// }
	// }.start();
	// }

	/**
	 * A polite request to stop playing the Channel list
	 */
	// public void stop() {
	// running = false;
	// }

	@Override
	public void setName(String name) {
		// Not relevant for a list of channels
	}

	@Override
	public void setDescription(String d) {
		// Not relevant for a list of content
	}

	@Override
	public void setCategory(ChannelCategory c) {
		// Not relevant for a list of content
	}

	@Override
	public void setType(SignageType t) {
		// Not relevant for a list of content
	}

	@Override
	public void setURI(URI i) {
		// Not relevant for a list of content
	}

	/**
	 * @return The list of Channels being used here
	 */
	public List<SignageContent> getChannels() {
		return channels;
	}

	/**
	 * @param channels
	 */
	public void setChannels(final List<SignageContent> channels) {
		this.channels = channels;
	}

	@Override
	public long getTime() {
		if (channels.get(currentChannel).getTime() <= 0)
			return dwell;
		return channels.get(currentChannel).getTime();
	}

	@Override
	public void setTime(long time) {
		dwell = time;
	}

	@Override
	public int getCode() {
		return channels.get(currentChannel).getCode();
	}

	@Override
	public void setCode(int n) {
		// Not relevant for a list of content
	}

	@Override
	public int getFrameNumber() {
		return frameNumber;
	}

	/**
	 * @param frameNumber
	 *            the frame number to give this content to.
	 */
	public void setFrameNumber(final int frameNumber) {
		this.frameNumber = frameNumber;
	}

	@Override
	public long getExpiration() {
		return expiration;
	}

	@Override
	public void setExpiration(long expire) {
		this.expiration = expire;
	}

	/**
	 * Point to the next channel in the list
	 */
	public void advanceChannel() {
		if (lastAdvanceTime + channels.get(currentChannel).getTime() < System.currentTimeMillis()) {
			// This channel is still valid, keep playing it.
			return;
		}
		lastAdvanceTime = System.currentTimeMillis();
		currentChannel++;
		if (currentChannel >= channels.size())
			currentChannel = 0;
	}

	@Override
	public String toString() {
		String retval = "List of length " + channels.size() + ": [" ;
		for (SignageContent SC: channels) 
			retval += "(" + SC.getName() + " @ " + SC.getTime() + "msec) ";
		return retval;
	}
}
