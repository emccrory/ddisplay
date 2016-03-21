package gov.fnal.ppd.dd.signage;

/**
 * Abstraction for the XOC Dynamic Display "Emergency" Channel
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 */
public interface EmergencyCommunication extends SignageContent {

	/**
	 * @return the message that is to be communicated
	 */
	public String getMessage();

	/**
	 * @param theMessage
	 *            The message that is to be communicated
	 */
	public void setMessage(final String theMessage);
}
