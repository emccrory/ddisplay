package gov.fnal.ppd.dd.signage;

import gov.fnal.ppd.dd.changer.ChannelClassification;

import java.io.Serializable;
import java.net.URI;

/**
 * <p>
 * The signature for content in the Fermilab Dynamic Displays system.
 * </p>
 * <p>
 * In order to be fully serializable, the concrete classes must implement all the getters and setters.
 * </p>
 * <p>
 * Originally, it was thought that there could be all kinds of different content, like in a commercial digital signage system. But
 * we have settled on all content being a Channel, and all channels are web pages. So this interface is not any more useful that the
 * Channel interface. Nevertheless, it is used in a few places where we want to be the most general. If the system were to be
 * extended to allow for more general sort of content, this interface _might_ make that extension a little easier.
 * </p>
 * 
 * @author Elliott McCrory, Fermilab, 2012-21
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
	 * The category this content should be put in when presented to the user in the channel selector.
	 * 
	 * @return The category of this Channel
	 */
	public ChannelClassification getChannelClassification();

	/**
	 * @param c
	 */
	public void setChannelClassification(ChannelClassification c);

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

	/**
	 * @return The expiration time in milliseconds. Negative or zero means no expiration time. This is interpreted as the length of
	 *         time that this content will be shown, after which the previous content is to be reinstated.
	 */
	public long getExpiration();

	/**
	 * @param expire
	 *            The new expiration time, in milliseconds.
	 */
	public void setExpiration(long expire);

	/**
	 * @return the special code that may be applicable for the content
	 */
	public int getCode();

	/**
	 * Gives the Content an attribute that can be interpreted in a number of ways. At this time, it is not used.
	 * 
	 * @param n
	 *            The new user-defined code
	 */
	public void setCode(final int n);

	/**
	 * Compute and return the checksum (CRC32 assumed) corresponding to the key element of this object. For a URL-based channel, it
	 * is assumed this will be the checksum of the URL itself.
	 * 
	 * @return The checksum
	 */
	public long getChecksum();
}
