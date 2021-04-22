package gov.fnal.ppd.dd.channel.list;

/**
 * When a new list is created by the user, the top-level GUI (in this case, the ChannelSelector) needs to know so
 * it can reload the lists.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public interface NewListCreationListener {
	/**
	 * The callback that the sub-class must implement
	 */
	public void newListCreationCallback();
}
