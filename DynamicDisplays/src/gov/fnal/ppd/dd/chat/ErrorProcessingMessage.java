package gov.fnal.ppd.dd.chat;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class ErrorProcessingMessage extends Exception {

	private static final long	serialVersionUID	= -3793753596302036490L;

	/**
	 * 
	 */
	public ErrorProcessingMessage() {
		super();
	}

	/**
	 * @param message
	 */
	public ErrorProcessingMessage(final String message) {
		super(message);
	}

}
