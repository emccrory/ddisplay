package gov.fnal.ppd.signage.xml;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;

/**
 * Base class for all the XML classes. Includes encoding of the ID of this sender, and an ability to
 * affect the encoding (future enhancement).
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012-14
 */
public class EncodedCarrier {

	protected static final EncryptionService service = EncryptionService.getInstance();
	protected String encodedId;
	protected long key;
	protected String			dateString	= new Date().toString();

	/**
	 * @param id
	 */
	public void setEncodedId( final long id ) {
		// TODO Use the encoding value to encrypt this ID. this is not right yet. I need to find a
		// package that uses a KEY to encode the ID. (This kinda works.)
		try {
			this.encodedId = service.encrypt("" + (id + key));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param receivedEncoding
	 * @param actualID
	 * @return Is this ID valid?
	 */
	public boolean checkID( final String receivedEncoding, final long actualID ) {
		try {
			String encodedGuess = service.encrypt("" + (actualID + key));
			return encodedGuess.equals(encodedId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * @return the encoded ID
	 */
	@XmlElement
	public String getEncodedId() {
		return encodedId;
	}

	/**
	 * @param key
	 */
	public void setKey( final long key ) {
		// Not transmitted via XML!
		this.key = key;
	}

	/**
	 * @return the date string
	 */
	@XmlElement
	public String getDate() {
		return dateString;
	}

	/**
	 * @param d
	 */
	public void setDate(final String d) {
		dateString = d;
	}
}
