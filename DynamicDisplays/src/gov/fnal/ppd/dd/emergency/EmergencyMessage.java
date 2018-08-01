package gov.fnal.ppd.dd.emergency;

import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;

import java.io.Serializable;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class EmergencyMessage implements Serializable {

	private static final long	serialVersionUID	= 7099415687513564803L;

	private String				headline			= "", message = "", footnote = "";
	private Severity			severity			= Severity.TESTING;
	private long				dwellTime			= 2 * 60 * 60 * 1000;

	/**
	 * 
	 */
	public EmergencyMessage() {
		setSeverity(Severity.TESTING);
	}

	/**
	 * @return the sub-text of this message
	 */
	public String getFootnote() {
		return footnote;
	}

	/**
	 * @param footnote
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
	 * @param headline
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
	 * @param message
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
	 * @param severity
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
	 * @return an HTML string that represents this emergency message
	 */
	public String toHTML() {
		String retval = "";

		retval += "<img src='" + getFullURLPrefix() + "/emergency/fermilabLogo.jpg' class='logo'>";
		String headlineClass = ("" + severity).toLowerCase();
		retval += "<h1 class='" + headlineClass + "'>" + headline.replace("\'", "\"") + "</h1>";
		retval += "<p class='message'>" + message.replace("\'", "\"") + "</p>";

		if (footnote != null && footnote.length() > 0)
			retval += "<p class='footnote'>" + footnote.replace("\'", "\"") + "</p>";

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
	 * IMHO, it makes no sense to show an "emergency message" for days and days. Therefore, there has to be a limit to the time any
	 * message is shown.
	 * 
	 * @param dwellTime
	 *            the amount of time this message should be seen
	 */
	public void setDwellTime(final long dwellTime) {
		this.dwellTime = dwellTime;
	}
}
