package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_FOLDER;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_NAME;
import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DisplayButtonGroup;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

/**
 * Create a tab of images that can be shown on a Display. This is a very different way of giving user content to see because it ties
 * up the URL from within this class (as opposed to the DB containing the URLs and classes like this just simply read them).
 * 
 * This is likely to be inappropriate when we have thousands of images: All the images are read from the server when the GUi starts.
 * At 151 images (today), it takes the better part of a minute to complete. It may be better to launch this as a separate
 * application, when needed.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class ImageGrid extends DetailedInformationGrid {

	private static final long	serialVersionUID	= 3102445872732142334L;

	private static class JWhiteLabel extends JLabel {

		private static final long	serialVersionUID	= -4821299908662876531L;

		public JWhiteLabel(String text) {
			super("  " + trunc(text) + " ");
			setOpaque(true);
			setAlignmentX(JPanel.LEFT_ALIGNMENT);
		}

		public JWhiteLabel(String text, float size) {
			this(text);
			setFont(getFont().deriveFont(size));
		}
	}

	private static class DrawingPanel extends JPanel {

		private static final long				serialVersionUID	= 8963747596403311688L;
		private static Map<String, ImageIcon>	cache				= new HashMap<String, ImageIcon>();
		private ImageIcon						icon;
		private final int						LONG_EDGE			= (SHOW_IN_WINDOW ? 300 : 350);

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

					ImageIcon icon0 = new ImageIcon(new URL(url.replace("upload/items", "upload/items/thumbs")));
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

		public ImageIcon getIcon() {
			return icon;
		}
	}

	private static class DDIconButton extends DDButton {

		private static final long	serialVersionUID	= -8377645551707373769L;

		public DDIconButton(Channel channel, Display display, int maxLen, Icon icon) {
			super(channel, display, maxLen);
			setIcon(icon);
			setVerticalTextPosition(BOTTOM);
			setHorizontalTextPosition(CENTER);
			setMargin(new Insets(5, 5, 5, 5));
		}

	}

	private static boolean						firstTime			= true;

	private static final Set<SignageContent>	list				= ChannelCatalogFactory.getInstance().getChannelCatalog(
																			ChannelCategory.IMAGE);

	private static final int					MAX_CAPTION_LENGTH	= (SHOW_IN_WINDOW ? 33 : 46);

	/**
	 * Create this tab for the ChannelSelector GUI
	 * 
	 * @param display
	 * @param bg
	 */
	public ImageGrid(final Display display, final DisplayButtonGroup bg) {
		super(display, bg);
	}

	private static String trunc(String text) {
		if (text.length() < MAX_CAPTION_LENGTH)
			return text;
		return text.substring(0, MAX_CAPTION_LENGTH - 2) + " ...";
	}

	@Override
	protected JComponent makeExpGrid(ChannelCategory set) {
		int ncol = 4;
		if (SHOW_IN_WINDOW)
			ncol = 2;

		// TODO -- Implement touch scrolling for this panel. From a brief search of google, it seems that one must either
		// (a) Use JavaFX (and use their touch interface stuff) or (b) use the Canvas class (and implement it yourself)

		final JPanel internalGrid = new JPanel(new GridLayout(0, ncol));
		internalGrid.setOpaque(true);
		internalGrid.setBackground(display.getPreferredHighlightColor());

		synchronized (list) {
			if (firstTime) {
				System.out.println(this.getClass().getSimpleName() + ".makeExpGrid(): resizing " + list.size() + " images.");
				firstTime = false;
			} else
				System.out.print(".");

			TreeSet<SignageContent> newList = new TreeSet<SignageContent>(new Comparator<SignageContent>() {
				public int compare(SignageContent o1, SignageContent o2) {
					if (o1 instanceof ChannelImage && o2 instanceof ChannelImage) {
						ChannelImage c1 = (ChannelImage) o1;
						ChannelImage c2 = (ChannelImage) o2;
						return (c1.getExp() + c1.getName()).toUpperCase().compareTo((c2.getExp() + c2.getName()).toUpperCase());
					}
					return o1.getName().compareTo(o2.getName());
				}
			});
			for (SignageContent P : list) {
				ChannelImage imageChannel = (ChannelImage) P;
				if (CategoryDictionary.isExperiment(imageChannel.getExp()))
					newList.add(P);
			}

			String colorString = String.format("%06X", display.getPreferredHighlightColor().getRGB() & 0xFFFFFF);
			for (SignageContent content : newList)
				try {
					ChannelImage imageChannel = (ChannelImage) content;
					String name = imageChannel.getName(); // This is the URL end
					String url = "http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/" + name;
					// if (index++ % 10 == 0)
					// System.out.println(this.getClass().getSimpleName() + ".makeExpGrid(): resizing " + index + " -- " +
					// url);
					DrawingPanel dp = new DrawingPanel(url, display.getPreferredHighlightColor());

					url += "&color=" + colorString;
					String desc = imageChannel.getDescription();
					String exp = imageChannel.getExp();

					Box b = Box.createVerticalBox();
					b.setOpaque(true);

					DDButton button = new DDIconButton(imageChannel, display, MAX_CAPTION_LENGTH, dp.getIcon());
					button.setText(name.replace("upload/items/", ""));
					button.setAlignmentX(JPanel.LEFT_ALIGNMENT);
					b.add(new JWhiteLabel(exp, 24.0f));
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
