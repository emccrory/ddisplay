package gov.fnal.ppd.dd.comm.attic;

import java.net.URL;

/**
 * @deprecated -- Not used anywhere
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 *
 */
public interface URLChangeListener {

	/**
	 * @param newURL
	 * @return Was the change successful?
	 */
	public boolean changeURL(URL newURL);
}
