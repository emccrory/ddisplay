package gov.fnal.ppd.dd.db;

/**
 * A special exception used during startup.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class NoSuchDisplayException extends Exception {

	private static final long serialVersionUID = -4818498891468716674L;

	public NoSuchDisplayException(String message) {
		super(message);
	}

	
}
