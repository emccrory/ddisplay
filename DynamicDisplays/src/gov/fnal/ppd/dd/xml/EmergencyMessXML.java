package gov.fnal.ppd.dd.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import gov.fnal.ppd.dd.emergency.Severity;

@XmlRootElement
public class EmergencyMessXML extends EncodedCarrier {

	private String		headline, message, footnote;
	private Severity	severity;

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

}
