package gov.fnal.ppd.dd.channel.list.table;

import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.signage.Channel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public class SelectedChannelsTableModel extends AbstractChannelTableModel implements ActionListener {

	private static final long	serialVersionUID	= 5736576268689815979L;

	/**
	 * 
	 */
	public SelectedChannelsTableModel() {
		setRelativeWidths(new int[] { 20, 40, 400, 50 });
		columnNames = new String[] { "Seq", "Chan#", "Channel / URL", "Dwell", };
	}

	@Override
	public Class<? extends Object> getColumnClass(int c) {
		switch (c) {

		case 2:
			return Channel.class;
		default:
			return Long.class;
		}
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return col == 3 || col == 0;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (row < 0)
			return null;
		ChannelInList cac = (ChannelInList) allChannels.get(row);
		switch (col) {

		case 0:
			return cac.getSequenceNumber();
		case 1:
			return cac.getNumber();
		case 2:
			return cac;
		case 3:
			return cac.getTime() / 1000L;
		default:
			return 0; // FIXME Is this relevant?

		}
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		try {
			if (col == 0) {
				ChannelInList chan = (ChannelInList) allChannels.get(row);
				chan.setSequenceNumber(((Long) value).intValue());
			} else if (col == 3) {
				Channel chan = allChannels.get(row); // It has to be re-mapped already
				chan.setTime(1000L * ((Long) value));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param cac
	 *            The row to add
	 */
	public void addChannel(final ChannelInList cac) {
		allChannels.add(cac);
		fireTableDataChanged();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("Event " + e);
	}

	/**
	 * @param here
	 * @param there
	 * @return if a swap was made
	 */
	public boolean swap(final int here, final int there) {
		if (there < 0 || there >= allChannels.size())
			return false;
		Collections.swap(allChannels, here, there);
		fireTableDataChanged();
		return true;
	}

	/**
	 * @param here
	 * @return if something was deleted
	 */
	public boolean delete(final int here) {
		if (here >= 0 && here < allChannels.size()) {
			allChannels.remove(here);
			fireTableDataChanged();
			return true;
		}
		return false;
	}

	/**
	 * Delete all the rows from this table
	 */
	public void reset() {
		allChannels.clear();
		fireTableDataChanged();
	}

	/**
	 * Remove all the rows from this list
	 */
	public void clear() {
		allChannels.clear();
		fireTableDataChanged();
	}

	// May need this later
	// public Channel getRow(int row) {
	// return allChannels.get(row);
	// }
}