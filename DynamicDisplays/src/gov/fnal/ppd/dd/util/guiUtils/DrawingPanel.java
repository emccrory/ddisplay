package gov.fnal.ppd.dd.util.guiUtils;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * Specific to the environment of the Fermilab Dynamic Displays system, this widget holds a thumbnail and a URL for an image, and it
 * behaves like a button.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014-21
 *
 */
public class DrawingPanel extends JPanel {

	private static final long				serialVersionUID	= 8963747596403311688L;
	private static Map<String, ImageIcon>	cache				= new HashMap<String, ImageIcon>();
	private ImageIcon						icon;
	private static int						LONG_EDGE			= 220;

	static {
		if (SHOW_IN_WINDOW) {
			LONG_EDGE = 180;
		}
	}

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

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		icon.paintIcon(this, g, 0, 0);
	}

	public ImageIcon getIcon() {
		return icon;
	}
}