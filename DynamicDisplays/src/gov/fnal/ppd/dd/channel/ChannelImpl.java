/*
 * ChannelImpl
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import java.util.zip.CRC32;
import java.util.zip.Checksum;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;

import java.net.URI;

/**
 * A simple implementation of the Channel interface
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelImpl implements Channel {

	private static final long	serialVersionUID	= -5603360064745048878L;

	protected int				number;

	private String				name, description;

	private ChannelCategory		category;

	// The content for this channel!
	// private SignageContent content = null;

	private URI					uri;

	private SignageType			type				= SignageType.Public;

	private long				time;
	private long				expiration			= 0L;
	private int					code				= 0;
	private int					frameNumber			= 0;

	// public ChannelImpl( String name, ChannelCategory category, String description ) {
	// this(name, category, description, Count++);
	// }
	//
	// public ChannelImpl( String name, ChannelCategory category, String description, int number ) {
	// this.name = name;
	// this.category = category;
	// this.number = number;
	// if (description != null)
	// this.description = description;
	// else
	// this.description = "This is the '" + name + "' channel";
	// }

	/**
	 * @param name
	 *            The name of this Channel
	 * 
	 * @param description
	 *            A description of this Channel
	 * @param uri
	 *            The URI that represents this Channel
	 * @param number
	 *            A number for this Channel
	 * @param dwellTime
	 *            The amount of time to wait before asking to refresh the content. 0 = use default (currently 2 hours); negative =
	 *            never refresh.
	 */
	public ChannelImpl(final String name, final String description, final URI uri, final int number, final long dwellTime) {
		this(name, new ChannelCategory("PUBLIC", "PUBLIC"), description, uri, number, dwellTime);
	}

	/**
	 * @param name
	 *            The name of this Channel
	 * @param category
	 *            A classifier for this channel
	 * @param description
	 *            A description of this Channel
	 * @param uri
	 *            The URI that represents this Channel
	 * @param number
	 *            A number for this Channel
	 * @param dwellTime
	 *            The amount of time to wait before asking to refresh the content. 0 = use default (currently 2 hours); negative =
	 *            never refresh.
	 */
	public ChannelImpl(final String name, ChannelCategory category, final String description, final URI uri, final int number,
			final long dwellTime) {
		assert (name != null);
		assert (number > 0);

		this.name = name;
		this.number = number;
		this.category = category;
		if (description != null)
			this.description = description;
		else
			this.description = "This is the '" + name + "' channel";
		this.uri = uri;
		this.time = dwellTime;
	}

	/**
	 * @param c
	 *            the SignageContent to clone
	 */
	public ChannelImpl(final SignageContent c) {
		this.name = c.getName();
		this.category = c.getCategory();
		this.time = c.getTime();
		this.description = c.getDescription();
		this.uri = c.getURI();
		this.frameNumber = c.getFrameNumber();
		this.time = c.getTime();
		this.code = c.getCode();

		if (c instanceof Channel) {
			this.number = ((Channel) c).getNumber();
		}
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * @deprecated (non-Javadoc)
	 * @see gov.fnal.ppd.dd.signage.SignageContent#getCategory()
	 */
	public ChannelCategory getCategory() {
		return category;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + frameNumber;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + number;
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ChannelImpl))
			return false;
		ChannelImpl other = (ChannelImpl) obj;
		if (frameNumber != other.frameNumber)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (number != other.number)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	public String toString() {
		return getName();
	}

	public int getNumber() {
		return number;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public SignageType getType() {
		return type;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	// @Override
	// public void setContent(Serializable content) {
	// this.content = (SignageContent) content;
	// }

	@Override
	public void setDescription(String d) {
		this.description = d;
	}

	@Override
	public void setCategory(ChannelCategory c) {
		this.category = c;
	}

	@Override
	public void setType(SignageType t) {
		this.type = t;
	}

	@Override
	public void setURI(URI i) {
		this.uri = i;
	}

	@Override
	public long getTime() {
		return time;
	}

	@Override
	public void setTime(long time) {
		this.time = time;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public void setCode(int n) {
		this.code = n;
	}

	@Override
	public int getFrameNumber() {
		return frameNumber;
	}

	/**
	 * @param f
	 *            the frame number to set this content to.
	 */
	public void setFrameNumber(final int f) {
		frameNumber = f;
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
	 * @return the CRC32 checksum of the URL for this channel
	 */
	public long getChecksum() {
		Checksum checksum = new CRC32();

		byte bytes[] = uri.toASCIIString().getBytes();
		// update the current checksum with the specified array of bytes
		checksum.update(bytes, 0, bytes.length);

		// get the current checksum value
		return checksum.getValue();
	}

}
