package gov.fnal.ppd.dd.channel;

import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class SkeletonChannel implements SignageContent {
	
	private static final long	serialVersionUID	= 6188374436280281827L;
	private String	name;
	private String	desc	= null;
	private URI		uri		= null;

	/**
	 * @param name
	 */
	public SkeletonChannel(final String name) {
		this.name = name;
	
		try {
			if (name.contains("http://")) {
				this.name = name.substring(0, name.indexOf("http://") - 1).replace(" ...", "");
				uri = new URI(name.substring(name.indexOf("http://"), name.length() - 1).replace(" ...", ""));
			} else if (name.contains("https://")) {
				this.name = name.substring(0, name.indexOf("https://") - 1).replace(" ...", "");
				uri = new URI(name.substring(name.indexOf("https://"), name.length() - 1).replace(" ...", ""));
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDescription() {
		if (desc != null)
			return desc;
		return "Skeleton of '" + name + "'";
	}

	@Override
	public void setDescription(String d) {
		desc = d;
	}

	@Override
	public ChannelCategory getCategory() {
		return ChannelCategory.MISCELLANEOUS;
	}

	@Override
	public void setCategory(ChannelCategory c) {
	}

	@Override
	public SignageType getType() {
		return SignageType.XOC;
	}

	@Override
	public void setType(SignageType t) {
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public void setURI(URI i) {
		uri = i;
	}

	@Override
	public long getTime() {
		return 0;
	}

	@Override
	public void setTime(long time) {
	}

	@Override
	public long getExpiration() {
		return 0;
	}

	@Override
	public void setExpiration(long expire) {
	}

	@Override
	public int getCode() {
		return 0;
	}

	@Override
	public void setCode(int n) {
	}

	@Override
	public int getFrameNumber() {
		return 0;
	}

	@Override
	public void setFrameNumber(int frameNumber) {
	}

}
