package gov.fnal.ppd.dd.util;

/**
 * An interface to allow a details class to tell a high-level class to activate a specific display card. This is used to have the
 * splash screen (screen saver) go away and show the GUI again.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public interface DisplayCardActivator {

	/**
	 * @param show
	 */
	public void activateCard(boolean show);
}
