/*
 * DocentGrid
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.db.ChannelsFromDatabase.getDocentContent;
import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.specific.DisplayButtonGroup;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

/**
 * Create a tab that is specifically tailored to the use of the Docents (and tour guides) at the Lab.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class DocentGrid extends DetailedInformationGrid {

	private static final long		serialVersionUID	= 3102445872732142334L;
	private List<SignageContent>	myButtonList;
	static final int				MAX_CAPTION_LENGTH	= (SHOW_IN_WINDOW ? 35 : 40);

	/**
	 * Create this tab for the ChannelSelector GUI
	 * 
	 * @param display
	 * @param bg
	 * @param docentName
	 *            The name of the docent for whom we are creating this grid
	 */
	public DocentGrid(final Display display, final DisplayButtonGroup bg, String docentName) {
		super(display, bg);
		myButtonList = getDocentContent(docentName);
	}

	static String trunc(String text, int maxLen) {
		if (text.length() < maxLen)
			return text;
		return text.substring(0, maxLen - 2) + " ...";
	}

	@Override
	protected JComponent makeExpGrid(ChannelClassification set) {
		int ncol = 5;
		if (SHOW_IN_WINDOW)
			ncol = 3;

		final JPanel channelButtonBox = new JPanel(new GridLayout(0, 3));
		channelButtonBox.setOpaque(true);
		channelButtonBox.setBackground(display.getPreferredHighlightColor());

		final JPanel pictureButtonBox = new JPanel(new GridLayout(0, ncol));
		pictureButtonBox.setOpaque(true);
		pictureButtonBox.setBackground(display.getPreferredHighlightColor());

		synchronized (myButtonList) {
			String colorString = String.format("%06X", display.getPreferredHighlightColor().getRGB() & 0xFFFFFF);
			for (SignageContent content : myButtonList)
				try {
					if (content instanceof ChannelImage) {
						ChannelImage imageChannel = (ChannelImage) content;
						String name = imageChannel.getName(); // This is the URL end
						String url = getFullURLPrefix() + "/" + name;
						// if (index++ % 10 == 0)
						// System.out.println(this.getClass().getSimpleName() + ".makeExpGrid(): resizing " + index + " -- " +
						// url);
						DrawingPanel dp = new DrawingPanel(url, display.getPreferredHighlightColor());

						url += "&color=" + colorString;
						String desc = imageChannel.getDescription();
						String exp = imageChannel.getExp();

						Box b = Box.createVerticalBox();
						b.setOpaque(true);
						b.setAlignmentX(CENTER_ALIGNMENT);

						DDButton button = new DDIconButton(imageChannel, display, MAX_CAPTION_LENGTH, dp.getIcon());
						String[] pieces = name.split("/", -1);
						String filename = pieces[pieces.length - 1].replace(".jpg", "").replace(".jpeg", "").replace(".JPG", "").replace(".JPEG", "");
						button.setText(trunc(filename, MAX_CAPTION_LENGTH));
						button.setAlignmentX(JPanel.CENTER_ALIGNMENT);
					
						b.add(new JWhiteLabel(exp, 18.0f));
						b.add(button);
						// b.add(dp);
						if (SHOW_IN_WINDOW) {
							button.setFont(button.getFont().deriveFont(Font.PLAIN, 10.0f));
							b.add(new JWhiteLabel(desc, Font.PLAIN, 11.0f));
						} else
							b.add(new JWhiteLabel(desc));
						b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.white),
								BorderFactory.createLineBorder(display.getPreferredHighlightColor(), 2)));

						button.addActionListener(display);

						bg.add(button);

						pictureButtonBox.add(b);
					} else {
						DDButton button = new DDButton((Channel) content, display, 50);
						if (SHOW_IN_WINDOW) {
							button.setFont(button.getFont().deriveFont(Font.PLAIN, 2 * FONT_SIZE / 5));
						} else {
							button.setFont(button.getFont().deriveFont(FONT_SIZE / 2));
							button.setMargin(new Insets(30, 2, 30, 2));
						}

						button.addActionListener(this); // Order is important for the GUI. This one goes last.
						button.addActionListener(display);
						bg.add(button);
						channelButtonBox.add(button);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
		}

		Box outerBox = Box.createVerticalBox();
		outerBox.setOpaque(true);
		outerBox.setBackground(display.getPreferredHighlightColor());
		outerBox.add(channelButtonBox);
		outerBox.add(pictureButtonBox);

		expGrid = new JScrollPane(outerBox);
		expGrid.setAlignmentX(JComponent.TOP_ALIGNMENT);
		JScrollBar sb = ((JScrollPane) expGrid).getVerticalScrollBar();
		sb.setUnitIncrement(20);
		sb.setBlockIncrement(317);
		if (!SHOW_IN_WINDOW) {
			sb.setPreferredSize(new Dimension(40, 0));
			sb.setBlockIncrement(450);
		}
		return expGrid;
	}

	static BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight, boolean preserveAlpha) {
		int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
		Graphics2D g = scaledBI.createGraphics();
		if (preserveAlpha) {
			g.setComposite(AlphaComposite.Src);
		}
		g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
		g.dispose();
		return scaledBI;
	}
}
