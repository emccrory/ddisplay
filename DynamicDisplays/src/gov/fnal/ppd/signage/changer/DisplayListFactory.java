package gov.fnal.ppd.signage.changer;

import gov.fnal.ppd.signage.display.DisplayListDatabaseRemote;

public class DisplayListFactory {

	public static enum DisplayDebugTypes {
		TESTING, REAL_BUT_LOCAL, REAL_AND_REMOTE
	};

	private static DisplayList	me;

	private DisplayListFactory() {
	}

	public static void useRealDisplays(DisplayDebugTypes real) {
		switch (real) {
		case TESTING:
			me = new DisplayListTesting();
			break;

		case REAL_BUT_LOCAL:
			me = new DisplayListDatabase();
			break;

		case REAL_AND_REMOTE:
			me = new DisplayListDatabaseRemote();
			break;
		}
	}

	public static DisplayList getInstance() {
		return me;
	}
}
