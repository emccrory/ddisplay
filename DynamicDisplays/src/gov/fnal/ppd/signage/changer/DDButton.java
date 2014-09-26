package gov.fnal.ppd.signage.changer;

import static gov.fnal.ppd.GlobalVariables.SHOW_IN_WINDOW;
import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.Display;

import java.awt.Color;
import java.awt.SystemColor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;

/**
 * Create a JButton that can be put into a ButtonGroup. represents a Channel for a Display or a Display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2012
 */
public class DDButton extends JButton {

	private static final long	serialVersionUID	= 6951625597558149464L;

	private static final int	MAX_STRING_LENGTH	= 14;

	private boolean				selected			= false;

	private Color				background, selectedColor, plainFont = SystemColor.textText, selectedFont = SystemColor.textText;

	private Channel				channel;

	private Display				display;

	private Border				regularButtonBorder;

	private final Border		BorderA, BorderB;

	private int					numBR;
	private static int			staticNumBR			= 0;

	/**
	 * @param channel
	 *            (may be null) The Channel that this button represents
	 * @param display
	 *            (may NOT be null) the Display that this button is associated with. If channel is not null, this is the Display
	 *            that the channel is associated with. If channel IS null, this is the actual "select this display" button
	 * @param maxLen
	 */
	public DDButton(final Channel channel, final Display display, int maxLen) {
		super(align(channel != null ? channel.getName() : display.toString(), maxLen));
		numBR = staticNumBR;
		this.selected = false;
		this.channel = channel;
		this.display = display;
		// Hold the original button border for later
		regularButtonBorder = getBorder();

		if (display == null)
			throw new IllegalArgumentException("Display cannot be null!");
		this.selectedColor = display.getPreferredHighlightColor();

		if (SHOW_IN_WINDOW) {
			BorderA = BorderFactory.createCompoundBorder(
					BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(selectedColor, 3),
							BorderFactory.createLineBorder(Color.gray, 2)), regularButtonBorder);
			BorderB = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(selectedColor, 5), regularButtonBorder);
		} else {
			Border c = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black), regularButtonBorder);
			BorderA = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray, 8), c);
			BorderB = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(selectedColor, 8), c);
		}

		float brightness = (Color.RGBtoHSB(selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue(), null))[2];
		if (brightness < 0.6f) {
			selectedFont = new Color(0xdddddd);
		}

		// setToolTipText(toolTip);

		// Hold the original button background for later
		background = getBackground();

		setEnabled(false);
		setOpaque(true);
	}

	/**
	 * @param display
	 */
	public DDButton(final Display display) {
		this(null, display, MAX_STRING_LENGTH);
	}

	/**
	 * @return The number of lines that this title should cover (will help determine the font size)
	 */
	public int numLinesInTitle() {
		return numBR;
	}

	private static String align(String string, int maxLen) {
		if (string.length() < maxLen)
			return string;
		String[] split = string.split(" ");
		String first = "";
		String second = "";
		String third = "";
		int i = 0;
		for (; (first + split[i]).length() < maxLen; i++)
			first += split[i] + " ";
		for (; i < split.length && (second + split[i]).length() < maxLen; i++)
			second += split[i] + " ";
		for (; i < split.length; i++)
			third += split[i] + " ";
		if (third.length() > maxLen)
			third = third.substring(0, maxLen - 1) + "&hellip;"; // Ellipsis
		staticNumBR = 1;
		if (second.length() > 0) {
			first += "<br />";
			staticNumBR = 2;
		}
		if (third.length() > 0) {
			second += "<br />";
			staticNumBR = 3;
		}

		return "<html><center>" + first + second + third + "</center></html>";
	}

	@Override
	public boolean isSelected() {
		return this.selected;
	}

	@Override
	public void setSelected(final boolean selected) {
		super.setSelected(selected);

		this.selected = selected;
		setBackground(selected ? selectedColor : background);

		if (selected)
			super.setForeground(selectedFont);
		else
			super.setForeground(plainFont);
	}

	/**
	 * @return The channel represented by this button
	 */
	public Channel getChannel() {
		return channel;
	}

	/**
	 * For the refresh operation, change the actual channel attached to this button.
	 * 
	 * @param channelNew
	 */
	public void setChannel(Channel channelNew) {
		if (channelNew == null)
			return;
		channel = channelNew;
		setText(channel.getName());
	}

	/**
	 * @return The Display represented by this button
	 */
	public Display getDisplay() {
		return display;
	}

	/*
	 * Overrides setEnabled to handle HTML graying out (which does not happen by default)
	 */
	@Override
	public void setEnabled(boolean vEnabled) {
		super.setEnabled(vEnabled);
		if (display != null && !vEnabled)
			setBorder(BorderA);
		else
			setBorder(BorderB);

	}
}
