package gov.fnal.ppd.dd.emergency;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.signage.EmergencyCommunication;

/**
 * Reference implementation of EmergencyCommunication
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class EmergCommunicationImpl implements EmergencyCommunication {

	private static final long	serialVersionUID	= 4875847440423308800L;
	private EmergencyMessage	message;
	private String				name				= "Energency Communication";
	private String				desc				= "Emergency Communication message";
	private ChannelClassification		category			= ChannelClassification.MISCELLANEOUS;
	private long				dwell				= 3600000L;
	private URI					uri;
	private long				expire				= 0L;
	private int					code;
	private boolean				debug				= true;

	/**
	 * 
	 */
	public EmergCommunicationImpl() {
		try {
			uri = new URI("http://emergency.message");
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
		return desc;
	}

	@Override
	public void setDescription(String d) {
		this.desc = d;
	}

	@Override
	public ChannelClassification getChannelClassification() {
		return category;
	}

	@Override
	public void setChannelClassification(ChannelClassification c) {
		// this.category = c; Cannot override!
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public void setURI(URI i) {
		// this.uri = i; Cannot override!
		alertReadOnly();
	}

	@Override
	public long getTime() {
		return dwell;
	}

	@Override
	public void setTime(long time) {
		this.dwell = time;
	}

	@Override
	public long getExpiration() {
		return expire;
	}

	@Override
	public void setExpiration(long expire) {
		this.expire = expire;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public void setCode(int n) {
		this.code = n;
	}

	@Override
	public EmergencyMessage getMessage() {
		return message;
	}

	@Override
	public void setMessage(EmergencyMessage theMessage) {
		this.message = theMessage;
	}

	@Override
	public String toString() {
		return "Emergency Message: " + message.toString();
	}

	private void alertReadOnly() {
		// Inform the user (somehow) that someone has tried to write to this read-only object.
		if (debug)
			throw new RuntimeException("An attempt has been made to change the internals of a read-only "
					+ getClass().getSimpleName() + " object");
	}

	public long getChecksum() {
		Checksum checksum = new CRC32();

		byte bytes[] = toString().getBytes();
		// update the current checksum with the specified array of bytes
		checksum.update(bytes, 0, bytes.length);

		// get the current checksum value
		return checksum.getValue();
	}
}
