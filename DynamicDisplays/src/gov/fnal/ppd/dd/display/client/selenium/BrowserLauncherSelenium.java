package gov.fnal.ppd.dd.display.client.selenium;

import gov.fnal.ppd.dd.display.client.BrowserLauncher;

/**
 * All of the initialization is handled in the class SeleniumConnectionToBrowser. This class is essentially empty.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class BrowserLauncherSelenium extends BrowserLauncher {

	/**
	 * @param screenNumber
	 */
	public BrowserLauncherSelenium(final int screenNumber) {
		super(screenNumber);
	}

	@Override
	public void startBrowser() {
	}

	@Override
	public void exit() {		
	}

}
