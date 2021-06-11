/*
 * NoPrivateKeyException
 *
 * Copyright (c) 2015-21 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.chat;

/**
 * <p>
 * This exception is thrown if a client cannot sign a message because it cannot find its a private key.
 * </p>
 * <p>
 * Note that the messaging server does not need, or have, a private key.
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class NoPrivateKeyException extends Exception {
	private static final long serialVersionUID = -6598594995368928L;

	/* *** Force the user to use only the String constructor *** */

	/**
	 * @param message
	 */
	public NoPrivateKeyException(final String message) {
		super(message);
	}

}
