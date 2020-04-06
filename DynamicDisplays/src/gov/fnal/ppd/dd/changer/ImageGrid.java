/*
 * ImageGrid
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.ChannelSelector.progressMonitor;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.specific.DisplayButtonGroup;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
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
 * At 151 images (2015, over 1200 in 2018), it takes the better part of a minute to complete.
 * 
 * And then if there are lots of displays being controlled, these images are placed in each display's GUI, multiplying the startup
 * time by the number of displays. E.g., for a controller that controls all the displays (42 in May 2017), it takes forever.
 * 
 * Changed the nominal thumb-nail image size from 600 pixels @ Quality=75 to 300 pixels @ Q=40. This makes about a 5X reduction in
 * the size of these images, but the impact on startup here has been minimal. (5/23/2017)
 * 
 * It may be better to launch this as a separate application, when needed. Or to have these tabs be constructed after the rest of
 * the GUI has been constructed and shown to the user.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ImageGrid extends DetailedInformationGrid {

	private static final long serialVersionUID = 3102445872732142334L;

	private static class JWhiteLabel extends JLabel {

		private static final long serialVersionUID = -4821299908662876531L;

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

	private static class DDIconButton extends DDButton {

		private static final long serialVersionUID = -8377645551707373769L;

		public DDIconButton(Channel channel, Display display, int maxLen, Icon icon) {
			super(channel, display, maxLen);
			setIcon(icon);
			setVerticalTextPosition(BOTTOM);
			setHorizontalTextPosition(CENTER);
			setMargin(new Insets(5, 5, 5, 5));
		}

	}

	private static boolean						firstTime			= true;

	private static final Set<SignageContent>	list				= ChannelCatalogFactory.getInstance()
			.getChannelCatalog(ChannelClassification.IMAGE);

	private static final int					MAX_CAPTION_LENGTH	= (SHOW_IN_WINDOW ? 44 : 47);

	private static long							beginResizingAt		= 0L;

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
	protected JComponent makeExpGrid(ChannelClassification set) {
		int progMonStart = progressMonitor.getMaximum()/3;
		int progMonCount = 0;
		int ncol = 4;
		if (SHOW_IN_WINDOW)
			ncol = 2;

		// TODO -- Implement touch scrolling for this panel. From a brief search of google, it seems that one must either
		// (a) Use JavaFX (and use their touch interface stuff) or (b) use the Canvas class (and implement it yourself)

		final Color displaySelectedColor = display.getPreferredHighlightColor();
		final JTabbedPane internalTabPane = new JTabbedPane() {
			private static final long	serialVersionUID	= -8241902366493820040L;
			private final float			brightness			= (Color.RGBtoHSB(displaySelectedColor.getRed(),
					displaySelectedColor.getGreen(), displaySelectedColor.getBlue(), null))[2];
			private final Color			unselectedColor		= (brightness < 0.6f ? Color.WHITE : Color.BLACK);

			public Color getForegroundAt(int index) {
				// If the background color is dark, make the font color white.
				if (getSelectedIndex() == index)
					return Color.BLACK;
				return unselectedColor;
			}
		};
		internalTabPane.setOpaque(true);
		internalTabPane.setBackground(displaySelectedColor);
		internalTabPane.setFont( new Font( Font.MONOSPACED, Font.PLAIN, 12 ) );

		HashMap<String, JPanel> expPanels = new HashMap<String, JPanel>();

		final int WIDTH = (SHOW_IN_WINDOW ? 300 : 350);
		final int HEIGHT = 3 * WIDTH / 4;

		synchronized (list) {
			if (firstTime) {
				println(this.getClass(), ".makeExpGrid(): resizing " + list.size() + " images.");
				println(this.getClass(),
						".makeExpGrid(): working on the images for display number " + display.getVirtualDisplayNumber() + " ...");
				beginResizingAt = System.currentTimeMillis();
			} else
				System.out.print(display.getVirtualDisplayNumber() + " ");

			TreeSet<SignageContent> newList = new TreeSet<SignageContent>(new Comparator<SignageContent>() {
				public int compare(SignageContent o1, SignageContent o2) {
					// Sort by Experiment name first, then by image number.
					if (o1 instanceof ChannelImage && o2 instanceof ChannelImage) {
						ChannelImage c1 = (ChannelImage) o1;
						ChannelImage c2 = (ChannelImage) o2;
						if (c1.getExp().toUpperCase().equals(c2.getExp().toUpperCase()))
							return ((Integer) c1.getNumber()).compareTo(c2.getNumber());

						return (c1.getExp() + c1.getName()).toUpperCase().compareTo((c2.getExp() + c2.getName()).toUpperCase());
					}
					return o1.getName().compareTo(o2.getName());
				}
			});
			for (SignageContent P : list) {
				// Prior to 3/1/2016, it was assumed that each location would want a different set of images, based on
				// the focus of that location (e.g., Neutrino versus CERN). The bad part of this is that when someone thinks
				// of a new "experiment", the code has to be modified. So we abandon this sorting.

				newList.add(P);
			}

			String colorString = String.format("%06X", display.getPreferredHighlightColor().getRGB() & 0xFFFFFF);
			for (SignageContent content : newList)
				try {
					if (firstTime) {
						progressMonitor.setProgress(progMonStart + (progMonCount++) / 200);
					}
					ChannelImage imageChannel = (ChannelImage) content;
					String name = imageChannel.getName(); // This is the URL end
					String url = getFullURLPrefix() + "/" + name;
					// System.out.println(this.getClass().getSimpleName() + ".makeExpGrid(): resizing " + url);

					DrawingPanelForImage dp = new DrawingPanelForImage(url, display.getPreferredHighlightColor());

					url += "&color=" + colorString;
					String desc = imageChannel.getDescription();
					String exp = imageChannel.getExp();

					Box b = Box.createVerticalBox();
					b.setPreferredSize(new Dimension(WIDTH, HEIGHT));
					b.setMinimumSize(new Dimension(WIDTH, HEIGHT));
					b.setMaximumSize(new Dimension(WIDTH, HEIGHT));

					b.setOpaque(true);

					DDButton button = new DDIconButton(imageChannel, display, MAX_CAPTION_LENGTH, dp.getIcon());
					String[] fields = name.split("/", -1);
					button.setText(fields[fields.length - 1]);
					button.setToolTipText(name);
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
		firstTime = false;

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
			String expPadded = padd(exp);
			internalTabPane.addTab(expPadded, sp);
		}

		if (!SHOW_IN_WINDOW)
			internalTabPane.setFont(getFont().deriveFont(20.0f));

		expGrid = new JPanel(new BorderLayout());
		JLabel lab = new JLabel("Portfolio image categories");
		lab.setFont(new Font("Arial", Font.ITALIC, 20));
		Box box = Box.createHorizontalBox();
		box.setOpaque(true);
		box.setBackground(displaySelectedColor);

		float brightness = (Color.RGBtoHSB(displaySelectedColor.getRed(), displaySelectedColor.getGreen(),
				displaySelectedColor.getBlue(), null))[2];

		if (brightness < 0.6)
			lab.setForeground(Color.white);
		box.add(Box.createGlue());
		box.add(lab);
		box.add(Box.createGlue());
		expGrid.add(box, BorderLayout.NORTH);
		expGrid.add(internalTabPane, BorderLayout.CENTER);
		expGrid.setAlignmentX(JComponent.TOP_ALIGNMENT);

		println(getClass(), "Image tab completed for display " + display.getVirtualDisplayNumber() + "|"
				+ display.getDBDisplayNumber() + ".  Elapsed time = " + (System.currentTimeMillis() - beginResizingAt) + " msec");
		return expGrid;
	}

	private static final int DESIRED_LEN = 12; 

	/// A utility for making the sizes of the tabs be equal.  This makes them line up better, and, IMHO, it is easier to read.
	private static String padd(String exp) {
		if (exp.length() == DESIRED_LEN)
			return exp;
		if (exp.length() > DESIRED_LEN)
			return exp.substring(0, DESIRED_LEN - 1) + "\u2026";
		while (exp.length() < DESIRED_LEN) {
			if (exp.length() % 2 == 0)
				exp = " " + exp;
			else
				exp += " ";
		}
		return exp;
	}

	private static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list, new Comparator<T>() {

			@Override
			public int compare(T o1, T o2) {
				return o2.compareTo(o1);
			}
		});
		return list;
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
