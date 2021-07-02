/*
 * DatabaseNotVisibleException
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.security;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
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
}
