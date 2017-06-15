package gov.fnal.ppd.dd.channel.list;

import gov.fnal.ppd.dd.changer.CategoryDictionary;
import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.util.Set;

/**
 * A table of all the channels in the system (relevant for this location)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public class ChannelChooserTableModel extends AbstractChannelTableModel {

	private static final long	serialVersionUID	= 5736576268689815979L;

	/**
	 * 
	 */
	public ChannelChooserTableModel() {

		relativeWidths = new int[] { 175, 70, 450 };
		columnNames = new String[] { "Category", "Chan#", "Channel Details" };

		ChannelCategory categories[] = CategoryDictionary.getCategories();

		for (ChannelCategory CAT : categories) {
			if (CAT.getValue().equals("Archive"))
				continue;
			Set<SignageContent> chans = ChannelCatalogFactory.getInstance().getChannelCatalog(CAT);
			for (SignageContent CHAN : chans)
				allChannels.add((Channel) CHAN);
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
			return String.class;
		case 1:
			return Integer.class;

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
	public Channel getRow(final int row) {
		return allChannels.get(row);
	}

	@Override
	public Object getValueAt(int row, int col) {
		Channel cac = allChannels.get(row);
		switch (col) {

		case 1:
			return cac.getNumber();
		case 2:
			return cac;
		

		default:
		case 0:
			return cac.getCategory().getValue();

		}
	}

}