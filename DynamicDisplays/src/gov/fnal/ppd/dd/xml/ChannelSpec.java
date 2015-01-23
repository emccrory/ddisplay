package gov.fnal.ppd.dd.xml;

import static gov.fnal.ppd.dd.GlobalVariables.*;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Message from a Control Panel that specifies a Channel (SignageContent)
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012
 */
@SuppressWarnings("javadoc")
@XmlRootElement
public class ChannelSpec {
	protected SignageContent	content;
	private long				time	= 0;
	private int					code	= 0;

	public ChannelSpec() {
		try {
			content = new EmptyChannel();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public ChannelSpec(SignageContent c) {
		content = c;
		time = c.getTime();
		code = c.getCode();
	}

	@XmlElement
	public ChannelCategory getCategory() {
		return content.getCategory();
	}

	public void setCategory(ChannelCategory c) {
		content.setCategory(c);
	}

	@XmlElement
	public String getName() {
		return content.getName();
	}

	public void setName(String name) {
		content.setName(name);
	}

	// Not actually needed at this time. May be requested someday
	// @XmlElement
	// public int getChannelNumber() {
	// return content.getNumber();
	// }

	@XmlElement
	public URI getUri() {
		if (content == null) {
			System.out.println("Oops -- got a bad URI");
			try {
				return new URI(SELF_IDENTIFY);
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
		} else
			return content.getURI();
	}

	public void setUri(URI uri) {
		content.setURI(uri);
	}

	public void setContent(final SignageContent c) {
		this.content = c;
		this.time = c.getTime();
		this.code = c.getCode();
	}

	@XmlElement
	public long getTime() {
		return time;
	}

	public void setTime(final long n) {
		time = n;
	}

	/**
	 * @return The user-defined "code"
	 */
	@XmlElement
	public int getCode() {
		return code;
	}

	/**
	 * Gives the channel an attribute that can be interpreted in a number of ways. Initially, this code is either 0 or 1; 0 means
	 * show the URL in the normal way; 1 means show the URL in a wrapper web page, which (presumably) has color and Display number
	 * information on it, somehow.
	 * 
	 * @param n
	 *            The new user-defined code
	 */
	public void setCode(final int n) {
		code = n;
	}
}
