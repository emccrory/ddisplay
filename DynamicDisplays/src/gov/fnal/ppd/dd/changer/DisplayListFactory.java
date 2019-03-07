/*
 * DisplayListFactory
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.db.DisplayUtilDatabase.getDisplays;

import java.util.List;

import gov.fnal.ppd.dd.signage.Display;

/**
 * Class that contains static methods for retrieving the displays in the system.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class DisplayListFactory {

	private DisplayListFactory() {
	}

	/**
	 * @param locationCode
	 * @return The list of displays that are of the right type and the right location
	 */
	public static List<Display> getInstance(final int locationCode) {
		return getDisplays(locationCode);
	}

}
