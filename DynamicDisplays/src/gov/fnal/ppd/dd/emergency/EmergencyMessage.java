package gov.fnal.ppd.dd.emergency;

import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;

import java.io.Serializable;
import java.util.Date;

/**
 * The class to hold all the information that defines an emergency message in the Dynamic Displays system.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class EmergencyMessage implements Serializable {

	private static final long	serialVersionUID	= 7099415687513564803L;

	private String				headline			= "", message = "", footnote = "";
	private Severity			severity			= Severity.TESTING;
	private long				dwellTime			= 2 * 60 * 60 * 1000;
	private String				ipAddress;
	private long				timestamp;

	/**
	 * Construct a TEST message with a timestamp of now.
	 */
	public EmergencyMessage() {
		setSeverity(Severity.TESTING);
		timestamp = System.currentTimeMillis();
	}

	/**
	 * @return the sub-text of this message
	 */
	public String getFootnote() {
		return footnote;
	}

	/**
	 * Set the sub-text for this message
	 * 
	 * @param footnote
	 *            the sub-text of this message
	 */
	public void setFootnote(final String footnote) {
		this.footnote = footnote.replaceAll("(\\r|\\n)", " ");
		;
	}

	/**
	 * @return the headline of this message
	 */
	public String getHeadline() {
		return headline;
	}

	/**
	 * Set the prominent headline that appears in this message
	 * 
	 * @param headline
	 *            the headline of this message
	 */
	public void setHeadline(final String headline) {
		this.headline = headline.replaceAll("(\\r|\\n)", " ");
	}

	/**
	 * @return the message string
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the detailed message that pertains to this emergency message
	 * 
	 * @param message
	 *            the message string
	 */
	public void setMessage(final String message) {
		this.message = message.replaceAll("(\\r|\\n)", " ");
	}

	/**
	 * @return the severity code of this message
	 */
	public Severity getSeverity() {
		return severity;
	}

	/**
	 * Set the Severity of this message.
	 * 
	 * @param severity
	 *            the Severity code of this message
	 */
	public void setSeverity(final Severity severity) {
		this.severity = severity;
	}

	@Override
	public String toString() {
		return "EmergencyMessage [headline='" + headline + "', message='" + message + "', footnote='" + footnote + "', severity="
				+ severity + ", dwell=" + dwellTime + "]";
	}

	/**
	 * @return an HTML string that represents this emergency message. This HTML should be what the display uses to show this
	 *         message.
	 */
	public String toHTML() {
		String retval = "";

		retval += "<img src='" + getFullURLPrefix() + "/emergency/fermilabLogo.jpg' class='logo'>";
		String headlineClass = ("" + severity).toLowerCase();
		retval += "<h1 class='" + headlineClass + "'>" + headline.replace("\'", "\"") + "</h1>";
		retval += "<p class='message'>" + message.replace("\'", "\"") + "</p>";

		if (footnote != null && footnote.length() > 0)
			retval += "<p class='footnote'>" + footnote.replace("\'", "\"") + "</p>";

		retval += "<p class='footnote'>This message came from " + ipAddress + " at " + new Date(timestamp) + " </p>";

		return retval;
	}

	/**
	 * 
	 * @return the dwell time
	 */
	public long getDwellTime() {
		return dwellTime;
	}

	/**
	 * <p>
	 * How long to keep the message visible.
	 * </p>
	 * <p>
	 * IMHO, it makes no sense to show an "emergency message" for days and days. Therefore, there has to be a limit to the time any
	 * message is shown.
	 * </p>
	 * 
	 * @param dwellTime
	 *            the amount of time this message should be seen
	 */
	public void setDwellTime(final long dwellTime) {
		this.dwellTime = dwellTime;
	}

	/**
	 * 
	 * @return The IP Address of the source. I expect it will be, for example, "machinename.fnal.gov - 131.225.1.234"
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * Who is responsible for this message?
	 * 
	 * @param ipAddress
	 *            - The IP Address of the source. I expect it will be, for example, "machinename.fnal.gov - 131.225.1.234"
	 */
	public void setIpAddress(String ipAddress) {
		if (ipAddress == null) {
			this.ipAddress = "Unknown";
		} else {
			this.ipAddress = ipAddress;
		}
	}

	/**
	 * The time this message was created
	 * @return
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * 
	 * @param t - the timestamp for when this message was created.
	 */
	public void setTimestamp(long t) {
		timestamp = t;
	}
}
