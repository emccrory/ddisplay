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
	 * The most severe sort of message. It is thought that this would be something truly severe, like an active shooter incident
	 */
	EMERGENCY,

	/**
	 * An important message. IT is thought that this would be a very important message, like
	 * "The lab will be closing at 2PM because of the snow"
	 */
	ALERT,

	/**
	 * An information-only message. It is thought that this would be an interesting and important message, like
	 * "Don't forget about the all-hands meeting today at 1PM" or
	 * "Keep the horseshoe area clear between 10 and 2 today when King William and Prince George are scheduled to arrive."
	 */
	INFORMATION,

	/**
	 * The least severe sort of message (in fact, not an emergency at all). It is thought that this would be equivalent to the old
	 * radio alerts that happen once a week or so. For example,
	 * "This is a test of the emergency broadcast system.  If this had been a real emergency, [...]"
	 */
	TESTING,

	/**
	 * remove the message
	 */
	REMOVE;
}
