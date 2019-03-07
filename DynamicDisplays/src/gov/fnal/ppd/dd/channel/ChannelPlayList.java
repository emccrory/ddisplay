/*
 * ChannelPlayList
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

/**
 * This class holds a list of Channels that is to be played on a Display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelPlayList implements Channel {

	// FIXME -- No, the playing of a list of channels has to be handled by the Real Display.

	private static final long		serialVersionUID	= 7785042152443699996L;
	private String					description			= null;
	private List<SignageContent>	channels			= new ArrayList<SignageContent>();
	private int						currentChannel		= 0;
	private long					dwell				= 5000l;
	// private boolean running = true;
	// private Thread playThread = null;
	// private Display myDisplay = null;
	// private int frameNumber = 0;
	private long					expiration			= 0;
	// private long					lastAdvanceTime		= 0;

	/**
	 * Create a channel list to play out on the Display.
	 * 
	 * @param list
	 *            - The list of channels to play (must be 2 or greater)
	 * @param millisecondDwellTime
	 *            - How long to wait on each channel (must be 2 seconds or longer)
	 */
	public ChannelPlayList(List<? extends SignageContent> list, long millisecondDwellTime) {
		assert (list.size() > 1);
		assert (millisecondDwellTime > 2000);

		this.dwell = millisecondDwellTime;
		for (SignageContent L : list)
			this.channels.add(L);
	}

	/**
	 * An empty constructor, as required by XML marshalling
	 */
	public ChannelPlayList() {
	}

	@Override
	public String getName() {
		return channels.get(currentChannel).getName() + " (Length=" + channels.size() + ")";
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
		if (description != null)
			return description;
		return "List of channels, now showing " + channels.get(currentChannel).getDescription();
	}

	@Override
	public ChannelCategory getCategory() {
		return channels.get(currentChannel).getCategory();
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
		description = d;
	}

	@Override
	public void setCategory(ChannelCategory c) {
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
	 * @param list
	 */
	public void setChannels(final List<? extends SignageContent> list) {
		for (SignageContent L : list)
			this.channels.add(L);
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
		currentChannel++;
		if (currentChannel >= channels.size())
			currentChannel = 0;
	}

	@Override
	public String toString() {
		String retval = "List of length " + channels.size() + ": { ";
		// for (SignageContent SC: channels) {
		for (int k = 0; k < channels.size(); k++) {
			int index = (k + currentChannel) % channels.size();
			SignageContent SC = channels.get(index);
			retval += "( [" + index + "] " + SC.getName() + " @ " + SC.getTime()/1000.0 + " sec ) ";
		}
		return retval + " }";
	}

	public long getChecksum() {
		// I bet there is a more efficient way than this!

		// The string, ["http://1http://2"] WILL NOT return the same checksum as ["http://2http://1"] (verified)

		Checksum checksum = new CRC32();

		List<byte[]> allBytes = new ArrayList<byte[]>();
		int totalLength = 0;
		for (SignageContent C : channels) {
			byte b[] = C.getURI().toASCIIString().getBytes();
			totalLength += b.length;
			allBytes.add(b);
		}
		byte ab[] = new byte[totalLength];
		int index = 0;
		for (byte[] B : allBytes) {
			for (int k = 0; k < B.length; k++)
				ab[index + k] = B[k];
			index += B.length;
		}
		// update the current checksum with the specified array of bytes
		checksum.update(ab, 0, ab.length);

		// get the current checksum value
		return checksum.getValue();
	}
}
