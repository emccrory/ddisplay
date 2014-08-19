package gov.fnal.ppd.signage.changer;

/**
 * The way the channels are organized/sorted for the user
 * 
 * FIXME -- This should be determined by what is in the database, not hard-coded like this
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public enum ChannelCategory {
	// This enum corresponds exactly to the names chosen for the values in the Database table, ChannelTabSort

	/**
	 * Channels on the front page
	 */
	PUBLIC,

	/**
	 * Channels on the second page
	 */
	PUBLIC_DETAILS,

	/**
	 * Channels on the front page of the "Experiments" view
	 */
	EXPERIMENT_DETAILS,
	NUMI_DETAILS,
	NOVA_DETAILS,
	
	/**
	 * Everything else
	 */
	MISCELLANEOUS,

	/**
	 * Channels that are actually just JPEG images
	 */
	IMAGE
}
