package gov.fnal.ppd.dd.display.client.selenium;

import static gov.fnal.ppd.dd.util.Util.catchSleep;

import java.util.ArrayList;
import java.util.List;

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
		FirstSeleniumTest t = new FirstSeleniumTest();
		List<String> urls = new ArrayList<String>();

		urls.add("http://dynamicdisplays.fnal.gov/border7.php");
		urls.add("http://www-bd.fnal.gov/notifyservlet/www?project=HD&refresh=on&infolinks=none");
		urls.add("https://www.youtube.com/embed/sBDCIOERPNw?autoplay=1&cc_load_policy=1");

		t.runTest(urls);
	}

	/**
	 * 
	 */
	public FirstSeleniumTest() {
		connection = new SeleniumConnectionToBrowser(0, 0, 0, null, false);
		connection.openConnection();
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

		connection.exit();
	}
}
