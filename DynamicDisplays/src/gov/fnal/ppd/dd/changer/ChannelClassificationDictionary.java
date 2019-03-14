/*
 * ChannelClassificationDictionary
 *
 * Copyright (c) 2014-19 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.db.ChannelsFromDatabase.getCategoriesDatabase;

/**
 * Retrieve the categories (a.k.a., the Tab Names) that are to be used by the ChannelSelector GUI
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelClassificationDictionary {
	private static ChannelClassification[]	categories	= null;

	private ChannelClassificationDictionary() {
		// do not instantiate
	}

	/**
	 * @return The array of Channel categories that are appropriate for this instance of the ChannelSelector GUI
	 */
	public static ChannelClassification[] getCategories() {
		if (categories == null) {
			categories = getCategoriesDatabase();
		}
		return categories;
	}
}
