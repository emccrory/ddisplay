package gov.fnal.ppd.dd.emergency;

/**
 * The Severity is use to distinguish among the sorts of messages that could be displayed. In particular, one might imagine that the
 * messages become bolder and more prominent ans they get more severe.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public enum Severity {

	/**
	 * The most severe sort of message
	 */
	EMERGENCY,

	/**
	 * 
	 */
	ALERT,

	/**
	 * 
	 */
	INFORMATION,

	/**
	 * The least severe sort of message (in fact, not an emergency at all)
	 */
	TESTING,

	/**
	 * remove the message
	 */
	REMOVE;
}
