/*
 * JLabelFooter
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.guiUtils;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.shortDate;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.truncate;

import java.awt.Dimension;

import javax.swing.JLabel;

/**
 * <p>
 * A single-purpose instance of JLabel to show the status of a display at the bottom of the ChannelSelector screen
 * </p>
 * 
 * <p>
 * Adjusts the text within the label to fit in the provided space.
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class JLabelFooter extends JLabel {

	private static final long	serialVersionUID	= -7328547318315265510L;
	private final static int	DEFAULT_SIZE		= (SHOW_IN_WINDOW ? 30 : 60);

	/**
	 * @param text
	 */
	public JLabelFooter(final String text) {
		super("<html>" + truncate(text, DEFAULT_SIZE) + " (" + shortDate() + ")" + "</html>");
	}

	@Override
	public void setText(String text) {
		Dimension d = getSize();
		if (d.width * d.height == 0) {
			super.setText(text);
			return;
		}
		int numChars = Math.max(-23 + d.width / 7, DEFAULT_SIZE);
		super.setText("<html>" + truncate(text, numChars) + " (" + shortDate() + ")" + "</html>");
	}
}
