package gov.fnal.ppd.signage.display;

import static gov.fnal.ppd.signage.display.testing.BrowserLauncher.debug;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Figure out the geometry of the screens on this system.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class ScreenLayoutInterpreter {
	private static List<Rectangle>	rects	= new ArrayList<Rectangle>();

	// private static boolean debug = true;

	private ScreenLayoutInterpreter() {
	}

	private static void init() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		int gsNum = 0;
		if (debug)
			System.out.println("Screen/Config\tRect Bounds\tDimensions");
		for (GraphicsDevice curGs : gs) {
			GraphicsConfiguration[] gc = curGs.getConfigurations();
			int gcNum = 0;
			Rectangle lastRectangle = null;
			for (GraphicsConfiguration curGc : gc) {
				Rectangle bounds = curGc.getBounds();
				if (!bounds.equals(lastRectangle))
					rects.add(bounds);
				lastRectangle = bounds;
				if (debug)
					System.out.println(gsNum + "/" + (gcNum++) + ":\t\t(" + bounds.getX() + "," + bounds.getY() + ")\t("
							+ bounds.getWidth() + "x" + bounds.getHeight() + "), rects.size()=" + rects.size());
			}
			++gsNum;
		}
		if (debug)
			System.out.println(Arrays.toString(rects.toArray()));
	}

	/**
	 * @return the number of screens seen
	 */
	public static int getNumScreens() {
		if (rects.size() == 0)
			init();
		return rects.size();
	}

	/**
	 * @param screenNumber
	 *            Which screen number?
	 * @return the bounds of the specified screen number
	 */
	public static Rectangle getBounds(int screenNumber) {
		if (rects.size() == 0)
			init();
		if (screenNumber < 0)
			screenNumber = 0;
		if (screenNumber >= rects.size())
			screenNumber = rects.size() - 1;
		return rects.get(screenNumber);
	}

}
