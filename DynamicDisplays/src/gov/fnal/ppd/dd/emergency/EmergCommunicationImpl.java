package gov.fnal.ppd.dd.emergency;

import java.net.URI;
import java.net.URISyntaxException;

import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.EmergencyCommunication;
import gov.fnal.ppd.dd.signage.SignageType;

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
	private ChannelCategory		category			= ChannelCategory.MISCELLANEOUS;
	private SignageType			sType				= SignageType.XOC;
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
	public ChannelCategory getCategory() {
		return category;
	}

	@Override
	public void setCategory(ChannelCategory c) {
		// this.category = c; Cannot override!
	}

	@Override
	public SignageType getType() {
		return sType;
	}

	@Override
	public void setType(SignageType t) {
		// this.sType = t; Cannot override!
		alertReadOnly();
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
	public int getFrameNumber() {
		return 0;
	}

	@Override
	public void setFrameNumber(int frameNumber) {
		// Cannot be override!
		alertReadOnly();
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
}
