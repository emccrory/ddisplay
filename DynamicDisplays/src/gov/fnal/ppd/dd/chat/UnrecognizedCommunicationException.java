package gov.fnal.ppd.dd.chat;

/**
 * This exception is thrown from MessageConveyor. This signals that an unknown string was seen in communicating with a client.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class UnrecognizedCommunicationException extends Exception {

	private static final long serialVersionUID = -5788105482013640042L;

	/* Only the String constructor is used */

	public UnrecognizedCommunicationException(String message) {
		super(message);
	}

}
