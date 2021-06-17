package gov.fnal.ppd.dd.xml.signature;

/**
 * Thrown when a signature is not found.  In a well-behaved system you won't see this.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014-21
 *
 */
public class SignatureNotFoundException extends Exception {

	private static final long serialVersionUID = -6565305325434724558L;

	public SignatureNotFoundException(String text) {
		super(text);
	}
}