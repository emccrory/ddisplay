package gov.fnal.ppd.signage.channel;

import java.io.Serializable;
import java.net.URI;

import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.ChannelCategory;

/**
 * A simple implementation of the Channel interface
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class ChannelImpl implements Channel {

	private static final long	serialVersionUID	= -5603360064745048878L;

	// TODO Make this realistic!
	protected int				number;

	private String				name, description;

	private ChannelCategory		category;

	// The content for this channel!
	private SignageContent		content				= null;

	private URI					uri;

	private SignageType			type				= SignageType.Public;

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
	 * @param category
	 *            The category of this Channel
	 * @param description
	 *            A description of this Channel
	 * @param uri
	 *            The URI that represents this Channel
	 * @param number
	 *            A number for this Channel
	 */
	public ChannelImpl(final String name, final ChannelCategory category, final String description, final URI uri, final int number) {
		this.name = name;
		this.category = category;
		this.number = number;
		if (description != null)
			this.description = description;
		else
			this.description = "This is the '" + name + "' channel";
		this.uri = uri;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public ChannelCategory getCategory() {
		return category;
	}

	public boolean equals(Object o) {
		if (o == null || !(o instanceof Channel))
			return false;
		Channel that = (Channel) o;
		if (this.content == null || that.getContent() == null)
			return that.getName().equals(this.name);
		return that.getContent().equals(this.content);
	}

	public String toString() {
		return number + ": " + name + " (" + category + ")";
	}

	public int getNumber() {
		return number;
	}

	@Override
	public Object getContent() {
		return this;
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

	@Override
	public void setContent(Serializable content) {
		this.content = (SignageContent) content;
	}

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

}
