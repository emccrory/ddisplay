package gov.fnal.ppd.dd.channel.list;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2017
 *
 */
public class ListUtilsGUI {
	private ListUtilsGUI(){}
	
	/**
	 * @param val The number to interpret
	 * @return A reasonable string that says how much time this number represents (seconds, minutes, hours; TODO: Days, Weeks, Months, who knows?)
	 */
	public static String interp(final long val) {
		DecimalFormat format = new DecimalFormat("#.0");
		String t = "" + val;
		if (val < 60L) {
			t = (val) + " seconds";
		} else if (val < 3600) {
			double min = (val) / 60.0;
			t = format.format(min) + " minute" + (min != 1 ? "s" : "");
		} else {
			double hours = ((double) val) / (60 * 60);
			t = format.format(hours) + " hour" + (hours != 1 ? "s" : "");
		}
		return " " + t;
	}

	/**
	 * @return A list of values that is useful in giving the user something to choose in the time realm.
	 */
	public static List<Long> getDwellStrings() {
		ArrayList<Long> retval = new ArrayList<Long>();

		retval.add(5L);
		retval.add(8L);
		retval.add(9L);
		retval.add(10L); // 10 seconds
		retval.add(12L);
		retval.add(15L);
		retval.add(20L);
		retval.add(30L);
		retval.add(45L);
		retval.add(60L);
		retval.add(75L);
		retval.add(90L);
		retval.add(100L); // 100 seconds
		retval.add(110L);
		retval.add(120L); // 2 minutes
		retval.add(150L);
		retval.add(180L);
		retval.add(210L);
		retval.add(240L);
		retval.add(300L); // 5 minutes
		retval.add(360L);
		retval.add(420L);
		retval.add(480L);
		retval.add(540L);
		retval.add(600L); // 10 minutes
		retval.add(1200L);
		retval.add(1800L); // 30 minutes
		retval.add(2400L);
		retval.add(3600L); // One hour
		retval.add(2 * 3600L);
		retval.add(3 * 3600L);
		retval.add(4 * 3600L);
		retval.add(5 * 3600L);
		retval.add(6 * 3600L);
		retval.add(8 * 3600L); // 8 hours
		retval.add(10 * 3600L); // 10 hours
		retval.add(12 * 3600L); // 12 hours

		return retval;
	}

}
