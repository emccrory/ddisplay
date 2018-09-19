package gov.fnal.ppd.dd.channel.list.table;

import static gov.fnal.ppd.dd.channel.list.ListUtilsGUI.getDwellStrings;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;

import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.util.BigButtonSpinner;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public class SelectedChannelsTableModel extends AbstractChannelTableModel implements ActionListener {

	private static final long serialVersionUID = 5736576268689815979L;

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
		ChannelInList cac = allChannels.get(row);
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
				ChannelInList chan = allChannels.get(row);
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

	public void edit(final int here, Component parent) {
		ChannelInList cil = allChannels.get(here);
		// I guess the only thing to edit is the dwell time.

		final SpinnerModel model = new SpinnerListModel(getDwellStrings());
		JSpinner time = BigButtonSpinner.create(model);
		time.setValue(new Long(cil.getTime() / 1000L));
		time.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		time.setPreferredSize(new Dimension(500, 100));

		JPanel p = new JPanel();

		p.add(time);
		p.add(Box.createRigidArea(new Dimension(10, 10)));
		p.add(new JLabel("Seconds"));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		Object[] options = { "Accept", "Cancel" };
		int result = JOptionPane.showOptionDialog(parent, p, "Change Dwell for Channel " + (here + 1) + ": \"" + cil.getName() + "\"",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		if (result == 0) {
			cil.setTime((Long) time.getModel().getValue() * 1000L);
		}
		fireTableDataChanged();
	}

	// May need this later
	// public Channel getRow(int row) {
	// return allChannels.get(row);
	// }
}