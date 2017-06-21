package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import gov.fnal.ppd.dd.changer.CategoryDictionary;
import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.DrawingPanelForImage;
import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.awt.Color;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

	/**
	 * 
	 */
	public ImageChooserTableModel() {

		// Columns: (0) Image Icon, (1) Experiment, (2) URL

		if (SHOW_IN_WINDOW)
			relativeWidths = new int[] { 160, 70, 450 };
		else
			relativeWidths = new int[] { 300, 110, 800 };

		columnNames = new String[] { "Image", "Exp", "Image Details" };

		Set<SignageContent> list = ChannelCatalogFactory.getInstance().getChannelCatalog(ChannelCategory.IMAGE);

		for (SignageContent SC : list) {
			allChannels.add((ChannelImage) SC);
			String name = SC.getName(); // This is the URL end, without all the caption stuff.
			String url = getFullURLPrefix() + "/" + name;			
			DrawingPanelForImage dp = new DrawingPanelForImage(url, Color.green);
			icons.add(dp.getIcon());
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
			return Channel.class;

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
	public ChannelImage getRow(final int row) {
		return (ChannelImage) allChannels.get(row);
	}

	@Override
	public Object getValueAt(int row, int col) {
		ChannelImage cac = (ChannelImage) allChannels.get(row);
		switch (col) {

		case 0:
			return icons.get(row);

		case 1:
		default:
			return cac.getExp();
			
		case 2:
			return cac;

		}
	}

}