package gov.fnal.ppd.signage.changer;

import static gov.fnal.ppd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.GlobalVariables.WEB_SERVER_NAME;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.channel.ChannelImage;
import gov.fnal.ppd.signage.util.DisplayButtonGroup;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

public class ImageGrid extends DetailedInformationGrid {

	private static class JWhiteLabel extends JLabel {
		public JWhiteLabel(String text) {
			super("  " + trunc(text) + " ");
			setOpaque(true);
			setAlignmentX(JPanel.LEFT_ALIGNMENT);
		}
	}

	private static class DrawingPanel extends JPanel {

		private static Map<String, ImageIcon>	cache		= new HashMap<String, ImageIcon>();
		private ImageIcon						icon;
		private final int						LONG_EDGE	= (SHOW_IN_WINDOW ? 300 : 450);

		public DrawingPanel(String url, Color bgColor) {
			int height = LONG_EDGE;
			int width = LONG_EDGE;
			if (cache.containsKey(url)) {
				icon = cache.get(url); // This just copies the reference, not the icon itself.
				width = icon.getIconWidth();
				height = icon.getIconHeight();
			} else
				try {
					// This does not work unless we cache the images (we run out of space!)
					// System.out.println(DrawingPanel.class.getSimpleName() + ": fetching image at " + url);

					ImageIcon icon0 = new ImageIcon(new URL(url));
					int h = icon0.getIconHeight();
					int w = icon0.getIconWidth();
					if (h > w) {
						width = LONG_EDGE * w / h;
						height = LONG_EDGE;
					} else {
						height = LONG_EDGE * h / w;
						width = LONG_EDGE;
					}
					Image img = icon0.getImage();

					BufferedImage smallImage = createResizedCopy(img, width, height, true);
					icon = new ImageIcon(smallImage);
					cache.put(url, icon);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}

			setMinimumSize(new Dimension(width, height));
			setMaximumSize(new Dimension(width, height));
			setPreferredSize(new Dimension(width, height));
			setBackground(bgColor);
			setAlignmentX(JPanel.LEFT_ALIGNMENT);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			icon.paintIcon(this, g, 0, 0);
		}
	}

	private static boolean						firstTime	= true;
	private static final Set<SignageContent>	list		= ChannelCatalogFactory.getInstance().getChannelCatalog(
																	ChannelCategory.IMAGE);

	public ImageGrid(Display display, DisplayButtonGroup bg) {
		super(display, bg, 7);
	}

	private static String trunc(String text) {
		if (text.length() < 37)
			return text;
		return text.substring(0, 35) + " ...";
	}

	@Override
	protected JComponent makeExpGrid(int set) {
		int ncol = 3;
		if (SHOW_IN_WINDOW)
			ncol = 2;

		final JPanel internalGrid = new JPanel(new GridLayout(0, ncol));
		internalGrid.setOpaque(true);
		internalGrid.setBackground(display.getPreferredHighlightColor());

		synchronized (list) {
			if (firstTime) {
				System.out.println(this.getClass().getSimpleName() + ".makeExpGrid(): resizing " + list.size() + " images.");
				firstTime = false;
			} else
				System.out.println(this.getClass().getSimpleName() + ".makeExpGrid(): finding " + list.size() + " images.");

			String colorString = String.format("%06X", display.getPreferredHighlightColor().getRGB() & 0xFFFFFF);
			for (SignageContent content : list)
				try {
					ChannelImage imageChannel = (ChannelImage) content;
					String name = imageChannel.getName(); // This is the URL end
					String url = "http://" + WEB_SERVER_NAME + "/XOC/" + name;
					// if (index++ % 10 == 0)
					// System.out.println(this.getClass().getSimpleName() + ".makeExpGrid(): resizing " + index + " -- " +
					// url);
					DrawingPanel dp = new DrawingPanel(url, display.getPreferredHighlightColor());

					url += "&color=" + colorString;
					String desc = imageChannel.getDescription();
					String exp = imageChannel.getExp();

					Box b = Box.createVerticalBox();
					b.setOpaque(true);

					DDButton button = new DDButton(imageChannel, display, 30);
					button.setText(name.replace("upload/items/", ""));
					button.setAlignmentX(JPanel.LEFT_ALIGNMENT);
					b.add(new JWhiteLabel(exp));
					b.add(button);
					b.add(dp);
					b.add(new JWhiteLabel(desc));
					b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.white),
							BorderFactory.createLineBorder(display.getPreferredHighlightColor(), 5)));
					button.addActionListener(display);

					bg.add(button);

					internalGrid.add(b);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}

		expGrid = new JScrollPane(internalGrid);
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

	private static BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight, boolean preserveAlpha) {
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
