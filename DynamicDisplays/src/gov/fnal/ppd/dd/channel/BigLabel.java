package gov.fnal.ppd.dd.channel;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * A utility class used in more than one place.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public class BigLabel extends JLabel {
	private static final long	serialVersionUID	= 8296427549410976741L;

	/**
	 * @param title
	 *            The text on the label
	 * @param style
	 *            the style (BOLD, PLAIN) of the label
	 */
	public BigLabel(final String title, final int style) {
		super(title);
		setOpaque(true);
		setBackground(Color.white);

		setFont(new Font("Arial", style, (SHOW_IN_WINDOW ? 16 : 24)));
		setAlignmentX(JComponent.CENTER_ALIGNMENT);
	}
}