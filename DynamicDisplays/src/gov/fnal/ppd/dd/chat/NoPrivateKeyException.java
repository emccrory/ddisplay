package gov.fnal.ppd.dd.chat;

/**
 * This client canno0t sdign a message because it does not have a private key. This is OK for the messaging server (and the expected
 * situation).
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class NoPrivateKeyException extends Exception {

	public NoPrivateKeyException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public NoPrivateKeyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NoPrivateKeyException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoPrivateKeyException(String message) {
		super(message);
	}

	public NoPrivateKeyException(Throwable cause) {
		super(cause);
	}

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6598594995368928L;

}
