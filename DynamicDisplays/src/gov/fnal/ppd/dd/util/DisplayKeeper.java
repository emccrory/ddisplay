package gov.fnal.ppd.dd.util;

/**
 * An interface to allow a details class to tell a high-level class when a display is alive (breaking a circular dependency)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public interface DisplayKeeper {

	/**
	 * @param displayNumber
	 * @param isAlive
	 */
	void setDisplayIsAlive(int displayNumber, boolean isAlive);

}
