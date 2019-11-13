/*
 * TemporaryDialogBox
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.util.Util.catchSleep;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.Border;

import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.xml.ChannelSpec;

/**
 * Creates a borderless pop-up window. Used by {@link gov.fnal.ppd.dd.ChannelSelector}, and others, to let the user know that
 * something is happening. Disappears after a short interval.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class TemporaryDialogBox extends JFrame {

	private static final long serialVersionUID = 526884696647205539L;

	/**
	 * A simple extension of JLabel to assist in the proper setup of the fonts and stuff
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	private static class MyLabel extends JLabel {
		private static final long serialVersionUID = -401307693591421751L;

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

	boolean		dismissNow	= false;
	static int	cc			= 0;

	/**
	 * 
	 * @param comp
	 *            The parent component for this dialog box
	 * @param di
	 *            The Display for which we are showing the dialog box
	 * @param imBusy 
	 */
	public TemporaryDialogBox(final JComponent comp, final Display di) {
		super(cc++ + " Display " + di + " changed successfully");

		dismissNow = false;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		float brightness = (Color.RGBtoHSB(di.getPreferredHighlightColor().getRed(), di.getPreferredHighlightColor().getGreen(),
				di.getPreferredHighlightColor().getBlue(), null))[2];
		Color fontColor = Color.black;
		if (brightness < 0.6f) {
			fontColor = Color.white;
		}

		String urlString = "";
		if (di.getContent() instanceof ChannelSpec) {
			urlString = ((ChannelSpec) di.getContent()).getURI().toString();
		} else if (di.getContent() instanceof Channel) {
			urlString = ((Channel) di.getContent()).getURI().toString();
		}
		Box h = Box.createVerticalBox();
		Color bg = di.getPreferredHighlightColor();
		Color fg = fontColor;
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		int time = 6;
		Color borderColor1 = Color.BLACK;
		Color borderColor2 = Color.BLACK;

		float big = 30.0f, regular = 20.0f, small = 12.0f;
		if (SHOW_IN_WINDOW) {
			big = 20.0f;
			regular = 15.0f;
			small = 10.0f;
		}

		// if (urlString.startsWith("https://")) {
        int code = 0;

        if (di.getContent() instanceof Channel)
                code = ((Channel) di.getContent()).getCode();
        else if (di.getContent() instanceof ChannelSpec)
                code = ((ChannelSpec) di.getContent()).getCode();
        if (code == 2) {	
			h.add(new MyLabel(fg, bg, "  ****  N O N - S E C U R E   C O N T E N T  **** ", big * 4 / 3));
			h.add(Box.createRigidArea(new Dimension(10, 10)));
			h.add(new MyLabel(fg, bg, " The content on " + di + " (" + di.getLocation() + ") "), regular);
			h.add(Box.createRigidArea(new Dimension(10, 10)));
			h.add(new MyLabel(fg, bg, " MIGHT NOT HAVE BEEN CHANGED to '" + breakup(di.getContent())
					+ "' because of a security incompatibility (hhtp versus https)"), regular);
			h.add(Box.createRigidArea(new Dimension(15, 15)));
			h.add(new MyLabel(fg, bg,
					"The web is moving to 'https everywhere'.  During this transition, some old servers "
							+ "are still hosting non-secure content.  The Dynamic Display system "
							+ "cannot show this content reliably until it is converted to https.",
					small));
			h.add(Box.createRigidArea(new Dimension(5, 5)));
			h.add(new MyLabel(fg, bg, "This is being enforced by the Firefox browser on the display", small));
			time = 30;
			borderColor1 = Color.WHITE;
			borderColor2 = Color.RED;
		} else {
			h.add(new MyLabel(fg, bg, " - - - - - - Channel Changed - - - - - - ", big));
			h.add(Box.createRigidArea(new Dimension(10, 10)));
			h.add(new MyLabel(fg, bg, " The content on " + di + " (" + di.getLocation() + ") "), regular);
			h.add(Box.createRigidArea(new Dimension(10, 10)));
			h.add(new MyLabel(fg, bg, "<html> Has been changed to " + breakup(di.getContent()) + " </html>"), regular);
		}
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		h.add(new MyLabel(fg, bg, " The URL is '" + urlString + "' ", (urlString.length() < 50 ? regular : small)));
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		final MyLabel timeLeft = new MyLabel(fg, bg, "This notification disappear automatically in " + time + " seconds", small);
		h.add(timeLeft);
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		JButton dismissNowButton = new JButton("Dismiss now");
		dismissNowButton.setAlignmentX(CENTER_ALIGNMENT);

		if (!SHOW_IN_WINDOW)
			dismissNowButton.setFont(dismissNowButton.getFont().deriveFont(regular));
		h.add(dismissNowButton);
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		dismissNowButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				timeLeft.setText("This noticication will disappear now");
				dismissNow = true;
			}
		});

		h.setOpaque(true);
		h.setBackground(di.getPreferredHighlightColor());
		Border b0 = BorderFactory.createRaisedSoftBevelBorder();
		Border b1 = BorderFactory.createLoweredSoftBevelBorder();
		Border b2a = BorderFactory.createLineBorder(borderColor1, 6);
		Border b2b = BorderFactory.createLineBorder(borderColor2, 2);
		Border b3 = BorderFactory.createEmptyBorder(50, 50, 50, 50);
		Border b4a = BorderFactory.createCompoundBorder(b0, b2b);
		Border b4b = BorderFactory.createCompoundBorder(b4a, b2a);
		Border b4c = BorderFactory.createCompoundBorder(b4b, b2b);
		Border b5 = BorderFactory.createCompoundBorder(b4c, b1);
		h.setBorder(BorderFactory.createCompoundBorder(b5, b3));

		setContentPane(h);
		setUndecorated(true);
		pack();
		PointerInfo p = MouseInfo.getPointerInfo();
		Point pointerLocation = p.getLocation();
		setLocation(pointerLocation.x - 100, pointerLocation.y - 100);

		setVisible(true);
		setAlwaysOnTop(true);

		final int tt = time;
		new Thread(this.getClass().getSimpleName() + "_Removal") {
			public void run() {
				for (int i = tt; i > 0 && !dismissNow; i--) {
					if (i > 1) {
						timeLeft.setText("This notification will disappear automatically in " + i + " seconds");
					} else {
						timeLeft.setText("This notification will disappear now");
					}
					catchSleep(1000L);
				}

				end(TemporaryDialogBox.this);
			}
		}.start();
	}

	protected static void end(TemporaryDialogBox tdb) {
		tdb.setVisible(false);
		tdb.dispose();
		tdb = null;
	}

	private static String breakup(SignageContent content) {
		if (content instanceof ChannelPlayList) {
			return content.toString().replace(") (", ")<br>(").replace("{", "{<br>").replace("}", "<br>}");
		}
		return "'" + content.toString() + "'";
	}

	/**
	 * 
	 * @param comp
	 *            The parent component for this dialog box
	 * @param di
	 *            The Display for which we are showing the dialog box
	 */
	public TemporaryDialogBox(final JComponent comp, final String title, final int dwell, final String headline, final String line2,
			final String line3) {
		super(title);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Color fontColor = Color.black;
		Color bg = Color.WHITE;
		Color fg = fontColor;

		Box h = Box.createVerticalBox();
		h.setOpaque(true);
		h.setBackground(bg);
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		h.add(new MyLabel(fg, bg, headline, 36.0f));
		h.add(Box.createRigidArea(new Dimension(30, 30)));
		h.add(new MyLabel(fg, bg, line2, 16.0f));
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		h.add(new MyLabel(fg, bg, line3, 14.0f));
		h.add(Box.createRigidArea(new Dimension(30, 30)));
		final MyLabel timeLeft = new MyLabel(fg, bg, "This message will disappear in " + dwell + " seconds", 10.0f);
		h.add(timeLeft);
		h.add(Box.createRigidArea(new Dimension(10, 10)));

		Border b0 = BorderFactory.createRaisedSoftBevelBorder();
		Border b1 = BorderFactory.createLoweredSoftBevelBorder();
		Border b2 = BorderFactory.createLineBorder(new Color(63, 156, 135), 10);
		Border ba2 = BorderFactory.createLineBorder(new Color(128, 91, 78), 5);
		Border b3 = BorderFactory.createEmptyBorder(50, 50, 50, 50);
		// Border b3a = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		Border b4 = BorderFactory.createCompoundBorder(b0, b2);
		Border b5 = BorderFactory.createCompoundBorder(b4, b1);
		Border b5a = BorderFactory.createCompoundBorder(ba2, b5);
		Border b5b = BorderFactory.createCompoundBorder(b3, b5a);
		h.setBorder(BorderFactory.createCompoundBorder(b5b, b3));

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
				for (int i = dwell; i > 0; i--) {
					timeLeft.setText("This will disappear in " + i + " seconds");
					catchSleep(1000L);
				}

				end(TemporaryDialogBox.this);
			}
		}.start();
	}
}
