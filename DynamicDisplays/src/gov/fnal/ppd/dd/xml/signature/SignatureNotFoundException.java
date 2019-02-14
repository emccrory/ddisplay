package gov.fnal.ppd.dd.xml.signature;

public class SignatureNotFoundException extends Exception {

	private static final long serialVersionUID = -6565305325434724558L;

	public SignatureNotFoundException(String text) {
		super(text);
	}
}