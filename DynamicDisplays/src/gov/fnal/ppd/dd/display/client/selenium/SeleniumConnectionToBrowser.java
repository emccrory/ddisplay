package gov.fnal.ppd.dd.display.client.selenium;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.dd.GlobalVariables.POSSIBLE_RECOVERABLE_ERROR;
import static gov.fnal.ppd.dd.GlobalVariables.SIMPLE_RECOVERABLE_ERROR;
import static gov.fnal.ppd.dd.GlobalVariables.UNRECOVERABLE_ERROR;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.opera.OperaDriver;

import gov.fnal.ppd.dd.display.client.ConnectionToBrowserInstance;
import gov.fnal.ppd.dd.display.client.DisplayControllerMessagingAbstract;
import gov.fnal.ppd.dd.util.nonguiUtils.ExitHandler;
import gov.fnal.ppd.dd.util.nonguiUtils.PropertiesFile;

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

	private Object								lastReturnValue;
	private CheckAndFixScreenDimensions			afsd								= null;

	private boolean								uninitialized						= true;
	private boolean								notWaitingToLookForLoadErrors		= true;
	private boolean								notWaitingToLookForTimeoutErrors	= true;
	private long								waitForTimeoutErrorsDuration		= 0;

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
				// It looks like a "kill 1" will exit the JVM with an exit code of 129. Could not get this to work (11/2/2018)
				println(getClass(), "Shutdown hook called");
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
			connected = false;
			// Oops! This is not good.
			long delayTime = 5000;
			println(getClass(),
					" Aborting in " + delayTime / 1000L + " seconds because the connection to the browser seems to have failed.");
			catchSleep(delayTime);
			System.exit(SIMPLE_RECOVERABLE_ERROR);
		}

		// Don't wait very long for the pushed Javascript to finish
		driver.manage().timeouts().setScriptTimeout(200, TimeUnit.MILLISECONDS);

		// The URL must be set during the initialization, to our special framed HTML.
		String initialURL = WEB_PAGE_EMERGENCY_FRAME + "?display=" + virtualID + "&color=" + colorCode + "&shownumber="
				+ (showNumber ? 1 : 0);
		setURL(initialURL);

		connected = true;
	}

	/**
	 * End the connection to the browser. This will close the browser
	 */
	@Override
	public void exit() {
		if (driver != null)
			synchronized (driver) {
				try {
					driver.close();
				} catch (Exception e) {
					printlnErr(getClass(), "Exception caught while trying to close the connection to the browser.\n"
							+ "\t\tExiting the JVM should happen real soon now.");
					e.printStackTrace();
					driver = null;
				}
			}
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
		driver = null;
		
		try {
			// ASSUME that we are using FireFox, which is the only browser we have tested to date (10/2019)

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
				// >>>>>>>>>> Only the Firefox driver has been thoroughly tested (EM 6/2018) <<<<<<<<<<
				System.setProperty("webdriver.gecko.driver", driverFile);
				FirefoxOptions options = new FirefoxOptions();
				options.setLogLevel(FirefoxDriverLogLevel.TRACE);
				FirefoxDriver ffDriver = new FirefoxDriver();
				println(getClass(), "Pausing for a bit just for fun.");
				catchSleep(5000L);
				
				if (ffDriver != null) {
					// I added this block to see if tickling the driver here would reveal a failure - it does not.
					Capabilities caps = ffDriver.getCapabilities();
					Map<String, ?> allCaps = caps.asMap();
					String listOfCaps = "";
					for (String key : allCaps.keySet())
						listOfCaps += "\t\t" + key + ": " + allCaps.get(key) + "\n";

					println(getClass(), "Capabilities of the Firefox Driver we just instantiated: " + caps.getBrowserName()
							+ " version " + caps.getVersion() + ", Running on " + caps.getPlatform() + ".\n" + listOfCaps);
					
				}
				driver = ffDriver;
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
				System.exit(POSSIBLE_RECOVERABLE_ERROR);
			}

			jse = (JavascriptExecutor) driver;
		} catch (WebDriverException e) {
			System.err.println(new Date() + " WebDriverException Exception caught in " + getClass().getSimpleName()
					+ ".setBrowserConfig()" + "\n\t\tWe hope to be able to continue from this, eventually.");
			driver = null;
			return;
		} catch (Exception e) {
			System.err.println(new Date() + " Exception caught in " + getClass().getSimpleName() + ".setBrowserConfig()");
			e.printStackTrace();
			System.err.println("\n" + new Date() + " Browser driver is " + driverFile + ", Location of browser: " + browserLocation
					+ ", Operating system: " + osName);
			System.err.println(new Date() + " Aborting...");
			System.exit(UNRECOVERABLE_ERROR);
		}

		println(getClass(), "***** It looks like we have a good driver connection - we'll see! *****");

		// Setup the timer task to make sure the screen is the right size, forevermore.

		Point p = new Point(bounds.x, bounds.y);
		Dimension sd = new Dimension(bounds.width, bounds.height);
		afsd = new CheckAndFixScreenDimensions(driver, p, sd, this);

		Timer timer = new Timer();
		timer.schedule(afsd, 15 * ONE_SECOND, ONE_MINUTE);
	}

	@Override
	public synchronized void send(String command) {
		println(getClass(), " Sending " + getScreenText() + " [" + command.substring(0, command.length() - 2) + "]");
		try {
			command += "return 1;\n";
			lastReturnValue = jse.executeScript(command, new Object[] { "nothing" });
			// If it goes as planned, the return value from this call will the the "return 1" I put in there.
			connected = lastReturnValue.equals(1L);
		} catch (NullPointerException e) {
			// Up to now (Late Spring, 2018), this exception has meant that jse is null.
			e.printStackTrace();
		} catch (WebDriverException e) {
			printlnErr(getClass(), "- SERIOUS ERROR! (" + e.getClass().getCanonicalName()
					+ ") - The connection to the browser has been lost.  Will exit and try again in a moment.");
			myDisplay.setOfflineMessage("Connection to Selenium disappeared");
			ExitHandler.saveAndExit("Received " + e.getClass().getName() + " exception", -1, 5000L);
		} catch (Exception e) {
			lastReturnValue = lastReturnValue.toString() + " - " + " Error sending this to the browser: [" + command + "]";
			println(getClass(), lastReturnValue.toString());
			e.printStackTrace();
		}

		if (!connected) {
			println(getClass(), " - Error return from Javascript: [" + lastReturnValue + "]");
		}

		if (notWaitingToLookForLoadErrors) {
			notWaitingToLookForLoadErrors = false;
			new Thread("WaitingToLookForLoadErrors") {
				public void run() {
					catchSleep(2000);
					if ( !showingCanonicalSite.get()) return;
					// Read the Javascript Variable "mostRecentLoadResult"
					long returnValue = (Long) jse.executeScript("return getMostRecentLoadResult();", new Object[] { "nothing" });
					println(SeleniumConnectionToBrowser.class, " Verification of iframe load returns [" + returnValue + "]");

					// Important false-positive: If the web page is secure, "https://", it can fail and the underlying javaScript
					// that is supposed to catch it will not see it. And since almost all of our channels are secure now, this
					// code does not do very much.

					// Another false-positive: The default timeout for Firefox to wait for a page to respond is 5 minutes. So we
					// won't know if the page has timed out for a long, long time.

					if (returnValue != 0) {
						// We have a problem here! Ask the listener to deal with this.
						errorSeen(returnValue);
						waitForTimeoutErrorsDuration = 0;
					} else {
						waitForTimeoutErrorsDuration = (long) PropertiesFile.getIntProperty("BrowserTimeout",
								(int) (5 * ONE_MINUTE));
						if (notWaitingToLookForTimeoutErrors) {
							notWaitingToLookForTimeoutErrors = false;
							new Thread("LookingForTimeoutErrors") {
								public void run() {
									// TODO - Maybe this should run all the time?
									long increment = 5000;
									long returnValue = 0;
									do {
										long w = Math.min(waitForTimeoutErrorsDuration, increment);
										catchSleep(w); // total wait after the load should be 5 minutes and two seconds.
										waitForTimeoutErrorsDuration -= increment;

										// borderA has a JavaScript function that dynamically checks the value of the title in the
										// iframe.
										returnValue = (Long) jse.executeScript("return checkMostRecentLoadResult();",
												new Object[] { "nothing" });

										if (returnValue != 0) {
											// There is a problem! Ask the listener to deal with it.
											errorSeen(returnValue);
											break;
										}
										// println(SeleniumConnectionToBrowser.class,
										// waitForTimeoutErrorsDuration + " No webpage-loading timeout seen yet");
									} while (waitForTimeoutErrorsDuration > 0);

									waitForTimeoutErrorsDuration = 0;
									notWaitingToLookForTimeoutErrors = true;
									println(SeleniumConnectionToBrowser.class,
											" Verification of iframe timeout returns [" + returnValue + "]");
								}
							}.start();
						}
					}
					notWaitingToLookForLoadErrors = true;
				}
			}.start();
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
				println(getClass(), "First URL for " + getScreenText() + " is " + url);
				driver.get(url);

				catchSleep(1000);
				// afsd.goToProperSizeAndPlace();

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

				// I have rigged up the border web page to write an error to a hidden <div id="hiddenPlaceForerrors">. But the
				// web page cannot detect if there is an error when loading a URL to an iFrame. Rats.
				@SuppressWarnings("unused")
				Thread watchForErrors = new Thread("WatchForErrorsInWebPage") {
					public void run() {
						while (true) {
							catchSleep(ONE_MINUTE);
							Object r = jse.executeScript("return document.getElementById('hiddenPlaceForErrors').innerHTML;\n",
									new Object[] { "nothing" });
							if (r.toString().toLowerCase().contains("error")) {
								printlnErr(SeleniumConnectionToBrowser.class, " Error detected in iFrame: " + r);
								connected = false;
							}
						}

					}
				};
				// Since this idea does not work, do not start this thread.
				// watchForErrors.start();
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

	// @Override
	public void forceRefresh() {
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
	public String getConnectionCode() {
		return getScreenText();
	}
}
