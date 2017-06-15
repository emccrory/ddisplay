package gov.fnal.ppd.dd.channel;

import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 * @deprecated
 */
public class SkeletonChannel implements Channel {

	private static final long	serialVersionUID	= 6188374436280281827L;
	private String				name;
	private String				desc				= null;
	private URI					uri					= null;

	@SuppressWarnings("unused")
	private SkeletonChannel() {
		// Keep this here in case something goes wrong
		new Exception().printStackTrace();
	}

	/**
	 * @param name
	 */
	public SkeletonChannel(final String name) {
		new Exception().printStackTrace();
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
			if (!e.getLocalizedMessage().contains("Illegal character in path"))
				e.printStackTrace();
			// When the URL is too long (e.g., within a list oc channels), it ends with " ...", which causes this exception.
			// Ignore it
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

	@Override
	public int getNumber() {
		return -1;
	}

	public long getChecksum() {
		Checksum checksum = new CRC32();

		byte bytes[] = uri.toASCIIString().getBytes();
		// update the current checksum with the specified array of bytes
		checksum.update(bytes, 0, bytes.length);

		// get the current checksum value
		return checksum.getValue();
	}
}
