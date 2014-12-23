package gov.fnal.ppd.dd.testing;


/**
 * Testing the functionality of Selenium for the Dynamic Displays.  
 * 
 * Copied from http://code.google.com/p/selenium/wiki/Grid2
 * 
 * It seems to work, but at the expense of having to run at least one server process (and maybe two) on the 
 * node where the browser is located.  This development abandoned, 11/14/14, when I was told about an update to the
 * FF-Remote-Control plugin, which solve the problem at hand.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class SeleniumTest1 {
//	// FIXME -- Not working!
//	private static String	GO_TO_FULL_SCREEN	= "function requestFullScreen(element) {\n"
//														+ "var requestMethod = element.requestFullScreen || element.webkitRequestFullScreen || element.mozRequestFullScreen || element.msRequestFullscreen;\n"
//														+ "if (requestMethod) {  requestMethod.call(element);\n"
//														+ "} else if (typeof window.ActiveXObject !== \"undefined\") {\n"
//														+ "    var wscript = new ActiveXObject(\"WScript.Shell\");\n"
//														+ "    if (wscript !== null) { wscript.SendKeys(\"{F11}\"); }\n"
//														+ "    }\n}\n  var elem = document.getElementById(\"iframe\");\n  requestFullScreen(elem);\n";
//
//	private static String	colorCode;
//	private FirefoxDriver	driver				= new FirefoxDriver();
//
//	SeleniumTest1() {
//
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			public void run() {
//				if (driver != null) {
//					driver.quit();
//					driver.close();
//					driver.kill();
//				}
//			}
//		});
//
//		try {
//			driver.navigate().to("http://mccrory.fnal.gov/border.php");
//
//			catchSleep(5000L);
//			send(GO_TO_FULL_SCREEN);
//			System.out.println("Did we go full screen??");
//
//			String[] urls = { "http://mccrory.fnal.gov", "http://mccrory.fnal.gov/XOC", "http://sist.fnal.gov" };
//
//			while (true) {
//				for (String U : urls) {
//					System.out.println(driver.getCurrentUrl());
//					catchSleep(5000L);
//					// driver.navigate().to(U);
//					send("document.getElementById('iframe').src = '" + U + "';\n");
//				}
//
//				catchSleep(2000L);
//				showIdentity();
//				catchSleep(10000L);
//				removeIdentify();
//
//			}
//		} catch (UnreachableBrowserException e) {
//			System.err.println("It seems that the browser is gone now.  We'll exit, too");
//			System.exit(0);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//	}
//
//	private void send(String s) {
//		try {
//			driver.executeAsyncScript(s, (Object) null);
//		} catch (TimeoutException e) {
//			System.err.println("Ignoring timeout " + e);
//		}
//	}
//
//	private void showIdentity() {
//		String s = "document.getElementById('numeral').style.opacity=0.8;\n";
//		s += "document.getElementById('numeral').style.font='bold 750px sans-serif';\n";
//		s += "document.getElementById('numeral').style.textShadow='0 0 18px black, 0 0 18px black, 0 0 18px black, 0 0 18px black, 0 0 18px black, 16px 16px 2px #"
//				+ colorCode + "';\n";
//		s += "document.getElementsByTagName('body')[0].setAttribute('style', 'background-color: #" + colorCode
//				+ "; padding:0; margin: 100;' );\n";
//		s += "document.getElementById('iframe').style.width=1700;\n";
//		s += "document.getElementById('iframe').style.height=872;\n";
//		s += "document.getElementById('colorName').innerHTML = '#" + colorCode + "';\n";
//
//		send(s);
//		System.out.println("--Sent: [[" + s + "]]");
//	}
//
//	/**
//	 * Remove the self-identify dressings on the Display
//	 */
//	private void removeIdentify() {
//		String s = "document.getElementById('numeral').style.opacity=0.4;\n";
//		s += "document.getElementById('numeral').style.font='bold 120px sans-serif';\n";
//		s += "document.getElementsByTagName('body')[0].setAttribute('style', 'padding:0; margin: 0;');\n";
//		s += "document.getElementById('numeral').style.textShadow='0 0 4px black, 0 0 4px black, 0 0 4px black, 0 0 4px black, 0 0 4px black, 6px 6px 2px #"
//				+ colorCode + "';\n";
//		s += "document.getElementById('iframe').style.width=1916;\n";
//		s += "document.getElementById('iframe').style.height=1074;\n";
//		s += "document.getElementById('colorName').innerHTML = '';\n";
//
//		send(s);
//		System.out.println("--Sent: [[" + s + "]]");
//	}
//
//	/**
//	 * @param args
//	 */
//	public static void main(final String[] args) {
//		colorCode = "00ff33";
//		new SeleniumTest1();
//	}
}
