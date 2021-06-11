package gov.fnal.ppd.dd;

/**
 * An exception that is thrown if no credentials to access the database are found
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class CredentialsNotFoundException extends Exception {

	private static final long serialVersionUID = 9135212513717918907L;

	public CredentialsNotFoundException() {
		super();
	}

	public CredentialsNotFoundException(String message) {
		super(message);
	}

}
