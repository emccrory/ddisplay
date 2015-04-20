/*
 * NoPrivateKeyException
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.chat;

/**
 * This client cannot sign a message because it does not have a private key. This is OK for the messaging server (and the expected
 * situation).
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class NoPrivateKeyException extends Exception {
	private static final long	serialVersionUID	= -6598594995368928L;

	/**
	 * Create an exception
	 */
	public NoPrivateKeyException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public NoPrivateKeyException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public NoPrivateKeyException(final String message, final Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public NoPrivateKeyException(final String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public NoPrivateKeyException(final Throwable cause) {
		super(cause);
	}

}
