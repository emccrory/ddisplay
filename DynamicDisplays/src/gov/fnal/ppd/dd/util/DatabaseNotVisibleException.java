/*
 * DatabaseNotVisibleException
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util;

/**
 * Specific exception that is thrown when one cannot contact the remote Dynamic Displays database.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class DatabaseNotVisibleException extends Exception {

	private static final long	serialVersionUID	= -4495579375575150057L;

	/**
	 * @param message The associated message for this exception.
	 */
	public DatabaseNotVisibleException(final String message) {
		super(message);
	}
}