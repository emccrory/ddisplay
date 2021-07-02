package gov.fnal.ppd.dd.display.client.selenium;

import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameDisplay;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.prepareUpdateWatcher;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import gov.fnal.ppd.dd.util.nonguiUtils.CredentialsNotFoundException;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class FirstSeleniumTest {

	private SeleniumConnectionToBrowser connection;

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		prepareUpdateWatcher(false);
		
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		getMessagingServerNameDisplay();

		FirstSeleniumTest t = new FirstSeleniumTest();
		List<String> urls = new ArrayList<String>();

		urls.add("https://dynamicdisplays.fnal.gov/border7.php");
		urls.add("https://www-bd.fnal.gov/notifyservlet/www?project=HD&refresh=on&infolinks=none");
		urls.add("https://www.youtube.com/embed/sBDCIOERPNw?autoplay=1&cc_load_policy=1");

		t.runTest(urls);
		System.exit(0);
	}

	/**
	 * 
	 */
	public FirstSeleniumTest() {

		boolean simple = false;
		if (simple)
			// Simple startup for testing
			connection = new SeleniumConnectionToBrowser();
		else
			// Full startup.
			connection = new SeleniumConnectionToBrowser(0, 0, 0, Color.red, true, null);

		connection.openConnection();
		// connection.setPositionAndSize();
	}

	/**
	 * @param urls
	 */
	public void runTest(final List<String> urls) {
		long sleep = 15000L;
		for (String url : urls) {
			connection.setURL(url);
			catchSleep(sleep);
		}

		// Do an "Identify"
		connection.showIdentity();

		catchSleep(sleep);
		connection.removeIdentify();
		catchSleep(sleep);
	}
}
