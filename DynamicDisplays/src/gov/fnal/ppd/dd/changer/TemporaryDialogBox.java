/*
 * TemporaryDialogBox
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.Border;

/**
 * Creates a borderless pop-up window. Used by {@link gov.fnal.ppd.dd.ChannelSelector}, and others, to let the user know that
 * something is happening.  Disappears after a short interval.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class TemporaryDialogBox extends JFrame {

	private static final long	serialVersionUID	= 526884696647205539L;

	/**
	 * A simple extension of JLabel to assist in the proper setup of the fonts and stuff
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	private static class MyLabel extends JLabel {
		private static final long	serialVersionUID	= -401307693591421751L;

		MyLabel(Color fg, Color bg, String text, float size) {
			super(text, JLabel.CENTER);
			setFont(getFont().deriveFont(size));
			setBackground(bg);
			setOpaque(true);
			setForeground(fg);
			setAlignmentX(CENTER_ALIGNMENT);
		}

		MyLabel(Color fg, Color bg, String text) {
			this(fg, bg, text, 20.0f);
		}
	}

	/**
	 * 
	 * @param comp
	 *            The parent component for this dialog box
	 * @param di
	 *            The Display for which we are showing the dialog box
	 */
	public TemporaryDialogBox(final JComponent comp, final Display di) {
		super("Display " + di + " changed successfully");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		float brightness = (Color.RGBtoHSB(di.getPreferredHighlightColor().getRed(), di.getPreferredHighlightColor().getGreen(), di
				.getPreferredHighlightColor().getBlue(), null))[2];
		Color fontColor = Color.black;
		if (brightness < 0.6f) {
			fontColor = Color.white;
		}

		Color bg = di.getPreferredHighlightColor();
		Color fg = fontColor;
		Box h = Box.createVerticalBox();
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		h.add(new MyLabel(fg, bg, "  ***** CHANNEL CHANGED *****  ", 30.0f));
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		h.add(new MyLabel(fg, bg, " The content on " + di + " (" + di.getLocation() + ") "));
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		h.add(new MyLabel(fg, bg, " Has been changed to '" + di.getContent() + "' "));
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		if (di.getContent() instanceof Channel) {
			h.add(new MyLabel(fg, bg, " The URL is '" + ((Channel) di.getContent()).getURI() + "' ", 15.0f));
			h.add(Box.createRigidArea(new Dimension(10, 10)));
		}
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		final MyLabel timeLeft = new MyLabel(fg, bg, "This will disappear in 5 seconds", 10.0f);
		h.add(timeLeft);
		h.add(Box.createRigidArea(new Dimension(10, 10)));

		h.setOpaque(true);
		h.setBackground(di.getPreferredHighlightColor());
		Border b0 = BorderFactory.createRaisedSoftBevelBorder();
		Border b1 = BorderFactory.createLoweredSoftBevelBorder();
		Border b2 = BorderFactory.createLineBorder(Color.black, 10);
		Border b3 = BorderFactory.createEmptyBorder(50, 50, 50, 50);
		Border b4 = BorderFactory.createCompoundBorder(b0, b2);
		Border b5 = BorderFactory.createCompoundBorder(b4, b1);
		h.setBorder(BorderFactory.createCompoundBorder(b5, b3));

		setContentPane(h);
		setUndecorated(true);
		pack();
		PointerInfo p = MouseInfo.getPointerInfo();
		Point pointerLocation = p.getLocation();
		setLocation(pointerLocation.x - 100, pointerLocation.y - 100);

		setVisible(true);
		setAlwaysOnTop(true);

		new Thread(this.getClass().getSimpleName() + "_Removal") {
			public void run() {
				for (int i = 5; i > 0; i--) {
					timeLeft.setText("This will disappear in " + i + " seconds");
					catchSleep(1000L);
				}

				TemporaryDialogBox.this.dispose();
			}
		}.start();
	}
}
