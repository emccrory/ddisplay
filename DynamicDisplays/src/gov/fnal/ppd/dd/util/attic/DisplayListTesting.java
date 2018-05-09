/*
 * DisplayListTesting
 * 
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.attic;

import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.HSBColor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @deprecated
 */
public class DisplayListTesting extends ArrayList<Display> {
	private static final long serialVersionUID = -5048596353258886811L;
	private static int nd = 20;

	private static SignageType [] categories;

	/**
	 * @param numDisplays
	 */
	public static void setNumDisplays( final int numDisplays ) {
		// nd = numDisplays;
		// HSBColor.setNumColors(nd);
		//
		// categories = new SignageType [numDisplays];
		// final int numPublic = (numDisplays > 10 ? numDisplays / 5 : 2);
		// for (int i = 0; i < categories.length; i++)
		// categories[i] = (i < numPublic ? SignageType.Public
		// : (i >= categories.length - 3 ? SignageType.XOC
		// : SignageType.Experiment));
	}

	/**
	 * 
	 */
	public DisplayListTesting() {
//		super();
//
//		for (int i = 0; i < nd; i++) {
//			// Display d = new DisplayImpl(getTheColor(i));
//			// Display d = new DisplayAsJFrame(getTheColor(i), categories[i]);
//			Display d = new DisplayAsJxBrowser(null, i, i, null, getTheColor(i), categories[i]);
//			// System.out.println("Display " + i + ": " + d.getPreferredHighlightColor());
//			new DisplayMonitor(d);
//			add(d);
//		}
	}

	
	/**
	 * @param cat
	 */
	public void getSelectDisplays( final SignageType cat ) {
		@SuppressWarnings("unchecked")
		List<Display> p = (List<Display>) clone();
		for (Display D: p)
			if (D.getCategory() != cat)
				remove(D);

	}

	protected static Color getTheColor( int val ) {
		return new HSBColor(val);
	}

	/**
	 * @param dn
	 * @return the SignageType for this display number
	 */
	public SignageType getCategory( int dn ) {
		return categories[dn];
	}

	/**
	 * @return the SignageTypes
	 */
	public SignageType [] getCategories() {
		return categories;
	}
}
