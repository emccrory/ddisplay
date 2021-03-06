package gov.fnal.ppd.dd.xml;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.signage.Channel;
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
	private int					number				= 0;

	public ChannelSpec() {
		try {
			content = new XmlDerivedChannel();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public ChannelSpec(SignageContent c) {
		if (c == null) {
			try {
				content = new XmlDerivedChannel();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		} else
			content = c;
	}

	@XmlElement
	public ChannelClassification getChannelClassification() {
		return content.getChannelClassification();
	}

	public void setChannelClassification(ChannelClassification c) {
		content.setChannelClassification(c);
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

	@XmlElement
	public int getNumber() {
		if (number == 0 && content instanceof Channel)
			return ((Channel) content).getNumber();
		return number;
	}

	public void setNumber(int number) {
		if ( content instanceof ChannelImpl ) {
			((ChannelImpl) content).setNumber(number);
		}
		this.number = number;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
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
		ChannelSpec other = (ChannelSpec) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		return true;
	}

	public String toString() {
		return content.getURI().toString();
	}

}
