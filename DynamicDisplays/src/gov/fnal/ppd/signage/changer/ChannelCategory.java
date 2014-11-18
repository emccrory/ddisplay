package gov.fnal.ppd.signage.changer;

/**
 * The way the channels are organized/sorted for the user
 * 
 * FIXME -- This should be determined by what is in the database, not hard-coded like this
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
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

	/**
	 * Channels that show details on the NuMI experiment
	 */
	NUMI_DETAILS,

	/**
	 * Channels that show details on the NOvA experiment
	 */
	NOVA_DETAILS,

	/**
	 * Channels that are (YouTube) videos
	 */
	VIDEOS,

	/**
	 * Everything else
	 */
	MISCELLANEOUS,

	/**
	 * Channels that are actually just JPEG images
	 */
	IMAGE,
	
	/**
	 * Channels that give details on accelerator operations
	 */
	ACCELERATOR,
	
	/**
	 * Channels that show general stuff about Fermilab itself
	 */
	FERMILAB
}
