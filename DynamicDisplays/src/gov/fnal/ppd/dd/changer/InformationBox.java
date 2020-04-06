/*
 * InformationBox
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.util.GeneralUtilities.catchSleep;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.Border;

/**
 * Creates a borderless pop-up window. Used by {@link gov.fnal.ppd.dd.ChannelSelector}, and others, to let the user know that
 * something is happening.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class InformationBox extends JFrame {

	private static final long	serialVersionUID	= 4866012215771895245L;

	/**
	 * A simple extension of JLabel to assist in the proper setup of the fonts and stuff
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	private static class MyLabel extends JLabel {

		private static final long	serialVersionUID	= 5368553680158650696L;

		MyLabel(String text, float size) {
			super(text, JLabel.CENTER);
			setFont(getFont().deriveFont(size));
			setAlignmentX(CENTER_ALIGNMENT);
		}
	}

	/**
	 * 
	 * @param message1
	 *            The headline message
	 * @param message2
	 *            The sub message
	 */
	public InformationBox(final String message1, final String message2) {
		this(1.0F, message1, message2);
	}

	/**
	 * 
	 * @param scale
	 *            Default: 1.0f
	 * @param message1
	 *            The headline message
	 * @param message2
	 *            The sub message
	 */
	public InformationBox(final float scale, final String message1, final String message2) {
		super("URL Refresh Action");
		setup(scale, message1, message2);
	}
	
	private synchronized void setup(final float scale, final String message1, final String message2) {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Box h = Box.createVerticalBox();
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		h.add(new MyLabel("  ***** " + message1.toUpperCase() + " *****  ", scale * 30.0f));
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		h.add(new MyLabel(message2, scale * 24.0f));
		h.add(Box.createRigidArea(new Dimension(10, 10)));

		final MyLabel timeLeft = new MyLabel("This will disappear in 5 seconds", 10.0f);
		h.add(timeLeft);
		h.add(Box.createRigidArea(new Dimension(10, 10)));

		Border b0 = BorderFactory.createLineBorder(Color.black);
		Border b1 = BorderFactory.createLineBorder(Color.white, 15);
		Border b2 = BorderFactory.createLineBorder(Color.black, 20);
		Border b3 = BorderFactory.createEmptyBorder(50, 50, 50, 50);
		Border b4 = BorderFactory.createCompoundBorder(b0, b1);
		Border b5 = BorderFactory.createCompoundBorder(b4, b2);
		h.setBorder(BorderFactory.createCompoundBorder(b5, b3));

		setContentPane(h);
		setUndecorated(!SHOW_IN_WINDOW);
		pack();

		setVisible(true);
		setAlwaysOnTop(true);

		new Thread(this.getClass().getSimpleName() + "_Removal") {
			public void run() {
				for (int i = 5; i > 0; i--) {
					timeLeft.setText("This will disappear in " + i + " seconds");
					catchSleep(1000L);
				}
				InformationBox.this.dispose();
			}
		}.start();
	}
}
