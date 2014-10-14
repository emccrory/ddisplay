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
	 * The name of the content.
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
	 *            The (lengthy) description of this content
	 */
	public void setDescription(String d);

	/**
	 * The category this content should be place in when presented to the user in the channel selector.
	 * 
	 * @return The category of this Channel
	 */
	public ChannelCategory getCategory();

	/**
	 * @param c
	 */
	public void setCategory(ChannelCategory c);

	/**
	 * @return The type of this Content, e.g., PUBLIC, EXPERIMENT, and XOC. This determined where this content may be displayed.
	 */
	public SignageType getType();

	/**
	 * @param t
	 */
	public void setType(SignageType t);

	/**
	 * The address of the content. Usually, this will be a URL (likely, "http://" or "https://") and points to somewhere on the
	 * Internet.
	 * 
	 * @return The URI for this content
	 */
	public URI getURI();

	/**
	 * @param i
	 */
	public void setURI(URI i);

	/**
	 * @return The time associated with this content. Generally, this will be the dwell time (or the time to wait before refreshing)
	 */
	public long getTime();

	/**
	 * @param time
	 *            The time associated with this content.
	 */
	public void setTime(long time);
}
