package gov.fnal.ppd.dd.util.nonguiUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Check to see if a specific URL corresponds to an active and visible web page.
 * 
 * This class will be used when preparing to send a URL to the browser to give an indication if this page is working or not. It is
 * not fool-proof - the page can be valid one moment (when this class accesses it) and then invalid the next moment (when the
 * browser tries to access it). But given the problems we are having with cross-site access in the IFrame within the browser, this
 * might be the best we can do.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class URLTest {

	private static String whyFailed = null;

	private URLTest() {
		// Only the static method isValid is needed
	}

	public static boolean isValid(String u) {
		try {
			URL url = new URL(u);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
			return in.readLine() != null;
		} catch (Exception e) {
			whyFailed = e.getClass().getCanonicalName();
			return false;
		}
	}

	public static void main(String[] args) {
		try {
			System.out.println("The url [" + args[0] + "] is " + (isValid(args[0]) ? "valid" : "NOT valid - " + whyFailed));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
