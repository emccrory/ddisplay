package gov.fnal.ppd.signage.changer;

import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.display.DisplayListDatabaseRemote;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class DisplayListFactory {

	// Once used during initial testing
	// public static enum DisplayDebugTypes {
	// TESTING,
	// REAL_BUT_LOCAL,
	// REAL_AND_REMOTE
	// };

	private DisplayListFactory() {
	}

	// public static void useRealDisplays(DisplayDebugTypes real) {
	// switch (real) {
	// case TESTING:
	// me = new DisplayListTesting();
	// break;
	//
	// case REAL_BUT_LOCAL:
	// me = new DisplayListDatabase();
	// break;
	//
	// case REAL_AND_REMOTE:
	// me = new DisplayListDatabaseRemote();
	// break;
	// }
	// }

	/**
	 * @param type
	 * @param locationCode
	 * @return The list of displays that are of the right type and the right location
	 */
	public static List<Display> getInstance(final SignageType type, final int locationCode) {
		if (type == SignageType.XOC)
			return new DisplayListDatabaseRemote(locationCode);
		if (type == SignageType.Experiment)
			throw new RuntimeException("Unimplemented code!");
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
