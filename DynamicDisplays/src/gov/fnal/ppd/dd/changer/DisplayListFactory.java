/*
 * DisplayListFactory
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import gov.fnal.ppd.dd.display.DisplayListDatabaseRemote;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that contains static methods for retrieving the displays in the system. {@link DisplayListDatabaseRemote}
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class DisplayListFactory {

	private DisplayListFactory() {
	}

	/**
	 * @param type
	 * @param locationCode
	 * @return The list of displays that are of the right type and the right location
	 */
	public static List<Display> getInstance(final SignageType type, final int locationCode) {
		if (type == SignageType.XOC)
			return new DisplayListDatabaseRemote(locationCode);
		// if (type == SignageType.Experiment)
		// throw new RuntimeException("Unimplemented code!");
		// SignageType == Experiment means a "Public" controller for a Docent.

		// Public only
		List<Display> retval = new ArrayList<Display>();
		List<Display> all = new DisplayListDatabaseRemote(locationCode);
		for (Display D : all)
			if (D.getCategory() == SignageType.Public)
				retval.add(D);
		assert (retval.size() > 0);
		return retval;
	}

	/**
	 * @param type
	 * @return My instance
	 */
	public static List<Display> getInstance(final SignageType type) {
		return getInstance(type, 0);
	}
}
