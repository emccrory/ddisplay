package gov.fnal.ppd.dd.xml;

import javax.xml.bind.annotation.XmlElement;

import gov.fnal.ppd.dd.emergency.EmergencyMessage;
import gov.fnal.ppd.dd.emergency.Severity;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2017-18
 *
 */
public class EmergencyMessXML extends MessagingDataXML {

	private String		headline, message, footnote;
	private Severity	severity;
	private long		dwellTime	= 2 * 60 * 60 * 1000;	// two hours is the default
	private String		ipAddress;
	private long		timestamp;

	public EmergencyMessXML() {
		// Nothing to do yet.
	}
	
	public EmergencyMessXML(EmergencyMessage em) {
		headline = em.getHeadline();
		message = em.getMessage();
		footnote = em.getFootnote();
		severity = em.getSeverity();
		dwellTime = em.getDwellTime();
		timestamp = em.getTimestamp();
		ipAddress = em.getIpAddress();
	}

	/**
	 * @return the headline
	 */
	@XmlElement
	public String getHeadline() {
		return headline;
	}

	/**
	 * @param headline
	 *            the headline to set
	 */
	public void setIpAddress(String ip) {
		this.ipAddress = ip;
	}

	/**
	 * @return the headline
	 */
	@XmlElement
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * @param headline
	 *            the headline to set
	 */
	public void setHeadline(String headline) {
		this.headline = headline;
	}

	/**
	 * @return the message
	 */
	@XmlElement
	public String getMessage() {
		return message;
	}

	/**
	 * @param message
	 *            the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the footnote
	 */
	@XmlElement
	public String getFootnote() {
		return footnote;
	}

	/**
	 * @param footnote
	 *            the footnote to set
	 */
	public void setFootnote(String footnote) {
		this.footnote = footnote;
	}

	/**
	 * @return the severity
	 */
	@XmlElement
	public Severity getSeverity() {
		return severity;
	}

	/**
	 * @param severity
	 *            the severity to set
	 */
	public void setSeverity(Severity severity) {
		this.severity = severity;
	}

	/**
	 * 
	 * @return How long this emergency message is to be shown
	 */
	@XmlElement
	public long getDwellTime() {
		return dwellTime;
	}

	/**
	 * IMHO, it makes no sense to show an "emergency message" for days and days. Therefore, there has to be a limit to the time any
	 * message is shown.
	 * 
	 * @param dwellTime
	 *            How long do we need to show this emergency message?
	 */
	public void setDwellTime(long dwellTime) {
		this.dwellTime = dwellTime;
	}

	@XmlElement
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public boolean willNotChangeAnything() {
		return false;
	}

}
