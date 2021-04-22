package gov.fnal.ppd.dd.display.client;

/**
 * A listener interface to allow an object to be told when a page load fails.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public interface BrowserErrorListener {

	public void errorInPageLoad(int value);
}
