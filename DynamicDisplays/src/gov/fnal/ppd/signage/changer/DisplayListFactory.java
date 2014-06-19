package gov.fnal.ppd.signage.changer;

import gov.fnal.ppd.signage.display.DisplayListDatabaseRemote;

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
	public static DisplayList getInstance() {
		return new DisplayListDatabaseRemote();
	}
}
