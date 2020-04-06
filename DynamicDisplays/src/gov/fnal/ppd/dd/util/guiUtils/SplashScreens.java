/*
 * SplashScreens
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.guiUtils;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.dd.GlobalVariables.bgImage;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationDescription;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationName;
import static gov.fnal.ppd.dd.GlobalVariables.imageHeight;
import static gov.fnal.ppd.dd.GlobalVariables.imageNames;
import static gov.fnal.ppd.dd.GlobalVariables.imageWidth;
import static gov.fnal.ppd.dd.GlobalVariables.offsets;
import static gov.fnal.ppd.dd.GlobalVariables.userHasDoneSomething;
import static gov.fnal.ppd.dd.GlobalVariables.userIsInactive;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Box;
import javax.swing.JPanel;

import gov.fnal.ppd.dd.interfaces.DisplayCardActivator;

/**
 * Utility class for ChannelSelector to create splash screens on the touch-panel displays. It looks nice and might save the screen a
 * little
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class SplashScreens extends Thread {
	private Box[]					splashPanel		= new Box[imageNames.length];
	private DisplayCardActivator	listener;
	private int						activeScreen	= -1;

	/**
	 * @param listener
	 * @param displayChannelPanel
	 */
	public SplashScreens(final DisplayCardActivator listener, final Container displayChannelPanel) {
		this.listener = listener;

		// Create a nice splash screen that it comes back to after a period of inactivity
		SimpleMouseListener splashListener = new SimpleMouseListener(listener);

		// char arrow = '\u21E8';
		float headline = 70, subHead = 30;
		int gap = 25;

		for (int index = 0; index < splashPanel.length; index++) {
			Box splash = splashPanel[index] = Box.createVerticalBox();

			final int indexForThisImage = index;
			
			JPanel p = new JPanel() {

				private static final long serialVersionUID = -2364511327267313957L;

				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);
					// Implement a "fit" algorithm so the image is seen in its entirety, not stretched or cropped.
					int w = getWidth();
					int h = getHeight();
					int x = 0, y = 0;
					try {
						double imgAspect = ((double) imageWidth[indexForThisImage]) / ((double) imageHeight[indexForThisImage]); // Say 16:9 or 1.778
						double scrAspect = ((double) w) / ((double) h); // Say 3:2 or 1.5
						if (imgAspect > scrAspect) {
							// image is wider than the screen: reduce the height of the screen (and it will fill the width)
							h = (int) (w / imgAspect);
							y = (getHeight() - h) / 2;
						} else {
							// screen is wider than the image: Reduce the width of the screen (and it will fill the height)
							w = (int) (h * imgAspect);
							x = (getWidth() - w) / 2;
						}
						// System.out.println(w + "," + h + "," + getWidth() + "," + getHeight() + "," + x + "," + y + "," +
						// imgAspect + "," + scrAspect);

					} catch (Exception e) {
						// ignore what is probably a divide-by-zero exception
					}
					g.drawImage(bgImage[indexForThisImage], x, y, w, h, this); // draw the image
				}
			};
			p.setBackground(Color.black);
			p.setOpaque(true);
			p.addMouseListener(splashListener);
			splash.add(Box.createRigidArea(new Dimension(50, 50)));
			boolean hasCredits = (index % 7 == 0);
			int h = offsets[index];
			if (hasCredits)
				h = 100;
			// System.out.println("Splash screen " + index + " has vertical offset of " + h);
			splash.add(Box.createRigidArea(new Dimension(100, h)));
			splash.add(new JLabelCenter("   Welcome to " + getLocationName() + "!   ", headline));
			splash.add(Box.createRigidArea(new Dimension(50, gap)));
			splash.add(new JLabelCenter("   " + getLocationDescription() + "   ", subHead));
			splash.add(Box.createRigidArea(new Dimension(50, gap)));
			splash.add(new JLabelCenter("<html><em>Touch to continue</em></html>", subHead));
			
			if (hasCredits) {
				splash.add(Box.createRigidArea(new Dimension(50, gap)));
				splash.add(new JLabelCenter("<html><p align='center'>"
						+ "<em>Dynamic Display System software written by Elliott McCrory, Fermilab AD/Instrumentation<br>"
						+ "<br>All pictures are by Fermilab photographers</em></p></html>", 12));
			}
			// splash.add(new JLabelCenter("" + arrow, arrowSize));

			// splash.setOpaque(true);
			// splash.setBackground(new Color(200 + (int) (Math.random() * 40.0), 200 + (int) (Math.random() * 40.0),
			// 200 + (int) (Math.random() * 40.0)));
			p.add(splash);
			displayChannelPanel.add(p, "Splash Screen" + index);
		}
	}

	/**
	 * @return the number of splash screen images we have
	 */
	public int getLength() {
		return splashPanel.length;
	}

	public void run() {
		while (true) {
			if (userIsInactive()) {
				listener.activateCard(false);
				userHasDoneSomething();
			}
			catchSleep(ONE_SECOND);
		}
	}

	/**
	 * @return the name of the next splash screen
	 */
	public String getNext() {
		activeScreen = (activeScreen + 1) % splashPanel.length;
		return "Splash Screen" + activeScreen;
	}

}
