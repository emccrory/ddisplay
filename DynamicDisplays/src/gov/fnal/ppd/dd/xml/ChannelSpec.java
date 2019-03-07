package gov.fnal.ppd.dd.xml;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.SignageContent;

/**
 * Message from a Control Panel that specifies a Channel (SignageContent)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@SuppressWarnings("javadoc")
@XmlRootElement
public class ChannelSpec implements SignageContent {
	private static final long	serialVersionUID	= -3784860691526919774L;
	protected SignageContent	content;

	public ChannelSpec() {
		try {
			content = new EmptyChannel();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public ChannelSpec(SignageContent c) {
		if (c == null) {
			try {
				content = new EmptyChannel();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		} else
			content = c;
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

	@XmlTransient
	public SignageContent getContent() {
		return content;
	}

	public void setContent(final SignageContent c) {
		this.content = c;
	}

	@XmlElement
	public long getTime() {
		return content.getTime();
	}

	public void setTime(final long n) {
		content.setTime(n);
	}

	@XmlElement
	public long getExpiration() {
		return content.getExpiration();
	}

	public void setExpiration(final long n) {
		content.setExpiration(n);
	}

	/**
	 * @return The user-defined "code"
	 */
	@XmlElement
	public int getCode() {
		return content.getCode();
	}

	/**
	 * This value was thought to be useful someday. But it has not proven to be useful yet.
	 * 
	 * @param n
	 *            The new user-defined code
	 */
	public void setCode(final int n) {
		content.setCode(n);
	}

	/**
	 * Used in the "change channel by number" scheme, to verify that the URL seen by the receiver is the same as the one assumed by
	 * the transmitter. This is somewhat redundant to the overall digital signature of a transmitted object.
	 */
	@XmlElement
	public long getChecksum() {
		return content.getChecksum();
	}

	public void setChecksum(long checksum) {
		// This is irrelevant - the checksum is always calculated from the URIs
		// this.content.setChecksum(checksum);
	}

	// ----------
	// The rest of these methods are implementations of the interface SignageContent.
	// ----------
	@Override
	public String getDescription() {
		return content.getDescription();
	}

	@Override
	public void setDescription(String d) {
		content.setDescription(d);
	}

	@XmlElement
	@Override
	public URI getURI() {
		return content.getURI();
	}

	@Override
	public void setURI(URI i) {
		content.setURI(i);
	}
}
