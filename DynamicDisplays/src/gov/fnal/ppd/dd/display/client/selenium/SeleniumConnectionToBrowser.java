package gov.fnal.ppd.dd.display.client.selenium;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Color;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.opera.OperaDriver;

import gov.fnal.ppd.dd.display.client.ConnectionToBrowserInstance;
import gov.fnal.ppd.dd.display.client.selenium.config.PropertiesFile;

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
	 * Construct the connection and then open a web page
	 * 
	 * @param url
	 *            The web page to visit initially
	 */
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
	protected void openConnection() {
		setBrowser();
		setBrowserConfig();

		// Don't wait very long for the pushed Javascript to finish
		driver.manage().timeouts().setScriptTimeout(100, TimeUnit.MILLISECONDS);

		connected = true;

		if (numberOfScreens > 1)
			instance = " (Screen " + numberOfScreens + ")";
		else {
			Timer timer = new Timer();
			TimerTask g = new TimerTask() {
				public void run() {
					for (int i = 0; i < 100; i++) {
						if (numberOfScreens > 1) {
							instance = " (Screen " + numberOfScreens + ")";
							return;
						}
						// It often takes a moment for the second screen to appear. This loop waits a total of 20.5 seconds.
						catchSleep(200);
					}
				}
			};
			timer.schedule(g, 500); // Run it once, after a short delay.
		}

		// Make sure we exit the instance(s) of the browser when the VM exits.
		Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook_Display_" + numberOfScreens) {
			public void run() {
				exit();
			}
		});
	}

	/**
	 * End the connection to the browser. This will close the browser
	 */
	@Override
	public void exit() {
		driver.close();
	}

	private void setBrowser() {
		browser = PropertiesFile.getProperty("browser");
		System.setProperty("webdriver.firefox.bin", "/usr/bin/firefox");
	}

	private void setBrowserConfig() {
		String exe = "";
		if (System.getProperty("os.name").contains("Windows"))
			exe = ".exe";

		String baseLocation = System.getProperty("user.dir");
		if (browser.contains("Firefox")) {
			// Only the Firefox drive has been tested (EM 6/2018)
			System.setProperty("webdriver.gecko.driver", baseLocation + "/lib/selenium/geckodriver" + exe);
			driver = new FirefoxDriver();
		} else if (browser.contains("Chrome")) {
			System.setProperty("webdriver.chrome.driver", baseLocation + "/lib/selenium/chromedriver" + exe);
			driver = new ChromeDriver();
		} else if (browser.contains("Edge")) {
			System.setProperty("webdriver.chrome.driver", baseLocation + "/lib/selenium/edgedriver" + exe);
			driver = new EdgeDriver();
		} else if (browser.contains("Opera")) {
			System.setProperty("webdriver.chrome.driver", baseLocation + "/lib/selenium/operadriver" + exe);
			driver = new OperaDriver();
		} else {
			System.err.println("*****");
			(new Exception("ABORT: Unknown browser type specified: [" + browser + "]")).printStackTrace();
			System.exit(-2);
		}
		jse = (JavascriptExecutor) driver;
	}

	@Override
	public synchronized void send(String command) {
		try {
			command += "return 1;\n";
			lastReturnValue = jse.executeScript(command, new Object[] { "nothing" });
			connected = lastReturnValue.equals(1L);
		} catch (Exception e) {
			lastReturnValue = lastReturnValue.toString() + " - " + " Error sending this to the browser: ["
					+ command.substring(0, command.length() - 2) + "]";
			println(getClass(), lastReturnValue.toString());
			e.printStackTrace();
		}

		if (!connected) {
			println(getClass(), " - Error return from Javascript: [" + lastReturnValue + "]");
		}
	}

	/**
	 * Change the visible web page. Note that this assumes that this is a Dynamic Displays instance.
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
				driver.manage().window().fullscreen();
				catchSleep(100);
				driver.navigate().refresh();
				connected = true;
			} else {
				send("document.getElementById('iframe').src='" + url + "';\n");
			}
		} catch (Exception e) {
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
