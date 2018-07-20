package gov.fnal.ppd.dd.display.client.selenium;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.util.TimerTask;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;

import gov.fnal.ppd.dd.util.PropertiesFile;
import gov.fnal.ppd.dd.util.PropertiesFile.PositioningMethod;

/**
 * A thread to keep an eye on the screen size. In principle, this will fix to back to full screen when it varies.
 * 
 * First attempt does not work because of an internal exception generated during the call to driver.manage().window().getPosition()
 * - <date> org.openqa.selenium.remote.ErrorCodes toStatus INFO: HTTP Status: '404' -> incorrect JSON status mapping for 'unknown
 * error' (500 expected) [...] at
 * org.openqa.selenium.remote.RemoteWebDriver$RemoteWebDriverOptions$RemoteWindow.getPosition(RemoteWebDriver.java:817) [...] (And
 * Line 817 is not a valid line number in the library, somehow.)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class CheckAndFixScreenDimensions extends TimerTask {

	private static final PositioningMethod	positioningMethod		= PropertiesFile.getPositioningMethod();
	private final WebDriver					driver;
	private final Dimension					screenDimension;
	private final Point						screenPosition;

	private int								counter					= 0;
	private boolean							haventShowTraceback1	= true;
	private boolean							haventShowTraceback2	= true;
	private int								numExceptions			= 0;

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
		println(getClass(), " - Proper position and size of this screen is " + screenPosition + " and " + screenDimension);
	}

	public void run() {
		boolean adjusted = false;
		try {
			Point pos = driver.manage().window().getPosition();
			Dimension dim = driver.manage().window().getSize();

			if (!dim.equals(screenDimension)) {
				// The size has changed!
				printlnErr(getClass(), "Screen dimensions have changed from " + screenDimension + " to " + dim);
				adjusted = true;
			}
			if (!pos.equals(screenPosition)) {
				// The window has moved!
				printlnErr(getClass(), "Screen position have changed from " + screenPosition + " to " + pos);
				adjusted = true;
			}
		} catch (Exception e) {
			numExceptions++;
			printlnErr(getClass(),
					"Caught exception (#" + numExceptions + "/" + counter + ") in trying to read the position/size of the screen");
			if (haventShowTraceback1) {
				e.printStackTrace();
				haventShowTraceback1 = false;
			}
		}
		try {
			if (adjusted) {
				catchSleep(100);
				switch (positioningMethod) {
				case DirectPositioning:
					driver.manage().window().setPosition(screenPosition);
					driver.manage().window().setSize(screenDimension);
					break;
				case UseHiddenButton:
					// driver.manage().window().setPosition(screenPosition);
					driver.findElement(new By.ByName("hiddenButton")).click();
					break;
				case ChangeIframe:
					// TODO
					break;
				case DoNothing:
					break;
				}
				catchSleep(100);
				println(getClass(), counter + " Window layout - Position: " + driver.manage().window().getPosition()
						+ ", Dimensions: " + driver.manage().window().getSize());
			}
		} catch (Exception e) {
			numExceptions++;
			printlnErr(getClass(),
					"Caught exception (#" + numExceptions + "/" + counter + ") in trying to adjust the position of the screen");
			if (haventShowTraceback2) {
				e.printStackTrace();
				haventShowTraceback2 = false;
			}
		}
		++counter;
	}

	/**
	 * @param driver
	 */
	public static void goToProperSizeAndPlace(final WebDriver driver) {

		switch (positioningMethod) {
		case DirectPositioning:
			// driver.manage().window().setPosition(screenPosition);
			// driver.manage().window().setSize(screenDimension);
			// break;
			throw new RuntimeException("Not implemented!");

		case UseHiddenButton:
			// driver.manage().window().setPosition(screenPosition);
			driver.findElement(new By.ByName("hiddenButton")).click();
			break;
		case ChangeIframe:
			// TODO
			break;
		case DoNothing:
			break;
		}
		catchSleep(100);
		println(CheckAndFixScreenDimensions.class, "Window layout - Position: " + driver.manage().window().getPosition()
				+ ", Dimensions: " + driver.manage().window().getSize());

	}

}
