package gov.fnal.ppd.dd.chat;

/**
 * Used in MessageConveyor to signal that an unknown string was seen in communicating with a client.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class UnrecognizedCommunicationException extends Exception {

	private static final long serialVersionUID = -5788105482013640042L;

	public UnrecognizedCommunicationException(String message) {
		super(message);
	}

}
