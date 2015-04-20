/*
 * URLChangeListener
 * 
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.attic;

import java.net.URL;

/**
 * @deprecated -- Not used anywhere
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public interface URLChangeListener {

	/**
	 * @param newURL
	 * @return Was the change successful?
	 */
	public boolean changeURL(URL newURL);
}
