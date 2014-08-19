package gov.fnal.ppd.signage.changer;

import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.display.DisplayListDatabaseRemote;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
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
	 * @return My instance
	 */
	public static List<Display> getInstance(SignageType type) {
		if (type == SignageType.XOC)
			return new DisplayListDatabaseRemote();
		if (type == SignageType.Experiment)
			throw new RuntimeException("Unimplemented code!");
		List<Display> retval = new ArrayList<Display>();
		List<Display> all = new DisplayListDatabaseRemote();
		for (Display D : all)
			if (D.getCategory() == SignageType.Public)
				retval.add(D);
		assert (retval.size() > 0);
		return retval;
	}
}
