package gov.fnal.ppd.dd.display.client.selenium;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.opera.OperaDriver;

import gov.fnal.ppd.dd.display.client.ConnectionToBrowserInstance;
import gov.fnal.ppd.dd.util.PropertiesFile;

/**
 * The class that connects us to the browser via Selenium
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class SeleniumConnectionToBrowser extends ConnectionToBrowserInstance {
	private String				browser;
	private WebDriver			driver;
	private JavascriptExecutor	jse;

	private boolean				uninitialized	= true;
	private Object				lastReturnValue;

	/**
	 * Construct the connection
	 * 
	 * @param screenNumber
	 * @param virtualID
	 * @param dbID
	 * @param color
	 * @param showNumber
	 */
	public SeleniumConnectionToBrowser(final int screenNumber, final int virtualID, final int dbID, final Color color,
			final boolean showNumber) {
		super(screenNumber, virtualID, dbID, color, showNumber);
	}

	/**
	 * Construct the connection. To be used only in testing
	 */
	public SeleniumConnectionToBrowser() {
		bounds = new Rectangle(800, 700);
		// Make sure we exit the instance(s) of the browser when the VM exits ---------------------------
		Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook_Display_" + numberOfScreens) {
			public void run() {
				exit();
			}
		});
	}

	// public SeleniumConnectionToBrowser(final String url) {
	// setBrowser();
	// setBrowserConfig();
	//
	// setURL(url);
	//
	// // Set the Script Timeout to 100 milliseconds
	// driver.manage().timeouts().setScriptTimeout(100, TimeUnit.MILLISECONDS);
	//
	// }

	/**
	 * Complete the setup of the connection to the browser
	 */
	public void openConnection() {
		println(getClass(), "Screen " + screenNumber + " - Opening Selenium/Firefox connection");
		setBrowser();
		setBrowserConfig();

		if (driver == null) {
			// Oops! This is not good.
			println(getClass(), " Aborting in 10 seconds because the connection to the browser seems to have failed.");
			catchSleep(10000);
			System.exit(0);
		}

		// Don't wait very long for the pushed Javascript to finish
		driver.manage().timeouts().setScriptTimeout(100, TimeUnit.MILLISECONDS);

		// The URL must be set during the initializtion, to our special framed HTML.
		setURL(WEB_PAGE_EMERGENCY_FRAME);

		connected = true;
	}

	/**
	 * End the connection to the browser. This will close the browser
	 */
	@Override
	public void exit() {
		if (driver != null)
			driver.close();
	}

	private void setBrowser() {
		browser = PropertiesFile.getProperty("browser");
		System.setProperty("webdriver.firefox.bin", "/usr/bin/firefox");
	}

	private void setBrowserConfig() {
		try {
			String suffix = "";
			if (System.getProperty("os.name").contains("windows"))
				suffix = ".exe";
			else if (System.getProperty("os.name").contains("mac"))
				suffix = "_mac";
			// TODO - See if the architecture is ARM, and then act on it.

			String d = System.getProperty("user.dir");
			if (browser.contains("Firefox")) {
				// Only the Firefox driver has been tested (EM 6/2018)
				d += "/lib/selenium/geckodriver" + suffix;
				System.setProperty("webdriver.gecko.driver", d);
				println(getClass(), " Using this executable as the browser driver: " + d);
				driver = new FirefoxDriver();
			} else if (browser.contains("Chrome")) {
				// TODO - Untested
				d += "/lib/selenium/chromedriver" + suffix;
				System.setProperty("webdriver.chrome.driver", d);
				println(getClass(), " Using this executable as the browser driver: " + d);
				driver = new ChromeDriver();
			} else if (browser.contains("Edge")) {
				// TODO - Untested
				d += "/lib/selenium/edgedriver" + suffix;
				System.setProperty("webdriver.chrome.driver", d);
				println(getClass(), " Using this executable as the browser driver: " + d);
				driver = new EdgeDriver();
			} else if (browser.contains("Opera")) {
				// TODO - Untested and probably not right at all
				d += "/lib/selenium/geckodriver" + suffix;
				System.setProperty("webdriver.chrome.driver", d);
				println(getClass(), " Using this executable as the browser driver: " + d);
				driver = new OperaDriver();
			} else {
				System.err.println("*****");
				(new Exception("ABORT: Unknown browser type specified: [" + browser + "]")).printStackTrace();
				System.exit(-2);
			}
			jse = (JavascriptExecutor) driver;
		} catch (Exception e) {
			System.err.println("Exception caught and ignored in " + getClass().getSimpleName() + ".setBrowserConfig()");
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void send(String command) {
		println(getClass(), " Sending " + instance + " [" + command.substring(0, command.length() - 2) + "]");
		try {
			command += "return 1;\n";
			lastReturnValue = jse.executeScript(command, new Object[] { "nothing" });
			// If it goes as planned, the return value from this call will the the "return 1" I put in there.
			connected = lastReturnValue.equals(1L);
		} catch (NullPointerException e) {
			// Up to now, this exception has meant that jse is null.
			e.printStackTrace();
		} catch (Exception e) {
			lastReturnValue = lastReturnValue.toString() + " - " + " Error sending this to the browser: [" + command + "]";
			println(getClass(), lastReturnValue.toString());
			e.printStackTrace();
		}

		if (!connected) {
			println(getClass(), " - Error return from Javascript: [" + lastReturnValue + "]");
		}
	}

	/**
	 * Change the visible web page. Note that this assumes that this is a Dynamic Displays instance, which has an iframe into which
	 * we put the content.
	 * 
	 * @param url
	 *            The web page to visit
	 */
	public void setURL(String url) {
		try {
			if (uninitialized) {
				uninitialized = false;
				lastReturnValue = new Long(1);
				driver.get(url);

				// Does not work on GeckoDriver versions before 0.20 ...
				driver.manage().window().fullscreen();
				catchSleep(100);
				driver.navigate().refresh();
				connected = true;

				// A reasonable way to "reset" things would be to set uninitialized to true and then call this method with a new
				// URL. This is a _little_ tricky because here the URL we show has to be the so-called "border" web page with an
				// iframe.

			} else {
				// This assumes the Dynamic Displays border web page, which has the iframe for the content.
				send("document.getElementById('iframe').src='" + url + "';\n");
			}
		} catch (Exception e) {
			if (e.getLocalizedMessage().contains("fullscreenWindow")) {
				System.err.println(new Date() + " " + getClass().getSimpleName()
						+ ".setURL() - cannot set to full screen (known problem with geckodriver v19)");
			} else
				e.printStackTrace();
		}
	}

	@Override
	public void forceRefresh(int frameNumber) {
		driver.navigate().refresh();
	}

	@Override
	public Object getTheReply() {
		// If there is anything BUT a Long object of value 1, then it failed
		return lastReturnValue;
	}

	@Override
	public String getInstance() {
		// TODO Auto-generated method stub
		return instance;
	}

	@Override
	protected void setPositionAndSize() {
		// TODO - Fix this next statement, somehow.
		// driver.manage().window().setPosition(new Point((int) bounds.getX(), (int) bounds.getY()));
	}
}
