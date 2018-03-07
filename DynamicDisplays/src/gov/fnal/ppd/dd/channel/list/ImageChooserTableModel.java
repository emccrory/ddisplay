package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.DrawingPanelForImage;
import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

/**
 * A table of all the channels in the system (relevant for this location)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public class ImageChooserTableModel extends AbstractChannelTableModel {

	private static final long	serialVersionUID	= 5736576268689815979L;

	private List<ImageIcon>		icons				= new ArrayList<ImageIcon>();
	private List<ChannelImage>	imageChannels		= new ArrayList<ChannelImage>();

	/**
	 * 
	 */
	public ImageChooserTableModel() {

		// Columns: (0) Image Icon, (1) Experiment, (2) URL

		if (SHOW_IN_WINDOW)
			relativeWidths = new int[] { 160, 120, 400 };
		else
			relativeWidths = new int[] { 300, 150, 800 };

		columnNames = new String[] { "Image", "Exp", "Image Details" };

		Set<SignageContent> list = ChannelCatalogFactory.getInstance().getChannelCatalog(ChannelCategory.IMAGE);
		TreeSet<SignageContent> sortedList = new TreeSet<SignageContent>(new Comparator<SignageContent>() {

			@Override
			public int compare(SignageContent o1, SignageContent o2) {
				if (o1 instanceof ChannelImage && o2 instanceof ChannelImage) {
					String s1 = ((ChannelImage) o1).getExp() + "_" + ((ChannelImage) o1).getName();
					String s2 = ((ChannelImage) o2).getExp() + "_" + ((ChannelImage) o2).getName();
					return s1.compareTo(s2);
				}
				return o1.getName().compareTo(o2.getName());
			}
		});
		sortedList.addAll(list);

		for (SignageContent SC : sortedList) {
			allChannels.add(new ChannelInList((ChannelImage) SC));
			String name = SC.getName(); // This is the URL end, without all the caption stuff.
			String url = getFullURLPrefix() + "/" + name;
			DrawingPanelForImage dp = new DrawingPanelForImage(url, Color.green);
			icons.add(dp.getIcon());
			imageChannels.add((ChannelImage) SC);
		}
	}

	protected String getLastColumn(int row) {
		return "+";
	}

	@Override
	public Class<? extends Object> getColumnClass(int c) {
		switch (c) {

		case 0:
		default:
			return ImageIcon.class;

		case 1:
			return String.class;

		case 2:
			return ChannelImage.class;

		}
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}

	/**
	 * @param row
	 * @return The channel at this row
	 */
	public ChannelInList getRow(final int row) {
		return allChannels.get(row);
	}

	@Override
	public Object getValueAt(int row, int col) {
		switch (col) {

		case 0:
			return icons.get(row);

		case 1:
		default:
			return imageChannels.get(row).getExp();

		case 2:
			return imageChannels.get(row);

		}
	}

}