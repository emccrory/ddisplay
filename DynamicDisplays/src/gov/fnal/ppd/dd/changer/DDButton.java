package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.fixFontSize;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.SystemColor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;

import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;

/**
 * Create a JButton that can be put into a ButtonGroup. represents a Channel for a Display or a Display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2012-16
 */
public class DDButton extends JButton {

	private static final long	serialVersionUID	= 6951625597558149464L;

	private static final int	MAX_STRING_LENGTH	= 14;

	/**
	 * What do we show on the button?
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation 2017
	 * 
	 */
	public enum ButtonFieldToUse {
		/**
		 * Show the default name field on my button
		 */
		USE_NAME_FIELD,
		/**
		 * Show a full description on the button
		 */
		USE_DESCRIPTION_FIELD,
		/**
		 * Show the URL on the button
		 */
		USE_URL_FIELD;

		/**
		 * @return the next value of this enum to become
		 */
		public ButtonFieldToUse next() {
			if (this == DDButton.ButtonFieldToUse.USE_NAME_FIELD)
				return DDButton.ButtonFieldToUse.USE_DESCRIPTION_FIELD;
			else if (this == DDButton.ButtonFieldToUse.USE_DESCRIPTION_FIELD)
				return DDButton.ButtonFieldToUse.USE_URL_FIELD;
			else
				return DDButton.ButtonFieldToUse.USE_NAME_FIELD;
		}
	};

	private static int		staticNumBR			= 0;

	private boolean			selected			= false;

	private Color			background, selectedColor, plainFont = SystemColor.textText, selectedFont = SystemColor.textText;

	private Channel			channel;
	private Display			display;

	private Border			regularButtonBorder;
	private final Border	BorderA, BorderB;

	private int				numBR, maxLen;

	private Font			userDefinedFont		= null, descriptionFont = null, urlFont = null;

	private boolean			notInitialization	= false;

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

		// This does not work properly: addComponentListener(this);

		numBR = staticNumBR;
		this.selected = false;
		this.channel = channel;
		this.display = display;
		this.maxLen = maxLen;

		// Hold the original button border for later
		regularButtonBorder = getBorder();

		if (display == null)
			throw new IllegalArgumentException("Display cannot be null!");
		this.selectedColor = display.getPreferredHighlightColor();

		if (SHOW_IN_WINDOW) {
			BorderA = BorderFactory
					.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(selectedColor, 1),
							BorderFactory.createLineBorder(Color.gray, 1)), regularButtonBorder);
			BorderB = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(selectedColor, 2), regularButtonBorder);
		} else {
			Border c = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black), regularButtonBorder);
			BorderA = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray, 6), c);
			BorderB = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(selectedColor, 6), c);
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
		notInitialization = true;
	}

	/**
	 * @param display
	 */
	public DDButton(final Display display) {
		this(null, display, MAX_STRING_LENGTH);
	}

	@Override
	public void setFont(Font f) {
		if (notInitialization) {
			userDefinedFont = f;
			// new Exception("Ignore").printStackTrace();
		}
		super.setFont(f);
	}

	/**
	 * @return The number of lines that this title should cover (will help determine the font size)
	 */
	public int numLinesInTitle() {
		return numBR;
	}

	private static synchronized String align(String string, int maxLen) {
		staticNumBR = 1;
		if (string.length() < maxLen)
			return string;

		String[] split = string.split(" ");
		String first = "";
		String second = "";
		String third = "";
		int i = 0;
		for (; first.length() < maxLen; i++)
			first += split[i] + " ";
		for (; i < split.length && second.length() < maxLen; i++)
			second += split[i] + " ";
		for (; i < split.length; i++)
			third += split[i] + " ";
		if (third.length() > maxLen)
			third = third.substring(0, maxLen - 1) + "&hellip;"; // Ellipsis

		if (second.length() > 0) {
			// first += "<br />";
			staticNumBR = 2;
		}
		if (third.length() > 0) {
			// second += "<br />";
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

	boolean needsNewFontCalculation = false;

	/**
	 * Change the text on the button
	 * 
	 * @param which
	 *            either USE_NAME_FIELD or USE_DESCRIPTION_FIELD
	 */
	public void setText(final ButtonFieldToUse which) {
		if (channel == null || channel.getDescription().length() < 5 || channel instanceof ChannelImage)
			return;
		if (userDefinedFont == null)
			userDefinedFont = getFont();
		switch (which) {
		case USE_NAME_FIELD:
			// needsNewFontCalculation = true;
			setText(align(channel.getName(), maxLen));
			super.setFont(userDefinedFont);
			break;
		case USE_DESCRIPTION_FIELD:
			// needsNewFontCalculation = true;
			String tx = "<html><b>" + channel.getName() + ":</b> " + channel.getDescription() + " <em>(Channel "
					+ channel.getNumber() + ")</em></html>";
			if (descriptionFont == null) {
				float siz = ((float) userDefinedFont.getSize()) / 2.0f;
				siz = fixFontSize(siz, 7.0f, userDefinedFont.getSize());
				descriptionFont = userDefinedFont.deriveFont(siz);
			}
			setText(tx);
			super.setFont(descriptionFont);

			break;
		case USE_URL_FIELD:
			// needsNewFontCalculation = true;
			tx = channel.getURI().toString();
			tx = tx.replace("http://www.", "").replace("https://www.", "");
			tx = tx.replace("http://", "").replace("https://", "");
			tx = tx.replace("?", " ?").replace("/", " /").replace("=", "= ");
			if (urlFont == null) {
				float factor = 2.0f;
				if (tx.length() > 100)
					factor = 3.0f;
				float siz = fixFontSize(userDefinedFont.getSize() / factor, 8.0f, userDefinedFont.getSize());
				urlFont = userDefinedFont.deriveFont(siz);
			}
			setText("<html>" + tx + "</html>");
			super.setFont(urlFont);
			break;
		}

	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (needsNewFontCalculation) {
			// This does not work yet. Some
			needsNewFontCalculation = false;
			FontMetrics metrics = g.getFontMetrics();
			int adv = metrics.stringWidth(getText());
			int siz = getFont().getSize();
			float nSize = ((float) siz) * (((float) getWidth()) / ((float) adv));
			if (nSize < 8)
				nSize = 8;
			if (nSize > 64)
				nSize = 64;
			System.out.println(hashCode() + " width=" + getWidth() + ", adv=" + adv + ", old siz=" + siz + ", New size = " + nSize);
			super.setFont(getFont().deriveFont(nSize));
		}
	}
}
