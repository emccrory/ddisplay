package gov.fnal.ppd.signage;

/**
 * Specific exception that is thrown when one cannot contact the remote Dynamic Displays database.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 *
 */
public class SignageDatabaseNotVisibleException extends Exception {

	private static final long	serialVersionUID	= -4495579375575150057L;

	/**
	 * @param message The associated message for this exception.
	 */
	public SignageDatabaseNotVisibleException(final String message) {
		super(message);
	}
}