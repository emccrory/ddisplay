package gov.fnal.ppd.signage.xml;

import static gov.fnal.ppd.GlobalVariables.*;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.changer.ChannelCategory;
import gov.fnal.ppd.signage.channel.EmptyChannel;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Message from a Control Panel that specifies a Channel (SignageContent)
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012
 */
@XmlRootElement
public class ChannelSpec {
	protected SignageContent	content;
	private long				time	= 0;

	public ChannelSpec() {
		try {
			content = new EmptyChannel();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
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
			}
		}
		return content.getURI();
	}

	public void setUri(URI uri) {
		content.setURI(uri);
	}

	public void setContent(SignageContent c) {
		content = c;
	}

	@XmlElement
	public long getTime() {
		return time;
	}

	public void setTime(long n) {
		time = n;
	}
}
