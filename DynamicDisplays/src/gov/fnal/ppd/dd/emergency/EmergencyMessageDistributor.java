package gov.fnal.ppd.dd.emergency;

/**
 * The signature of the object that can send an emergency message out to the displays. This is a client-side object.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public interface EmergencyMessageDistributor {

	/**
	 * Send the message out to the displays.
	 * 
	 * @param em
	 *            The message to send
	 * @return Did it work?
	 */
	public boolean send(final EmergencyMessage em);
}
