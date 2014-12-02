package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.util.Util.shortDate;
import static gov.fnal.ppd.dd.util.Util.truncate;

import java.awt.Dimension;

import javax.swing.JLabel;

/**
 * A single-purpose instance of JLabel to show the status of the Display at the bottom of the ChannelSelector screen Adjusts the
 * text within the label to fit in the provided space.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
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
		int numChars = DEFAULT_SIZE;
		if (d.height != 0 && d.width != 0)
			numChars = 3 * d.width / d.height;

		super.setText("<html>" + truncate(text, numChars) + " (" + shortDate() + ")" + "</html>");
	}
}
