package gov.fnal.ppd.signage.util;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class Util {
	private static final String [] days = { "", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

	private Util() {}
	
	public static final String truncate(String name, int chars){
		if (name.length() < chars)
			return name;
		return name.substring(0, chars) + " \u2026";
	}

	public static final String truncate( String name ) {
		return truncate(name, 30);
	}

	public static final String twoDigits( int v ) {
		if (v < 10)
			return "0" + v;
		return "" + v;
	}

	public static final String shortDate() {
		Calendar c = new GregorianCalendar();
		return days[c.get(Calendar.DAY_OF_WEEK)] + " " + twoDigits(c.get(Calendar.HOUR_OF_DAY))
				+ ":" + twoDigits(c.get(Calendar.MINUTE)) + ":" + twoDigits(c.get(Calendar.SECOND));
	}
}
