package gov.fnal.ppd.dd.channel.list;

import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.signage.Channel;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 * Hold a list of channels that will be sent to the display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public class SelectedChannelsDisplayAsTable extends JTable {

	private static final long					serialVersionUID	= -3233862506460879139L;
	private SelectedChannelsTableModel	model;

	/**
	 * 
	 */
	public SelectedChannelsDisplayAsTable() {
		super();

		model = new SelectedChannelsTableModel();
		setModel(model);
		TableColumnModel tcm = getColumnModel();
		for (int i = 0; i < (tcm.getColumnCount()); i++) {
			tcm.getColumn(i).setPreferredWidth(model.relativeWidths[i]);
		}

		setRowHeight(40);

		setDefaultRenderer(Channel.class, new ChannelCellRenderer());
	}

	/**
	 * @param oldChan
	 *            the channel to insert into this Table
	 * @param dwell
	 *            The dwell time for this channel in this list
	 */
	public void add(final Channel oldChan, long dwell) {
		// Make a copy of the old channel
		ChannelImpl newChannel = new ChannelImpl(oldChan.getName(), oldChan.getCategory(), oldChan.getDescription(),
				oldChan.getURI(), oldChan.getNumber(), 1000L * dwell);

		model.addChannel(newChannel);
		System.out.println("** Added channel [" + newChannel + "] with dwell of " + newChannel.getTime() + ", numRows = "
				+ model.getRowCount() + " - local row count: " + getRowCount());
		
	}

}
