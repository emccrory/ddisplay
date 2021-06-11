/*
 * ChannelImpl
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import java.util.zip.CRC32;
import java.util.zip.Checksum;
import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.net.URI;

/**
 * A simple implementation of the Channel interface
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelImpl implements Channel {

	private static final long		serialVersionUID	= -5603360064745048878L;

	protected int					number;

	private String					name, description;

	private ChannelClassification	category;

	// The content for this channel!
	// private SignageContent content = null;

	private URI						uri;

	private long					time;
	private long					expiration			= 0L;
	private int						code				= 0;

	/**
	 * This inner class is an experiment with using the Builder pattern to construct a channel instance. It is not used anywhere,
	 * but it us a cool idea that _should_ be used.
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	public static class Builder {
		// This is not used, but I discovered this nifty way to create an object and decided to place it here for posterity.

		// example:
		// ChannelImpl c = new ChannelImpl.builder("The name")
		// .withDescription ("The description of this channel")
		// .withURI (new URI("https://some.webpage.com/page.html"))
		// .dwells (200000L)
		// .withChannelNumber(42)
		// .build();

		// Note the lack of ambiguity as to the meaning of the arguments, and that the order of the arguments is irrelevant as
		// long as the name is in the constructor and it ends with .build()
		// (but the compiler will flag it most of the time if you get it wrong)

		private final String			name;
		private String					desc		= "";
		private URI						uri			= null;
		private int						num			= 0;
		private long					dwell		= 0;
		private ChannelClassification	category	= ChannelClassification.MISCELLANEOUS;

		public Builder(String name) {
			this.name = name;
		}

		public Builder withDescription(String desc) {
			this.desc = desc;
			return this;
		}

		public Builder withURI(final URI uri) {
			this.uri = uri;
			return this;
		}

		public Builder withChanNumber(int num) {
			this.num = num;
			return this;
		}

		public Builder dwells(long dwell) {
			if (dwell < 0)
				dwell = 0;
			this.dwell = dwell;
			return this;
		}

		public Builder classification(ChannelClassification category) {
			this.category = category;
			return this;
		}

		public ChannelImpl build() {
			if (num <= 0)
				throw new RuntimeException("The channel number must be positive-definite");
			if (uri == null)
				throw new RuntimeException("The URL for a channel cannot be null");
			return new ChannelImpl(name, category, desc, uri, num, dwell);
		}
	}

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
		this(name, ChannelClassification.MISCELLANEOUS, description, uri, number, dwellTime);
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
	public ChannelImpl(final String name, ChannelClassification category, final String description, final URI uri, final int number,
			final long dwellTime) {
		assert (name != null);

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
		this.category = c.getChannelClassification();
		this.time = c.getTime();
		this.description = c.getDescription();
		this.uri = c.getURI();
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
	 * @see gov.fnal.ppd.dd.signage.SignageContent#getChannelClassification()
	 */
	public ChannelClassification getChannelClassification() {
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
		// result = prime * result + frameNumber;
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
		// if (frameNumber != other.frameNumber)
		// return false;
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
	
	public void setNumber(int n) {
		number = n;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setDescription(String d) {
		this.description = d;
	}

	@Override
	public void setChannelClassification(ChannelClassification c) {
		this.category = c;
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
