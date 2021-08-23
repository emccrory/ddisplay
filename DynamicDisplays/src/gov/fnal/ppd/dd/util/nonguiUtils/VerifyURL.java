package gov.fnal.ppd.dd.util.nonguiUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * <p>
 * Check to see if a specific URL corresponds to an active and visible web page.
 * </p>
 * 
 * <p>
 * This class will be used when preparing to send a URL to the browser to give an indication if this page is working or not. It is
 * not fool-proof - the page can be valid one moment (when this class accesses it) and then invalid the next moment (when the
 * browser tries to access it). But given the problems we are having with cross-site access in the IFrame within the browser, this
 * might be the best we can do.
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class VerifyURL {

	private static String whyFailed = null;
	

	private VerifyURL() {
		// Only the static method isValid is needed
	}

	/**
	 * To the best of our knowledge, is this URL valid at this time? echo echo
	 * "======================================================================" echo "=========="
	 * gov.fnal.ppd.dd.util.nonguiUtils.JavaVersion java gov.fnal.ppd.dd.util.nonguiUtils.JavaVersion
	 * 
	 * 
	 * @param u
	 *            The URL to test
	 * @return Is it valid?
	 */
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

	public static String getWhyFailed() {
		return whyFailed;
	}

	public static void main(String[] args) {
		try {
			System.out.println("The url [" + args[0] + "] is " + (isValid(args[0]) ? "valid" : "NOT valid - " + whyFailed));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
