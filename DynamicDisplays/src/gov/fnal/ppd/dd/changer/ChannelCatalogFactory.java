/*
 * ChannelCatalogFactory
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

/**
 * Singleton class to deal with getting the Channel Catalog to the client.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelCatalogFactory {

	private static ChannelCatalog	me;

	private ChannelCatalogFactory() {
		try {
			me = new ChannelsFromDatabase();
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Do we use real channels from the database, or not? This is irrelevant now as we always use real channels
	 * 
	 * @param real
	 * @deprecated
	 */
	public static void useRealChannels(boolean real) {
		assert (real);
		// if (real)
		// me = new ChannelsFromDatabase();
		// else {
		// me = new ChannelCatalogTesting();
		// new Exception(" NOT USING REAL CHANNELS!").printStackTrace();
		// }
	}

	/**
	 * @return The singleton
	 */
	public static ChannelCatalog getInstance() {
		if (me == null)
			refresh();
		return me;
	}

	/**
	 * Re-read the channels from the database
	 * 
	 * @return The instance of ChannelCatalog that is relevant here
	 */
	public static ChannelCatalog refresh() {
		me = null;
		System.gc();
		try {
			me = new ChannelsFromDatabase();
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
		}
		return me;
	}
}
