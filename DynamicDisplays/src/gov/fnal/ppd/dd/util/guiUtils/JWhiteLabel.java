package gov.fnal.ppd.dd.util.guiUtils;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.truncate;

import javax.swing.JLabel;
import javax.swing.JPanel;

import gov.fnal.ppd.dd.changer.DocentGrid;

/**
 * <p>This class is used when creating an array of buttons in the ChannelSelector.  The text is truncated and centered.</p>
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014-21
 *
 */
public class JWhiteLabel extends JLabel {

	private static final long	serialVersionUID	= -4821299908662876531L;

	public JWhiteLabel(String text) {
		super("  " + truncate(text, DocentGrid.MAX_CAPTION_LENGTH) + " ");
		setOpaque(true);
		setAlignmentX(JPanel.CENTER_ALIGNMENT);
	}

	public JWhiteLabel(String text, float size) {
		this(text);
		setFont(getFont().deriveFont(size));
		setAlignmentX(CENTER_ALIGNMENT);
	}
	
	public JWhiteLabel(String text, int style, float size) {
		this(text);
		setFont(getFont().deriveFont(style, size));
		setAlignmentX(CENTER_ALIGNMENT);
	}
}