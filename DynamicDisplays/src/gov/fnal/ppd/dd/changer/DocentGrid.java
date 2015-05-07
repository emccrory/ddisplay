/*
 * DocentGrid
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.channel.ChannelImpl;
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
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * 
 */
public class DocentGrid extends DetailedInformationGrid {

	private static final long	serialVersionUID	= 3102445872732142334L;

	private static class JWhiteLabel extends JLabel {

		private static final long	serialVersionUID	= -4821299908662876531L;

		public JWhiteLabel(String text) {
			super("  " + trunc(text, MAX_CAPTION_LENGTH) + " ");
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
		private final int						LONG_EDGE			= (SHOW_IN_WINDOW ? 180 : 240);

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
			super(channel, display, maxLen / 2);
			setIcon(icon);
			setVerticalTextPosition(BOTTOM);
			setHorizontalTextPosition(CENTER);
			setMargin(new Insets(5, 5, 5, 5));
		}

	}

	private static final List<SignageContent>	buttonList			= new ArrayList<SignageContent>();
	private List<SignageContent>				myButtonList		= new ArrayList<SignageContent>();

	private static final int					MAX_CAPTION_LENGTH	= (SHOW_IN_WINDOW ? 25 : 30);

	// private String docentName;

	// static {
	// Set<SignageContent> imageList = ChannelCatalogFactory.getInstance().getChannelCatalog(ChannelCategory.IMAGE);
	// buttonList.addAll(imageList);
	// ChannelCategory[] categories = CategoryDictionary.getCategories();
	// for (ChannelCategory cat : categories) {
	// Set<SignageContent> list = ChannelCatalogFactory.getInstance().getChannelCatalog(cat);
	// buttonList.addAll(list);
	// }
	// }

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
		// this.docentName = docentName;

		// Get the channels associated with this docent.
		if (buttonList.size() == 0) {
			try {
				/**
				 * FIXME This is the only place that I can think of that requires the Channel and Portfolio tables to be in the same
				 * database. To break this will require two Docent tables, on that is in the same DB as Channel and the other in the
				 * same DB as Portfolio.
				 */
				Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();

				Statement stmt = connection.createStatement();
				Statement stmt1 = connection.createStatement();
				Statement stmt2 = connection.createStatement();
				@SuppressWarnings("unused")
				ResultSet rs = stmt.executeQuery("USE " + DATABASE_NAME);

				String query1 = "select ChannelNumber,Name,Description,URL,DwellTime,Category from Docent,Channel "
						+ "where ChannelNumber=Number and DocentName='" + docentName + "'";
				String query2 = "select PortfolioNumber,Filename,Description,Experiment from Docent,Portfolio "
						+ "where PortfolioNumber=PortfolioID and DocentName='" + docentName + "'";

				ResultSet rsChan = stmt1.executeQuery(query1);
				ResultSet rsPort = stmt2.executeQuery(query2);

				rsChan.first(); // Move to first returned row
				while (!rsChan.isAfterLast()) {
					String name = ConnectionToDynamicDisplaysDatabase.makeString(rsChan.getAsciiStream("Name"));
					String descr = ConnectionToDynamicDisplaysDatabase.makeString(rsChan.getAsciiStream("Description"));
					String url = ConnectionToDynamicDisplaysDatabase.makeString(rsChan.getAsciiStream("URL"));
					String category = ConnectionToDynamicDisplaysDatabase.makeString(rsChan.getAsciiStream("Category"));
					long dwell = rsChan.getLong("DwellTime");
					int chanNum = rsChan.getInt("ChannelNumber");

					SignageContent c = new ChannelImpl(name, new ChannelCategory(category, category), descr, new URI(url), chanNum,
							dwell);

					buttonList.add(c);
					rsChan.next();
				}

				rsPort.first(); // Move to first returned row
				while (!rsPort.isAfterLast()) {
					String name = ConnectionToDynamicDisplaysDatabase.makeString(rsPort.getAsciiStream("FileName"));
					String descr = ConnectionToDynamicDisplaysDatabase.makeString(rsPort.getAsciiStream("Description"));
					String exp = ConnectionToDynamicDisplaysDatabase.makeString(rsPort.getAsciiStream("Experiment"));

					String url = getFullURLPrefix() + "/portfolioOneSlide.php?photo="
							+ URLEncoder.encode(name, "UTF-8") + "&caption=" + URLEncoder.encode(descr, "UTF-8");
					SignageContent c = new ChannelImage(name, ChannelCategory.IMAGE, descr, new URI(url), 0, exp);

					buttonList.add(c);
					rsPort.next();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for (SignageContent C : buttonList)
			myButtonList.add(C);

		// System.out.println("*** Number of buttons for docent " + docentName + ": " + myButtonList.size() + ", display number "
		// + display.getNumber());
	}

	private static String trunc(String text, int maxLen) {
		if (text.length() < maxLen)
			return text;
		return text.substring(0, maxLen - 2) + " ...";
	}

	@Override
	protected JComponent makeExpGrid(ChannelCategory set, int frameNumber) {
		int ncol = 5;
		if (SHOW_IN_WINDOW)
			ncol = 3;

		final JPanel simpleButtonBox = new JPanel(new GridLayout(0, ncol));
		simpleButtonBox.setOpaque(true);
		simpleButtonBox.setBackground(display.getPreferredHighlightColor());
		final JPanel internalGrid = new JPanel(new GridLayout(0, ncol));
		internalGrid.setOpaque(true);
		internalGrid.setBackground(display.getPreferredHighlightColor());

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

						DDButton button = new DDIconButton(imageChannel, display, MAX_CAPTION_LENGTH, dp.getIcon());
						button.setText(trunc(name.replace("upload/items/", ""), 2 * MAX_CAPTION_LENGTH / 3));
						button.setAlignmentX(JPanel.LEFT_ALIGNMENT);
						b.add(new JWhiteLabel(exp, 18.0f));
						b.add(button);
						// b.add(dp);
						b.add(new JWhiteLabel(desc));
						b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.white),
								BorderFactory.createLineBorder(display.getPreferredHighlightColor(), 2)));

						button.addActionListener(display);

						bg.add(button);

						internalGrid.add(b);
					} else {
						DDButton button = new DDButton((Channel) content, display, MAX_CAPTION_LENGTH);
						button.setMargin(new Insets(20, 2, 20, 2));

						button.addActionListener(this); // Order is important for the GUI. This one goes last.
						button.addActionListener(display);
						bg.add(button);
						simpleButtonBox.add(button);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
		}

		Box outerBox = Box.createVerticalBox();
		outerBox.setOpaque(true);
		outerBox.setBackground(display.getPreferredHighlightColor());
		outerBox.add(simpleButtonBox);
		outerBox.add(internalGrid);

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
