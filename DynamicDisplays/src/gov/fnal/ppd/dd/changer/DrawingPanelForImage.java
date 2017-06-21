package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;

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

/**
 * Assist in the resizing of the images needed in the ChannelSelector, and related programs.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2017
 * 
 */
public class DrawingPanelForImage extends JPanel {

	private static final long				serialVersionUID	= 8963747596403311688L;
	private static Map<String, ImageIcon>	cache				= new HashMap<String, ImageIcon>();
	private ImageIcon						icon;
	private final int						LONG_EDGE			= (SHOW_IN_WINDOW ? 150 : 250);

	/**
	 * @param url
	 *            The full URL of the image that is to be resized
	 * @param bgColor
	 *            The background color to use for this panel
	 */
	public DrawingPanelForImage(final String url, final Color bgColor) {
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

				String modifiedURL = url.replace("upload/items", "upload/items/thumbs");
				modifiedURL = modifiedURL.replace("upload/items/thumbs/otheritems", "upload/items/otheritems/thumbs");
				ImageIcon icon0 = new ImageIcon(new URL(modifiedURL));
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

				BufferedImage smallImage = ImageGrid.createResizedCopy(img, width, height, true);
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