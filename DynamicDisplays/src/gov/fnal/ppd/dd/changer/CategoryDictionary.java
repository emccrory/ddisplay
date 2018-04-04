/*
 * CategoryDictionary
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.db.ChannelsFromDatabase.getCategoriesDatabase;

/**
 * Retrieve the categories that are to be used by the ChannelSelector GUI
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class CategoryDictionary {
	private static ChannelCategory[]	categories	= null;

	private CategoryDictionary() {
		// do not instantiate
	}

	/**
	 * @return The array of Channel categories that are appropriate for this instance of the ChannelSelector GUI
	 */
	public static ChannelCategory[] getCategories() {
		if (categories == null) {
			categories = getCategoriesDatabase();
		}
		return categories;
	}
}
