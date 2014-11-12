package gov.fnal.ppd.signage.changer;

import static gov.fnal.ppd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.GlobalVariables.WEB_SERVER_NAME;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.channel.ChannelImage;
import gov.fnal.ppd.signage.util.DisplayButtonGroup;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ImageGrid extends DetailedInformationGrid {

	private static class JWhiteLabel extends JLabel {
		public JWhiteLabel(String text) {
			super("  " + trunc(text) + " ");
			setOpaque(true);

		}
	}

	private static class DrawingPanel extends JPanel {

		private ImageIcon	icon;

		public DrawingPanel(String url) {
			try {
				// This does not work--we run out of memory with the full-size images!

				ImageIcon icon0 = new ImageIcon(new URL(url));

				Image img = icon0.getImage();
				BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
				Graphics g = bi.createGraphics();
				g.drawImage(img, 0, 0, 100, 100, null, null);
				icon = new ImageIcon(bi);

				// int w = icon.getIconWidth();
				// int h = icon.getIconHeight();
				setPreferredSize(new Dimension(100, 100));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			icon.paintIcon(this, g, 0, 0);
		}

	}

	public ImageGrid(Display display, DisplayButtonGroup bg) {
		super(display, bg, 7);
	}

	public static String trunc(String text) {
		if (text.length() < 37)
			return text;
		return text.substring(0, 35) + " ...";
	}

	@Override
	protected JComponent makeExpGrid(int set) {
		Set<SignageContent> list = ChannelCatalogFactory.getInstance().getChannelCatalog(ChannelCategory.IMAGE);

		int ncol = 3;
		if (SHOW_IN_WINDOW)
			ncol = 2;
		GridLayout g = new GridLayout(0, ncol);

		JPanel internalGrid = new JPanel(g);
		internalGrid.setOpaque(true);
		internalGrid.setBackground(display.getPreferredHighlightColor());
		for (SignageContent content : list)
			try {
				ChannelImage imageChannel = (ChannelImage) content;
				String name = imageChannel.getName(); // This is the URL end
				String url = "http://" + WEB_SERVER_NAME + "/XOC/" + name;
				// DrawingPanel dp = new DrawingPanel(url);

				String desc = imageChannel.getDescription();
				String exp = imageChannel.getExp();

				Box b = Box.createVerticalBox();
				b.setOpaque(true);

				DDButton button = new DDButton(imageChannel, display, 30);
				button.setText(name.replace("upload/items/", ""));
				b.add(new JWhiteLabel(exp));
				b.add(button);
				// b.add(dp);
				b.add(new JWhiteLabel(desc));
				b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.white),
						BorderFactory.createLineBorder(display.getPreferredHighlightColor(), 5)));
				button.addActionListener(display);

				bg.add(button);

				internalGrid.add(b);
			} catch (Exception e) {
				e.printStackTrace();
			}

		expGrid = new JScrollPane(internalGrid);
		expGrid.setAlignmentX(JComponent.TOP_ALIGNMENT);
		return expGrid;
	}
}
