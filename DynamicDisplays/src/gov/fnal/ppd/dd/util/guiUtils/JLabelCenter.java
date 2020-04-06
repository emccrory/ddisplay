/*
 * JLabelCenter
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.guiUtils;

import java.awt.Color;

import javax.swing.JLabel;

/**
 * Helper class for building the Splash Screen for the channel selector.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class JLabelCenter extends JLabel {

	private static final long	serialVersionUID	= 7194703523029330474L;

	/**
	 * Create a label
	 * 
	 * @param title
	 *            The text on the label
	 * @param size
	 *            The size of the label
	 */
	public JLabelCenter(final String title, final float size) {
		super(title, JLabel.CENTER);

		setAlignmentX(CENTER_ALIGNMENT);
		setFont(getFont().deriveFont(size));
		setForeground(Color.white);
		setBackground(new Color(0.2f, 0.2f, 0.2f, 0.5f));
		setOpaque(true);
	}
}
