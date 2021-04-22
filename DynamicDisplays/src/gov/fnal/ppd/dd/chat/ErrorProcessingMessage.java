package gov.fnal.ppd.dd.chat;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class ErrorProcessingMessage extends Exception {

	private static final long	serialVersionUID	= -3793753596302036490L;

	/**
	 * Create the exception with the user-supplied message
	 * 
	 * @param message
	 */
	public ErrorProcessingMessage(final String message) {
		super(message);
	}

}
