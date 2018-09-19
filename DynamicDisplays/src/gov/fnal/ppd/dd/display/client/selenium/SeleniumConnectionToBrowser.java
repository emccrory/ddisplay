package gov.fnal.ppd.dd.display.client.selenium;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.opera.OperaDriver;

import gov.fnal.ppd.dd.display.client.ConnectionToBrowserInstance;
import gov.fnal.ppd.dd.display.client.DisplayControllerMessagingAbstract;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.ExitHandler;
import gov.fnal.ppd.dd.util.PropertiesFile;

/**
 * The class that connects us to the browser via Selenium
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class SeleniumConnectionToBrowser extends ConnectionToBrowserInstance {
	private String								browser;
	private WebDriver							driver;
	private JavascriptExecutor					jse;
	private DisplayControllerMessagingAbstract	myDisplay;

	private boolean								uninitialized	= true;
	private Object								lastReturnValue;

	private CheckAndFixScreenDimensions			afsd			= null;

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
			final boolean showNumber, DisplayControllerMessagingAbstract d) {
		super(screenNumber, virtualID, dbID, color, showNumber);
		myDisplay = d;
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
		driver.manage().timeouts().setScriptTimeout(200, TimeUnit.MILLISECONDS);

		// The URL must be set during the initialization, to our special framed HTML.
		String initialURL = WEB_PAGE_EMERGENCY_FRAME + "?display=" + virtualID + "&color=" + colorCode + "&shownumber=" + (showNumber? 1 : 0);
		setURL(initialURL);

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
	}

	//
	/**
	 * Determine the browser configuration, which depends on the choice of browsers, the architecture we are running on, and on the
	 * operating system
	 * 
	 */
	private void setBrowserConfig() {
		String browserLocation = null;
		String osName = System.getProperty("os.name").toUpperCase();
		String driverFile = null;
		try {
			// ASSUME that we are using FireFox, which is the only browser we have tested to date (6/2018)

			// ASSUME the location of FireFox for Linux
			browserLocation = PropertiesFile.getProperty("binLinux");

			driverFile = System.getProperty("user.dir") + PropertiesFile.getProperty("browserDriver");

			if (osName.toUpperCase().contains("WINDOWS")) {
				browserLocation = PropertiesFile.getProperty("binWindows");
			} else if (osName.toUpperCase().contains("MAC")) {
				driverFile = System.getProperty("user.dir") + PropertiesFile.getProperty("browserDriverMac");
				browserLocation = PropertiesFile.getProperty("binMac");
			}

			println(getClass(), "Browser driver is " + driverFile + ", Browser executable is " + browserLocation);

			if (browser.contains("Firefox")) {
				// >>>>>>>>>> Only the Firefox driver has been tested (EM 6/2018) <<<<<<<<<<
				System.setProperty("webdriver.gecko.driver", driverFile);
				driver = new FirefoxDriver();
			} else if (browser.contains("Chrome")) {
				// >>>>>>>>>>Initial tests of the Chromium driver were successful (EM 9/2018) <<<<<<<<<<
				System.setProperty("webdriver.chrome.driver", driverFile);
				driver = new ChromeDriver();
			} else if (browser.contains("Edge")) {
				// TODO - Untested
				System.setProperty("webdriver.chrome.driver", driverFile);
				driver = new EdgeDriver();
			} else if (browser.contains("Opera")) {
				// TODO - Untested and probably not right at all
				System.setProperty("webdriver.chrome.driver", driverFile);
				driver = new OperaDriver();
			} else {
				System.err.println("\n\n*****");
				(new Exception("ABORT: Unknown browser type specified: [" + browser + "]")).printStackTrace();
				System.exit(-2);
			}

			jse = (JavascriptExecutor) driver;
		} catch (Exception e) {
			System.err.println("Exception caught in " + getClass().getSimpleName() + ".setBrowserConfig()");
			e.printStackTrace();
			System.err.println("Browser driver is " + driverFile + ", Location of browser: " + browserLocation
					+ ", Operating system: " + osName);
			System.err.println("Aborting...");
			System.exit(-2);
		}

		// Setup the timer task to make sure the screen is the right size, forevermore.

		Point p = new Point(bounds.x, bounds.y);
		Dimension sd = new Dimension(bounds.width, bounds.height);
		afsd = new CheckAndFixScreenDimensions(driver, p, sd);

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(afsd, ONE_MINUTE, ONE_MINUTE);
	}

	boolean notWaitingYet = true;

	@Override
	public synchronized void send(String command) {
		println(getClass(), " Sending " + instance + " [" + command.substring(0, command.length() - 2) + "]");
		try {
			command += "return 1;\n";
			lastReturnValue = jse.executeScript(command, new Object[] { "nothing" });
			// If it goes as planned, the return value from this call will the the "return 1" I put in there.
			connected = lastReturnValue.equals(1L);
		} catch (NullPointerException e) {
			// Up to now (Late Spring, 2018), this exception has meant that jse is null.
			e.printStackTrace();
		} catch (SessionNotCreatedException e) {
			printlnErr(getClass(), "- SERIOUS ERROR! - The connection to the browser has been lost. Giving up.");
			myDisplay.setOfflineMessage("Connection to Selenium disappeared");
			catchSleep(10000);
			ExitHandler.saveAndExit("Received " + e.getClass().getName() + " exception");
		} catch (Exception e) {
			lastReturnValue = lastReturnValue.toString() + " - " + " Error sending this to the browser: [" + command + "]";
			println(getClass(), lastReturnValue.toString());
			e.printStackTrace();
		}

		if (!connected) {
			println(getClass(), " - Error return from Javascript: [" + lastReturnValue + "]");
		}
		// if (notWaitingYet)
		// new Thread() {
		// public void run() {
		// notWaitingYet = false;
		// catchSleep(2000);
		// PositioningMethod positioningMethod = PropertiesFile.getPositioningMethod();
		// Point p = new Point(bounds.x, bounds.y);
		//
		// switch (positioningMethod) {
		// case DirectPositioning:
		// Dimension sd = new Dimension(bounds.width, bounds.height);
		// println(getClass(), ">>>>>>>>>> Positioning the window <<<<<<<<<<");
		// driver.manage().window().setPosition(p);
		// driver.manage().window().setSize(sd);
		// break;
		// case UseHiddenButton:
		// // driver.manage().window().setPosition(p);
		// println(getClass(), ">>>>>>>>>> Clicking <<<<<<<<<<");
		// driver.findElement(new By.ByName("hiddenButton")).click();
		// break;
		// case ChangeIframe:
		// // TODO
		// println(getClass(), ">>>>>>>>>> Adjusting size of IFrame (not implemented yet) <<<<<<<<<<");
		// break;
		// case DoNothing:
		// break;
		// }
		// }
		// }.start();
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
				println(getClass(), "First URL is " + url);
				driver.get(url);

				catchSleep(1000);
				afsd.goToProperSizeAndPlace();

				// Does not work on GeckoDriver versions before 0.20 ...
				// driver.manage().window().fullscreen();

				/*
				 * The various ways to go to full screen I have tried:
				 * 
				 * 1. driver.manage().window().fullscreen() - works under Windows 10/FF60, but not under MacOS/FF60 and not under
				 * SL7/FF56
				 * 
				 * 2. driver.manage().window().maximize() - fills the screen with the browser window, including the window
				 * dressings, the address bar, and the tab bar.
				 * 
				 * 3. send("gotoFullScreen();\n") - fails with this error message on the JsvsScript console: "Request for fullscreen
				 * was denied because Element.requestFullscreen() was not called from inside a short running user-generated event
				 * handler."
				 * 
				 * 4. driver.findElement(new By.ByName("hiddenButton")).click() - briefly goes to TRUE full screen and then reverts
				 * to a window, probably when the next URL is requested. Clicking this button again does nothing.
				 */

				// catchSleep(1000);
				// send("gotoFullScreen();\n");
				// driver.manage().window().maximize();
				// driver.findElement(new By.ByName("hiddenButton")).click();
				// driver.navigate().refresh();
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
		// Not this, although it seems like a good idea (it causes the initial web page to be displayed)
		driver.navigate().refresh();

		println(this.getClass(), "Refresh ...");
		// Note that if the full screen-ness of the browser is done in the web page, this will cause it to fall out of full screen.
		catchSleep(1000);

		afsd.goToProperSizeAndPlace();
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
