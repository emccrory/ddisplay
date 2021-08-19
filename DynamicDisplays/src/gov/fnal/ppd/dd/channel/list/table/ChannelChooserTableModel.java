package gov.fnal.ppd.dd.channel.list.table;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;

import java.util.Set;

import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.changer.ChannelClassificationDictionary;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelInListImpl;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

/**
 * A table of all the channels in the system (relevant for this location)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public class ChannelChooserTableModel extends AbstractChannelTableModel {

	private static final long serialVersionUID = 5736576268689815979L;

	/**
	 * 
	 */
	public ChannelChooserTableModel() {

		if (SHOW_IN_WINDOW)
			setRelativeWidths(new int[] { 175, 70, 450 });
		else
			setRelativeWidths(new int[] { 200, 110, 800 });

		columnNames = new String[] { "Classification", "Chan#", "Channel Details" };

		ChannelClassification categories[] = ChannelClassificationDictionary.getCategoriesForLocation();

		for (ChannelClassification CAT : categories) {
			if (CAT.getValue().equals("Archive"))
				continue;
			Set<SignageContent> chans = ChannelCatalogFactory.getInstance().getChannelCatalog(CAT);
			int count = 0;
			for (SignageContent CHAN : chans) {
				if (CHAN instanceof ChannelInList) {
					allChannels.add((ChannelInList) CHAN);
					if (((ChannelInList) CHAN).getSequenceNumber() > count)
						count = ((ChannelInList) CHAN).getSequenceNumber() + 1;
				} else {
					allChannels.add(new ChannelInListImpl((Channel) CHAN, count, CHAN.getTime()));
					count += 1;
				}
			}
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
			return cac.getChannelClassification().getValue();

		}
	}

}