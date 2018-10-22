package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.util.Util.println;

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

import gov.fnal.ppd.dd.util.PropertiesFile;

/**
 * Assist in the resizing of the images needed in the ChannelSelector, and related programs.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2017
 * 
 */
public class DrawingPanelForImage extends JPanel {

	private static final long				serialVersionUID	= 8963747596403311688L;
	private static final int				LONG_EDGE			= (SHOW_IN_WINDOW ? 150 : 200);
	private static boolean					verbose				= PropertiesFile.getBooleanProperty("ImageURLVerbose", false);

	/// The static list of the icons we have created
	private static Map<String, ImageIcon>	cache				= new HashMap<String, ImageIcon>();

	/// The icon for this instance of the class
	private ImageIcon						icon;

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
				// This does not work unless we cache the images -- The JVM runs out of space!

				String modifiedURL = url.replace("upload/items", "upload/items/thumbs");
				modifiedURL = modifiedURL.replace("upload/items/thumbs/otheritems", "upload/items/otheritems/thumbs");

				if (verbose)
					println(getClass(), "fetching " + modifiedURL);

				ImageIcon icon0 = new ImageIcon(new URL(modifiedURL));
				// Note that ImageIcon is perfectly happy to get a URL that does not point to an image.
				// As long as the URL itself is not malformed, it is going to succeed.

				int h = icon0.getIconHeight();
				int w = icon0.getIconWidth();
				if (h >= w) {
					height = 2 * LONG_EDGE / 3;
					width = height * w / h;
				} else {
					width = LONG_EDGE;
					height = width * h / w;
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

	/**
	 * @return An icon of this image
	 */
	public ImageIcon getIcon() {
		return icon;
	}
}