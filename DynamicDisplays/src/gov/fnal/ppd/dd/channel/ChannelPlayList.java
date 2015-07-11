/*
 * ChannelPlayList
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * This class holds a list of Channels that are to be played on a Display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelPlayList implements Channel {

	// FIXME -- No, the playing of a list of channels has to be handled by the Real Display.

	private static final long				serialVersionUID	= 7785042152443699996L;
	private List<SignageContent>			channels			= new ArrayList<SignageContent>();
	private ListIterator<SignageContent>	chanIterator;
	private SignageContent					currentChannel;
	private long							dwell				= 5000l;
	private boolean							running				= true;
	// private Thread playThread = null;
	private Display							myDisplay			= null;
	private int								frameNumber			= 0;
	private long							expiration			= 0;

	/**
	 * Create a channel list to play out on the Display.
	 * 
	 * @param list
	 *            - The list of channels to play (must be 2 or greater)
	 * @param millisecondDwellTime
	 *            - How long to wait on each channel (must be 5 seconds or longer)
	 */
	public ChannelPlayList(List<SignageContent> list, long millisecondDwellTime) {
		assert (list.size() > 1);
		assert (millisecondDwellTime > 5000);

		this.dwell = millisecondDwellTime;
		this.channels = list;
		chanIterator = channels.listIterator();
		currentChannel = chanIterator.next(); // Get to the first channel
	}

	/**
	 * An empty constructor, as required by XML marshalling
	 */
	public ChannelPlayList() {
	}

	@Override
	public String getName() {
		return currentChannel.getName() + " (List)";
	}

	protected void informDisplay() {
		if (myDisplay != null) {
			myDisplay.setContent(currentChannel);
		}
	}

	/**
	 * @param d
	 */
	public void setDisplay(final Display d) {
		myDisplay = d;
	}

	@Override
	public String getDescription() {
		return "List of channels, now showing " + currentChannel.getDescription();
	}

	@Override
	public ChannelCategory getCategory() {
		return currentChannel.getCategory();
	}

	@Override
	public SignageType getType() {
		return currentChannel.getType();
	}

	@Override
	public URI getURI() {
		return currentChannel.getURI();
	}

	@Override
	public int getNumber() {
		if (currentChannel instanceof Channel)
			return ((Channel) currentChannel).getNumber();
		return 0;
	}

	/**
	 * Start playing this list of Channels
	 */
	public void start() {
		new Thread("PlayingChannelList") {
			public void run() {
				catchSleep(dwell);
				while (running) {
					if (chanIterator.hasNext()) {
						currentChannel = chanIterator.next();
					} else {
						chanIterator = channels.listIterator();
						currentChannel = chanIterator.next();
					}
					informDisplay();
					catchSleep(dwell);
				}
			}
		}.start();
	}

	/**
	 * A polite request to stop playing the Channel list
	 */
	public void stop() {
		running = false;
	}

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
		chanIterator = this.channels.listIterator();
	}

	@Override
	public long getTime() {
		if (currentChannel.getTime() <= 0)
			return dwell;
		return currentChannel.getTime();
	}

	@Override
	public void setTime(long time) {
		dwell = time;
	}

	@Override
	public int getCode() {
		return currentChannel.getCode();
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
}
