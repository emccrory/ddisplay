package gov.fnal.ppd.security;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 *
 */
public class DatabaseNotVisibleException extends Exception {

	private static final long	serialVersionUID	= 553037197993810792L;

	/**
	 * @param s
	 */
	public DatabaseNotVisibleException(final String s) {
		super(s);
	}

	public static void main(String[] args) {
		new DatabaseNotVisibleException("Howdy!").printStackTrace();
	}
	
}
