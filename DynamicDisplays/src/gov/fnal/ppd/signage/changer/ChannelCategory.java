package gov.fnal.ppd.signage.changer;

/**
 * The way the channels are organized for the user
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 *
 */
public enum ChannelCategory {
	// This needs to be re-thought.
	
	// There are two ways to organize channels: By what display they can be shown on and by what tab they appear in for the
	// ChannelSelector. Also, it may be necessary to add tabs for each experiment, expanding this enum.
	
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
	 * Everything else
	 */
	MISCELLANEOUS, 
	
	/**
	 * Channels that are actually just JPEG images
	 */
	IMAGE
}
