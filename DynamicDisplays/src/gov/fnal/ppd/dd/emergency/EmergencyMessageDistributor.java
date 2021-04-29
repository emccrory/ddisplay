package gov.fnal.ppd.dd.emergency;

/**
 * The signature of the object that can send an emergency message out to the displays. This is a client-side object.
 * 
 * This interface is implemented two ways: One for the test version that does not send out these messages and once for the real
 * object that actually sends out the messages.
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

	/**
	 * @return A likely count of the displays this will be sent to.
	 */
	public int getNumOfDisplays();
}
