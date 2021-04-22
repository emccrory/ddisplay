/*
 * DisplayKeeper
 *
 * Copyright (c) 2014-21 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.interfaces;

/**
 * An interface to allow a details class to tell a high-level class when a display is alive (breaking a circular dependency)
 * 
 * The name of this class is supposed to be what the implementer is - it is a "Keeper of (lots of) Displays".
 * Basically, the only class that implements this is the ChannelSelector.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public interface DisplayKeeper {

	/**
	 * @param displayNumber
	 * @param isAlive
	 */
	void setDisplayIsAlive(int displayNumber, boolean isAlive);

}
