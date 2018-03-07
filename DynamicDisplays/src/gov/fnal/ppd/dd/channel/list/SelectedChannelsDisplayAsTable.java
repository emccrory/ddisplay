package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.signage.Channel;

import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 * Hold a list of channels that will be sent to the display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public class SelectedChannelsDisplayAsTable extends JTable {

	private static final long			serialVersionUID	= -3233862506460879139L;
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

		setRowHeight((SHOW_IN_WINDOW ? 40 : 60));

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		setDefaultRenderer(Number.class, centerRenderer);

		setDefaultRenderer(Channel.class, new ChannelCellRenderer());
	}

	/**
	 * @param oldChan
	 *            the channel to insert into this Table
	 * @param dwell
	 *            The dwell time for this channel in this list
	 */
	public void add(final Channel oldChan, final long dwell) {
		int seq = 1 + model.allChannels.size();
		add(oldChan, seq, dwell);
	}

	/**
	 * @param oldChan
	 *            the channel to insert into this Table
	 * @param sequence
	 *            Where to put this channel in the list
	 * @param dwell
	 *            The dwell time for this channel in this list
	 */
	public void add(final Channel oldChan, int sequence, long dwell) {
		// Make a copy of the old channel
		ChannelInList newChannel = new ChannelInList(oldChan.getName(), oldChan.getCategory(), oldChan.getDescription(),
				oldChan.getURI(), oldChan.getNumber(), 1000L * dwell);

		println(getClass(), "Adding channel to new list: [" + newChannel + "], dwell=" + newChannel.getTime() / 1000L
				+ " sec. Number of channels in list: " + model.getRowCount() + " (" + getRowCount() + ")");
		model.addChannel(newChannel);
	}

	/**
	 * @return The current list of channels held in this table
	 */
	public List<ChannelInList> getChannelList() {
		return model.allChannels;
	}
}
