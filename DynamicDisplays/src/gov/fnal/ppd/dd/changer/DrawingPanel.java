package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.ChannelSelector.screenDimension;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

class DrawingPanel extends JPanel {

	private static final long				serialVersionUID	= 8963747596403311688L;
	private static Map<String, ImageIcon>	cache				= new HashMap<String, ImageIcon>();
	private ImageIcon						icon;
	private static int						LONG_EDGE			= 220;
	private static boolean					firstTime			= true;

	public DrawingPanel(String url, Color bgColor) {
		if (firstTime) {
			firstTime = false;
			int width = screenDimension.width;
			int height = screenDimension.height;

			if (height < 500) {
				LONG_EDGE = 100;
			} else if (width < 801 || height < 600) {
				LONG_EDGE = 120;
			} else if (width < 1000 || height < 800) {
				LONG_EDGE = 160;
			} else if (width < 1400 || height < 1000) {
				LONG_EDGE = 200;
			}
		}
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

				String filename = url.replace("upload/items", "upload/items/thumbs");
				if (url.contains("otheritems"))
					filename = url.replace("upload/items/otheritems", "upload/items/otheritems/thumbs");

				ImageIcon icon0 = new ImageIcon(new URL(filename));

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

				BufferedImage smallImage = DocentGrid.createResizedCopy(img, width, height, true);
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

	public ImageIcon getIcon() {
		return icon;
	}
}