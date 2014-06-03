package gov.fnal.ppd.signage.changer;

/**
 * Singleton class to deal with getting the Channel Catalog to the client.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 *
 */
public class ChannelCatalogFactory {

	private static ChannelCatalog	me	= null;

	private ChannelCatalogFactory() {
	}

	/**
	 * Do we use real channels from the database, or not?
	 * @param real
	 */
	public static void useRealChannels(boolean real) {
		if (real)
			me = new ChannelsFromDatabase();
		else {
			me = new ChannelCatalogTesting();
			new Exception(" NOT USING REAL CHANNELS!").printStackTrace();
		}
	}

	/**
	 * @return The singleton 
	 */
	public static ChannelCatalog getInstance() {
		if (me == null)
			useRealChannels(false);
		return me;
	}
}
