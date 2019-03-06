package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.db.ChannelsFromDatabase;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.util.DisplayButtonGroup;

/**
 * Create a button grid that contains all of the channels for this Display in the SimplifiedChannelChoice table.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class SimplifiedChannelGrid extends ChannelButtonGrid {

	private static final long	serialVersionUID	= 7871591178431365832L;
	private JComponent			myComponent;

	public SimplifiedChannelGrid(Display display, DisplayButtonGroup bg) {
		super(display, bg);
	}

	@Override
	public void makeGrid(ChannelCategory cat) {
		myComponent = makeGrid();
		if (myComponent != null)
			add(myComponent);
	}

	public boolean hasSpecialChannels() {
		return myComponent != null;
	}

	private static String trunc(String text, int maxLen) {
		if (text.length() < maxLen)
			return text;
		return text.substring(0, maxLen - 2) + " ...";
	}

	private JComponent makeGrid() {
		Set<SignageContent> list = getMySpecialChannels();
		if (list == null) {
			return null;
		}

		int cols = 1;
		int maxLen = 120;
		int hGap = 5;
		int vGap = 5;

		float FS = FONT_SIZE;

		if (list.size() > 6) {
			throw new RuntimeException("The number of 'special' channels for display " + display.getDBDisplayNumber()
					+ " is too big! " + list.size() + " Maximum is 6");
		}
		switch (list.size()) {
		case 6:
			break;

		case 5:
			FS = 1.05f * FONT_SIZE;
			vGap = 8;
			break;

		case 4:
			FS = 1.1f * FONT_SIZE;
			vGap = 10;
			break;

		default:
			FS = 1.2f * FONT_SIZE;
			maxLen = 100;
			vGap = 20;
			break;
		}


		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.insets = new Insets(vGap, hGap, vGap, hGap);
		gbc.weightx = 1;
		gbc.weighty = 1;

		expGrid = new JPanel(new GridBagLayout());
		expGrid.setOpaque(true);
		expGrid.setBackground(display.getPreferredHighlightColor());
		int numButtons = 0;
		Box pictureBox = Box.createHorizontalBox();
		for (SignageContent contentInstance : list) {
			numButtons++;
			if (contentInstance instanceof ChannelImage) {
				ChannelImage imageChannel = (ChannelImage) contentInstance;
				String name = imageChannel.getName(); // This is the URL end
				String url = getFullURLPrefix() + "/" + name;
				DrawingPanel dp = new DrawingPanel(url, display.getPreferredHighlightColor());
				String colorString = String.format("%06X", display.getPreferredHighlightColor().getRGB() & 0xFFFFFF);
				url += "&color=" + colorString;

				String longerName = imageChannel.getName();
				if (longerName.length() > maxLen)
					longerName = longerName.substring(0, maxLen - 3) + "...";
				DDIconButton button = new DDIconButton(imageChannel, display, maxLen, dp.getIcon());
				String[] pieces = name.split("/", -1);
				button.setText(
						"<html>" + trunc(pieces[pieces.length - 1] + "<br>" + imageChannel.getDescription(), maxLen)
								+ "</html>");
				button.setAlignmentX(JPanel.CENTER_ALIGNMENT);
				button.setSelected(contentInstance.equals(display.getContent()));
				button.addActionListener(this); // Order is important for the GUI. This one goes last.
				button.addActionListener(display);
				bg.add(button);
				pictureBox.add(button);
				pictureBox.add(Box.createRigidArea(new Dimension(hGap, vGap)));
			} else {
				DDButton button = new DDButton((Channel) contentInstance, display, maxLen);

				if (SHOW_IN_WINDOW) {
					button.setFont(button.getFont().deriveFont(FS));
				} else {
					switch (button.numLinesInTitle()) {
					case 2:
						button.setFont(button.getFont().deriveFont(FS * 0.8300f));
						break;
					case 3:
						button.setFont(button.getFont().deriveFont(FS * 0.7000f));
						break;
					default:
						if (cols == 2 && button.getText().length() > 15)
							button.setFont(button.getFont().deriveFont(FS * 0.8500f));
						else
							button.setFont(button.getFont().deriveFont(FS));
						break;
					}
				}

				button.setSelected(contentInstance.equals(display.getContent()));
				button.addActionListener(this); // Order is important for the GUI. This one goes last.
				button.addActionListener(display);
				bg.add(button);
				gbc.gridheight = 1;
				expGrid.add(button, gbc);
			}
			if (pictureBox.getComponentCount() > 0) {
				gbc.gridheight = 2;
				expGrid.add(pictureBox, gbc);
			}
		}

		expGrid.setAlignmentX(JComponent.TOP_ALIGNMENT);
		return expGrid;
	}

	private Set<SignageContent> getMySpecialChannels() {
		try {
			return ChannelsFromDatabase.getSpecialChannelsForDisplay(display.getDBDisplayNumber());
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
		}
		return null;
	}

}
