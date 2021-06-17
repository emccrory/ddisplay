package gov.fnal.ppd.dd.signage;

import gov.fnal.ppd.dd.emergency.EmergencyMessage;

/**
 * Abstraction for the object that carries an emergency message in the Fermilab Dynamic Displays system
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 */
public interface EmergencyCommunication extends SignageContent {

	/**
	 * @return the message that is to be communicated
	 */
	public EmergencyMessage getMessage();

	/**
	 * @param theMessage
	 *            The message that is to be communicated
	 */
	public void setMessage(final EmergencyMessage theMessage);
}
