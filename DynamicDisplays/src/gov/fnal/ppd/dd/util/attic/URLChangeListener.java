package gov.fnal.ppd.dd.util.attic;

import java.net.URL;

/**
 * @deprecated -- Not used anywhere
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 *
 */
public interface URLChangeListener {

	/**
	 * @param newURL
	 * @return Was the change successful?
	 */
	public boolean changeURL(URL newURL);
}
