package gov.fnal.ppd.dd.display.client.selenium;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.util.TimerTask;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;

/**
 * A thread to keep an eye on the screen size. In principle, this will fix to back to full screen when it varies.
 * 
 * First attempt does not work because of an internal exception generated during the call to driver.manage().window().getPosition() -
 *   <date> org.openqa.selenium.remote.ErrorCodes toStatus
 *   INFO: HTTP Status: '404' -> incorrect JSON status mapping for 'unknown error' (500 expected)
 *   [...]
 *   at org.openqa.selenium.remote.RemoteWebDriver$RemoteWebDriverOptions$RemoteWindow.getPosition(RemoteWebDriver.java:817)
 *   [...]
 *   (And Line 817 is not a valid line number in the library, somehow.)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class CheckAndFixScreenDimensions extends TimerTask {

	private static final boolean	directPositioning	= false;
	private WebDriver				driver;
	private Dimension				screenDimension;
	private Point					screenPosition;

	private int						counter				= 0;
	private boolean					haventShowTraceback	= true;
	private int						numExceptions		= 0;

	/**
	 * @param d
	 *            The WebDriver for this instance
	 * @param p
	 *            The position of the screen to maintain.
	 * @param sd
	 *            The size of the screen that we want to maintain.
	 * 
	 *            (Note that these two arguments are org.openqa.selenium.Dimension and Point, not the normal Java Dimension or Point
	 *            type.)
	 */
	public CheckAndFixScreenDimensions(final WebDriver d, final Point p, final Dimension sd) {
		driver = d;
		screenDimension = sd;
		screenPosition = p;
	}

	public void run() {
		try {
			Point pos = driver.manage().window().getPosition();
			Dimension dim = driver.manage().window().getSize();
			boolean adjusted = false;

			if (!dim.equals(screenDimension)) {
				// The size has changed!
				printlnErr(getClass(), " Screen dimensions have changed from " + screenDimension + " to " + dim);
				adjusted = true;
			}
			if (!pos.equals(screenPosition)) {
				// The window has moved!
				printlnErr(getClass(), "Screen position have changed from " + screenPosition + " to " + pos);
				adjusted = true;
			}

			if (adjusted) {
				if (directPositioning) {
					driver.manage().window().setPosition(screenPosition);
					driver.manage().window().setSize(screenDimension);
				} else {
					driver.findElement(new By.ByName("hiddenButton")).click();
				}
			}

			if (adjusted || counter < 25 || counter % 25 == 0) {
				catchSleep(100);
				println(getClass(), counter + " Window layout - Popsition: " + driver.manage().window().getPosition()
						+ ", Dimensions: " + driver.manage().window().getSize());
			}
		} catch (Exception e) {
			if (numExceptions++ % 30 == 0)
				printlnErr(getClass(),
						"Caught exception (" + numExceptions + ") in trying to read or adjust the position of the screen");
			if (haventShowTraceback) {
				e.printStackTrace();
				haventShowTraceback = false;
			}
		}
		++counter;
	}

}
