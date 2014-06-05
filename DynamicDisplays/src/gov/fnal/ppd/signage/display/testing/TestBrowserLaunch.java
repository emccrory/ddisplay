package gov.fnal.ppd.signage.display.testing;

import gov.fnal.ppd.signage.display.testing.BrowserLauncher.BrowserInstance;

/**
 * Test the ability to change the channel for the chosen Dynamic Display instance.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class TestBrowserLaunch {

	private TestBrowserLaunch() {
	}

	/**
	 * @param args
	 *            Expect at least two arguments
	 */
	public static void main(final String[] args) {
		if (args.length < 2) {
			System.err.println("Give me at least two URLs, please.");
			System.exit(-1);
		}
		BrowserLauncher x = new BrowserLauncher(0, BrowserInstance.FIREFOX);
		while (true)
			for (String U : args) {
				System.out.println("Changing URL to '" + U + "' ... ");
				x.changeURL(U);
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
	}
}
