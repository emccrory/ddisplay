package gov.fnal.ppd.signage.changer;

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

	private Color				background, selectedColor;

	private Channel				channel;

	private Display				display;

	private Border				regularButtonBorder;

	private final Border		BorderA, BorderB;

	/**
	 * @param channel
	 *            (may be null) The Channel that this button represents
	 * @param display
	 *            (may NOT be null) the Display that this button is associated with. If channel is not null, this is the Display
	 *            that the channel is associated with. If channel IS null, this is the actual "select this display" button
	 */
	public DDButton(final Channel channel, final Display display) {
		super(align(channel != null ? channel.getName() : display.toString()));
		this.selected = false;
		this.channel = channel;
		this.display = display;

		if (display == null)
			throw new IllegalArgumentException("Display cannot be null!");
		this.selectedColor = display.getPreferredHighlightColor();

		// There is no need to create these every time we get new status on this button--once should be enough.
		BorderA = BorderFactory.createCompoundBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(selectedColor, 1),
						BorderFactory.createLineBorder(Color.gray, 2)), regularButtonBorder);
		BorderB = BorderFactory
				.createCompoundBorder(BorderFactory.createLineBorder(selectedColor, 3), regularButtonBorder);

		// It looks like this is not needed -- it is rewritten later
		// String toolTip = "<html>";
		// if (channel != null) {
		// toolTip += "<p><b>Channel:</b> " + this.channel.getNumber();
		// toolTip += "<br /><b>Name:</b> " + this.channel.getName();
		// toolTip += "<br /><b>Description:</b> '" + this.channel.getDescription() + "'";
		// toolTip += "<br />Press to change to this channel";
		// } else if (display != null) {
		// toolTip += "<b>Display</b> " + display.getNumber() + " -- " + display.getDescription();
		// toolTip += "<br /><b>Last status update:</b> <i>Never</i>";
		// toolTip += "<br />Press to select the displayed channel on this screen.";
		// }
		// toolTip += "</p></html>";
		// setToolTipText(toolTip);

		// Hold the original button background for later
		background = getBackground();
		// Hold the original button border for later
		regularButtonBorder = getBorder();

		setEnabled(false);
		setOpaque(true);
	}

	/**
	 * @param knownToBeAlive
	 */
	public void setMyBorder(final boolean knownToBeAlive) {
		if (display != null && !knownToBeAlive)
			setBorder(BorderA);
		else
			setBorder(BorderB);
	}

	private static String align(String string) {
		if (string.length() < MAX_STRING_LENGTH)
			return string;
		String[] split = string.split(" ");
		String first = "";
		String second = "";
		int i = 0;
		for (; (first + split[i]).length() < MAX_STRING_LENGTH; i++)
			first += split[i] + " ";
		for (; i < split.length; i++)
			second += split[i] + " ";
		if (second.length() > MAX_STRING_LENGTH)
			second = second.substring(0, MAX_STRING_LENGTH - 1) + "&hellip;"; // Ellipsis
		return "<html><center>" + first + "<br />" + second + "</center></html>";
	}

	/**
	 * @param display
	 */
	public DDButton(final Display display) {
		this(null, display);
	}

	@Override
	public boolean isSelected() {
		return this.selected;
	}

	@Override
	public void setSelected(final boolean selected) {
		this.selected = selected;
		setBackground(selected ? selectedColor : background);
		super.setSelected(selected);
	}

	/**
	 * @return The channel represented by this button
	 */
	public Channel getChannel() {
		return channel;
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
	public void setEnabled(boolean vEnabled) {
		super.setEnabled(vEnabled);
		super.setForeground(vEnabled ? SystemColor.textText : SystemColor.textInactiveText);
		setMyBorder(vEnabled);
	}
}
