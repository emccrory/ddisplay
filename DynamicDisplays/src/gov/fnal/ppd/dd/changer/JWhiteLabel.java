package gov.fnal.ppd.dd.changer;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class JWhiteLabel extends JLabel {

	private static final long	serialVersionUID	= -4821299908662876531L;

	public JWhiteLabel(String text) {
		super("  " + DocentGrid.trunc(text, DocentGrid.MAX_CAPTION_LENGTH) + " ");
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