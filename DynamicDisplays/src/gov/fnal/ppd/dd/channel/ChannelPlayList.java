/*
 * ChannelPlayList
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

/**
 * This class holds a list of Channels that is to be played on a Display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelPlayList implements Channel {

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
	private String					name				= null;
	// private long lastAdvanceTime = 0;
	private int						listNumber			= 0;

	/**
	 * Create a channel list to play out on the Display.
	 * 
	 * @param list
	 *            - The list of channels to play (must be 2 or greater)
	 * @param millisecondDwellTime
	 *            - How long to wait on each channel (must be 2 seconds or longer)
	 */
	public ChannelPlayList(ChannelListHolder listHolder, long millisecondDwellTime) {
		assert (listHolder.getList().size() > 1);
		assert (millisecondDwellTime > 2000);

		this.dwell = millisecondDwellTime;
		for (SignageContent L : listHolder.getList())
			this.channels.add(L);
		this.name = listHolder.getName() + " (L=" + this.channels.size() + ")";
	}

	/**
	 * An empty constructor, as required by XML marshalling
	 */
	public ChannelPlayList() {
		name = "A list " + new Date();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		if (description != null)
			return description;
		return "List of channels, now showing " + channels.get(currentChannel).getDescription();
	}

	@Override
	public ChannelClassification getChannelClassification() {
		return channels.get(currentChannel).getChannelClassification();
	}

	@Override
	public URI getURI() {
		return channels.get(currentChannel).getURI();
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setDescription(String d) {
		description = d;
	}

	@Override
	public void setChannelClassification(ChannelClassification c) {
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
			retval += "( [" + index + "] " + SC.getName() + " @ " + SC.getTime() / 1000.0 + " sec ) ";
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (dwell ^ (dwell >>> 32));
		result = prime * result + (int) (expiration ^ (expiration >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((channels == null) ? 0 : channels.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChannelPlayList other = (ChannelPlayList) obj;

		if (dwell != other.dwell)
			return false;
		if (expiration != other.expiration)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;

		if (channels == null) {
			if (other.channels != null)
				return false;
		} else {
			if (channels.size() != other.channels.size())
				return false;
			for (int i = 0; i < channels.size(); i++)
				if (!channels.get(i).equals(other.channels.get(i)))
					return false;
		}
		return true;
	}

	public void setChannelNumber(int listNumber) {
		this.listNumber = listNumber;
	}

	@Override
	public int getNumber() {
		if (listNumber != 0)
			return listNumber;

		if (channels.get(currentChannel) instanceof Channel)
			return ((Channel) channels.get(currentChannel)).getNumber();
		return 0;
	}
}
