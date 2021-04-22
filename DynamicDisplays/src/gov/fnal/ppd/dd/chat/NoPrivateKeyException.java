/*
 * NoPrivateKeyException
 *
 * Copyright (c) 2015-21 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
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

	/* *** Force the user to use only the String constructor *** */

	/**
	 * @param message
	 */
	public NoPrivateKeyException(final String message) {
		super(message);
	}


}
