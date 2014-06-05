package gov.fnal.ppd.signage;

import gov.fnal.ppd.signage.changer.ChannelCategory;

import java.io.Serializable;
import java.net.URI;

/**
 * A place holder for the Signage Content.
 * 
 * In order to be fully serializable, the concrete classes must implement all the getters and setters.
 * 
 * @author Elliott McCrory, Fermilab, 2012
 */
public interface SignageContent extends Serializable {

	/**
	 * The name of the Channel. This must be unique in the set of channels as this can be used as a database lookup
	 * 
	 * @return The name of this content
	 */
	public String getName();

	/**
	 * @param name
	 */
	public void setName(String name);

	/**
	 * The actual content (if applicable)
	 * 
	 * @return The content
	 */
	public Object getContent();

	/**
	 * @param content
	 */
	public void setContent(Serializable content);

	/**
	 * A nice description of the content. This should (IMHO) be lengthy.
	 * 
	 * @return The description
	 */
	public String getDescription();

	/**
	 * @param d
	 */
	public void setDescription(String d);

	/**
	 * The type of channel this is. This allows for the channels to be categorized, of course.
	 * 
	 * @return The category of this Channel
	 */
	public ChannelCategory getCategory();

	/**
	 * @param c
	 */
	public void setCategory(ChannelCategory c);

	/**
	 * @return The type of this Content
	 */
	public SignageType getType();

	/**
	 * @param t
	 */
	public void setType(SignageType t);

	/**
	 * The location of the content. Usually, this will be a URL (likely, "http://" or "https://") and point to somewhere on the
	 * Internet.
	 * 
	 * @return The URI for this content
	 */
	public URI getURI();

	/**
	 * @param i
	 */
	public void setURI(URI i);

	// Not actually needed at this time. May be requested someday
	// public int getNumber();

}
