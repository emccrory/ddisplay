/*
 * SplashScreens
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.guiUtils;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationDescription;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationName;
import static gov.fnal.ppd.dd.GlobalVariables.userHasDoneSomething;
import static gov.fnal.ppd.dd.GlobalVariables.userIsInactive;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.ImageIcon;
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
	 * The names of the image files used in the Channel Selector's screen saver.
	 * 
	 * This was last update in 2019. It would look best if these images were update from time to time. The location of these images
	 * is expected to be in the package gov.fnal.ppd.dd.images.
	 */
	private final static String[]	imageNames		= { "fermilab3.jpg", "fermilab1.jpg", "fermilab2.jpg", "fermilab4.jpg",
			"fermilab5.jpg", "fermilab6.jpg", "fermilab7.jpg", "fermilab8.jpg", "fermilab9.jpg", "fermilab10.jpg", "fermilab11.jpg",
			"fermilab12.jpg", "fermilab13.jpg", "fermilab14.jpg", "fermilab15.jpg", "fermilab16.jpg", "fermilab17.jpg",
			"fermilab18.jpg", "fermilab19.jpg", "fermilab20.jpg", "fermilab21.jpg", "Baginski_1319.jpg", "Baginski_1649.jpg",
			"Biron_9077.jpg", "Brown_5472.jpg", "Chapman_1066.jpg", "Chapman_2860.jpg", "Chapman_3859.jpg", "Chapman_4721.jpg",
			"Chapman_7196.jpg", "Chapman_8814.jpg", "Dyer_3235.jpg", "Ferguson_2539.jpg", "Flores_9904.jpg", "Hahn_4109.jpg",
			"Higgins_7961.jpg", "Iraci_6742.jpg", "Kroc_9559.jpg", "Limberg_9772.jpg", "McCrory_2555.jpg", "McCrory_3368.jpg",
			"McCrory_3902.jpg", "McCrory_7002.jpg", "McCrory_8865.jpg", "Murphy_2507.jpg", "Murphy_3460.jpg", "Murphy_4658.jpg",
			"Nicol_1322.jpg", "Nicol_2066.jpg", "Nicol_7967.jpg", "Olsen_6219.jpg", "Paterno_6012.jpg", "Pygott_6098.jpg",
			"Robertson_7776.jpg", "Santucci_3708.jpg", "Schwender_7961.jpg", "Scroggins_1293.jpg", "Scroggins_9472.jpg",
			"Shaddix_1745.jpg", "Shaddix_2506.jpg", "Shaddix_3843.jpg", "Shaddix_5138.jpg", "american-lotus-2.jpg",
			"auditorium-stairs.jpg", "behind-tall-grass.jpg", "bright-tevatron-tunnel.jpg", "coyote-wilson-hall.jpg",
			"egret-spots.jpg", "feynman-fountain.jpg", "iarc-angles.jpg", "july-fourth-coyote.jpg",
			"lightning-storm-over-fermilab.jpg", "main-ring-magnets.jpg", "pom-pom.jpg", "ramsey-wilson.jpg",
			"sunset-at-caseys-pond.jpg", "water-moon-venus.jpg", "wilson-hall-moon-airplane.jpg", "yellow-flowers.jpg",
			"yellowwildflowers.jpg", };

	/** The images corresponding to the image names specified above */
	private final static Image[]	bgImage			= new Image[imageNames.length];
	/** How wide is each image, nominally? */
	private final static int[]		imageWidth		= new int[imageNames.length];
	/** how tall is each image, nominally? */
	private final static int[]		imageHeight		= new int[imageNames.length];
	/** The offset from the top of the screen to put the text */
	private final static int[]		offsets			= new int[imageNames.length];

	/**
	 * Must be called by the ChannelSelector prior to startup! This was simply a "static" block, but this is entirely unnecessary
	 * for the Displays
	 */
	public final static void prepareSaverImages() {
		List<String> a = Arrays.asList(imageNames);
		Collections.shuffle(a); // Randomize the presentation
		int i = 0;
		for (String name : a) {
			ImageIcon icon = new ImageIcon("bin/gov/fnal/ppd/dd/images/" + name);
			bgImage[i] = icon.getImage();
			imageWidth[i] = icon.getIconWidth();
			imageHeight[i] = icon.getIconHeight();
			offsets[i] = (int) (50.0 + 150.0 * Math.random());
			i++;
		}
	}

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
						double imgAspect = ((double) imageWidth[indexForThisImage]) / ((double) imageHeight[indexForThisImage]); // Say
																																	// 16:9
																																	// or
																																	// 1.778
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
