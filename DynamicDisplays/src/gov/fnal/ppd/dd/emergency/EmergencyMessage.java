package gov.fnal.ppd.dd.emergency;

import java.io.Serializable;


/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class EmergencyMessage implements Serializable {
	
	private static final long	serialVersionUID	= 7099415687513564803L;
	
	private String		headline = "", message = "", footnote = "";
	private Severity	severity = Severity.TESTING;

	/**
	 * 
	 */
	public EmergencyMessage() {
		setSeverity(Severity.TESTING);
	}

	/**
	 * @return
	 */
	public String getFootnote() {
		return footnote;
	}

	/**
	 * @param footnote
	 */
	public void setFootnote(final String footnote) {
		this.footnote = footnote.replaceAll("(\\r|\\n)", " ");;
	}

	/**
	 * @return
	 */
	public String getHeadline() {
		return headline;
	}

	/**
	 * @param headline
	 */
	public void setHeadline(final String headline) {
		this.headline = headline.replaceAll("(\\r|\\n)", " ");;
	}

	/**
	 * @return
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
	 * @return
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
		return "EmergencyMessage [headline=" + headline + ", message=" + message + ", footnote=" + footnote + ", severity="
				+ severity + "]";
	}

	/**
	 * @return an HTML string that represents this emergency message
	 */
	public String toHTML() {
		String retval = "";

		retval += "<img src='http://dynamicdisplays.fnal.gov/emergency/fermilabLogo.jpg' class='logo'>";
		String headlineClass = ("" + severity).toLowerCase();
		retval += "<h1 class='" + headlineClass + "'>" + headline.replace("\'", "\"") + "</h1>";
		retval += "<p class='message'>" + message.replace("\'", "\"") + "</p>";

		if (footnote != null && footnote.length() > 0)
			retval += "<p class='footnote'>" + footnote.replace("\'", "\"") + "</p>";

		return retval;
	}
}
