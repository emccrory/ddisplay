package gov.fnal.ppd.dd.xml;

import gov.fnal.ppd.dd.chat.MessageType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.bind.annotation.XmlElement;

/**
 * Base class for all the XML classes.
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012-14
 */
public class EnhancedEncodedCarrier {

	protected String		dateString	= new Date().toString();
	protected String		ipAddress;
	protected String		to;
	protected String		from;
	protected MessageType	type;
	protected byte[]		signature;

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

	/**
	 * @return the IP Address string
	 */
	@XmlElement
	public String getIPAddress() {
		return ipAddress;
	}

	/**
	 * @param d
	 */
	public void setIPAddress(final String d) {
		ipAddress = d;
	}

	/**
	 * @return the to
	 */
	@XmlElement
	public String getTo() {
		return to;
	}

	/**
	 * @param to
	 *            the to to set
	 */
	public void setTo(String to) {
		this.to = to;
	}

	/**
	 * @return the from
	 */
	@XmlElement
	public String getFrom() {
		return from;
	}

	/**
	 * @param from
	 *            the from to set
	 */
	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * @return the type
	 */
	@XmlElement
	public MessageType getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(MessageType type) {
		this.type = type;
	}

	/**
	 * @return the signature
	 */
	@XmlElement
	public byte[] getSignature() {
		return signature;
	}

	/**
	 * @param signature the signature to set
	 */
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	/**
	 * @return -- the age of this message in milliseconds
	 */
	public long howOld() {
		try { // Thu Dec 18 15:54:43 CST 2014
			DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
			Date dd = format.parse(dateString);
			long then = dd.getTime();
			// System.out.println("----------> Date from message interpreted as '" + dd + "'");
			return System.currentTimeMillis() - then;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return 3600 * 1000L; // an hour
	}
}
