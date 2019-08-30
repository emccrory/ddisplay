package gov.fnal.ppd.dd.chat;

public class UnrecognizedCommunicationException extends Exception {

	private static final long serialVersionUID = -5788105482013640042L;

	public UnrecognizedCommunicationException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UnrecognizedCommunicationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UnrecognizedCommunicationException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnrecognizedCommunicationException(String message) {
		super(message);
	}

	public UnrecognizedCommunicationException(Throwable cause) {
		super(cause);
	}

}
