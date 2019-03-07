/*
 * DisplayListTesting
 * 
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.attic;

import java.awt.Color;
import java.util.ArrayList;

import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.HSBColor;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @deprecated
 */
public class DisplayListTesting extends ArrayList<Display> {
	private static final long serialVersionUID = -5048596353258886811L;

	/**
	 * @param numDisplays
	 */
	public static void setNumDisplays(final int numDisplays) {
	}

	/**
	 * 
	 */
	public DisplayListTesting() {
	}

	protected static Color getTheColor(int val) {
		return new HSBColor(val);
	}
}
