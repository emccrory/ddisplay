/*
 * ImageGrid
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DisplayButtonGroup;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
import javax.swing.JTabbedPane;

/**
 * Create a tab of images that can be shown on a Display. This is a very different way of giving user content to see because it ties
 * up the URL from within this class (as opposed to the DB containing the URLs and classes like this just simply read them).
 * 
 * This is likely to be inappropriate when we have thousands of images: All the images are read from the server when the GUi starts.
 * At 151 images (today), it takes the better part of a minute to complete. It may be better to launch this as a separate
 * application, when needed.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
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

		// public JWhiteLabel(final String text, final float size) {
		// this(text);
		// setFont(getFont().deriveFont(size));
		// }
	}

	private static class DrawingPanel extends JPanel {

		private static final long				serialVersionUID	= 8963747596403311688L;
		private static Map<String, ImageIcon>	cache				= new HashMap<String, ImageIcon>();
		private ImageIcon						icon;
		private final int						LONG_EDGE			= (SHOW_IN_WINDOW ? 200 : 250);

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

	private static final int					MAX_CAPTION_LENGTH	= (SHOW_IN_WINDOW ? 44 : 47);

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
		return text.substring(0, MAX_CAPTION_LENGTH - 2) + " \u2026";
	}

	@Override
	protected JComponent makeExpGrid(ChannelCategory set, int frameNumber) {
		int ncol = 4;
		if (SHOW_IN_WINDOW)
			ncol = 2;

		// TODO -- Implement touch scrolling for this panel. From a brief search of google, it seems that one must either
		// (a) Use JavaFX (and use their touch interface stuff) or (b) use the Canvas class (and implement it yourself)

		final JTabbedPane internalTabPane = new JTabbedPane();
		internalTabPane.setOpaque(true);
		internalTabPane.setBackground(display.getPreferredHighlightColor());
		HashMap<String, JPanel> expPanels = new HashMap<String, JPanel>();

		// final long revertTime = 5 * ONE_MINUTE;
		final int WIDTH = (SHOW_IN_WINDOW ? 300 : 350);
		final int HEIGHT = 3 * WIDTH / 4;

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
			// for (SignageContent P : list) {
			// ChannelImage imageChannel = (ChannelImage) P;
			// if (CategoryDictionary.isExperiment(imageChannel.getExp())) {
			// // Executive decision: Image web pages expire after five minutes.
			// P.setExpiration(revertTime);
			// newList.add(P);
			// }
			// }

			String colorString = String.format("%06X", display.getPreferredHighlightColor().getRGB() & 0xFFFFFF);
			for (SignageContent content : newList)
				try {
					ChannelImage imageChannel = (ChannelImage) content;
					String name = imageChannel.getName(); // This is the URL end
					String url = getFullURLPrefix() + "/" + name;
					// System.out.println(this.getClass().getSimpleName() + ".makeExpGrid(): resizing " + url);

					DrawingPanel dp = new DrawingPanel(url, display.getPreferredHighlightColor());

					url += "&color=" + colorString;
					String desc = imageChannel.getDescription();
					String exp = imageChannel.getExp();

					Box b = Box.createVerticalBox();
					b.setPreferredSize(new Dimension(WIDTH, HEIGHT));
					b.setMinimumSize(new Dimension(WIDTH, HEIGHT));
					b.setMaximumSize(new Dimension(WIDTH, HEIGHT));

					b.setOpaque(true);

					DDButton button = new DDIconButton(imageChannel, display, MAX_CAPTION_LENGTH, dp.getIcon());
					button.setText(name.replace("upload/items/", ""));
					button.setAlignmentX(JPanel.LEFT_ALIGNMENT);
					// b.add(new JWhiteLabel(exp, 24.0f));
					b.add(button);
					// b.add(dp);
					b.add(new JWhiteLabel(desc));
					b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.white),
							BorderFactory.createLineBorder(display.getPreferredHighlightColor(), 5)));
					button.addActionListener(display);

					bg.add(button);

					JPanel expInternalGrid;
					if (expPanels.containsKey(exp)) {
						expInternalGrid = expPanels.get(exp);
					} else {
						expInternalGrid = new JPanel(new GridLayout(0, ncol));
						expInternalGrid.setOpaque(true);
						expInternalGrid.setBackground(display.getPreferredHighlightColor());
						expPanels.put(exp, expInternalGrid);
					}
					expInternalGrid.add(b);
					// internalGrid.add(b);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}

		for (String exp : asSortedList(expPanels.keySet())) {
			// TODO It might be better to sort this
			JScrollPane sp = new JScrollPane(expPanels.get(exp));
			JScrollBar sb = sp.getVerticalScrollBar();
			sb.setUnitIncrement(20);
			sb.setBlockIncrement(317);
			if (!SHOW_IN_WINDOW) {
				sb.setPreferredSize(new Dimension(40, 0));
				sb.setBlockIncrement(450);
			}
			internalTabPane.addTab(exp, sp);
		}

		if (!SHOW_IN_WINDOW)
			internalTabPane.setFont(getFont().deriveFont(20.0f));

		expGrid = new JPanel(new BorderLayout());
		expGrid.add(new JLabel("Experiment images tabs"), BorderLayout.NORTH);
		expGrid.add(internalTabPane, BorderLayout.CENTER);
		expGrid.setAlignmentX(JComponent.TOP_ALIGNMENT);

		return expGrid;
	}

	private static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
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
