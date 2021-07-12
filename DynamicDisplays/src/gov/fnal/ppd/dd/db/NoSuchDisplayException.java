package gov.fnal.ppd.dd.db;

/**
 * An exception that is thrown in the (hopefully never seen) case when the display details cannot be figured out.
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
