package gov.fnal.ppd.signage.changer;

/**
 * Singleton class to deal with getting the Channel Catalog to the client.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class ChannelCatalogFactory {

	private static ChannelCatalog	me	= new ChannelsFromDatabase();

	private ChannelCatalogFactory() {
	}

	/**
	 * Do we use real channels from the database, or not?  This is irrelevant now as we always use real channels
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
		// if (me == null)
		// useRealChannels(false);
		return me;
	}
}
