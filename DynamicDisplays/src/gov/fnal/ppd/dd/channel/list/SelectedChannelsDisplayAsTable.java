package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.channel.ChannelImpl;
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
	public void add(final Channel oldChan, long dwell) {
		// Make a copy of the old channel
		ChannelImpl newChannel = new ChannelImpl(oldChan.getName(), oldChan.getCategory(), oldChan.getDescription(),
				oldChan.getURI(), oldChan.getNumber(), 1000L * dwell);

		model.addChannel(newChannel);
		println(getClass(), "Added channel to new list: [" + newChannel + "], dwell=" + newChannel.getTime()/1000L + " sec. Number of channels in list: "
				+ model.getRowCount() + " (" + getRowCount() + ")");
	}

	/**
	 * @return The current list of channels held in this table
	 */
	public List<Channel> getChannelList() {
		return model.allChannels;
	}
}
