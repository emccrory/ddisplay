package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelInListImpl;
import gov.fnal.ppd.dd.channel.list.table.ChannelCellRenderer;
import gov.fnal.ppd.dd.channel.list.table.SelectedChannelsTableModel;
import gov.fnal.ppd.dd.signage.Channel;

/**
 * Hold a list of channels that will be sent to the display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2017-18
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
			tcm.getColumn(i).setPreferredWidth(model.getRelativeWidths()[i]);
		}

		setRowHeight((SHOW_IN_WINDOW ? 50 : 70 ));

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
		int seq = 1 + model.getLargestSequence();
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
		ChannelInList newChannel = new ChannelInListImpl(oldChan.getName(), oldChan.getChannelClassification(), oldChan.getDescription(),
				oldChan.getURI(), oldChan.getNumber(), 1000L * dwell);
		newChannel.setSequenceNumber(sequence);

		println(getClass(), "Adding channel to new list: [" + newChannel + "], dwell=" + newChannel.getTime() / 1000L
				+ " sec. Number of channels in list: " + model.getRowCount() + " (" + getRowCount() + ")");
		model.addChannel(newChannel);
	}

	/**
	 * @return The current list of channels held in this table
	 */
	public List<ChannelInList> getChannelList() {
		return model.getAllChannels();
	}

	public String getTotalTime() {
		long sum = 0;
		for (ChannelInList C : model.getAllChannels())
			sum += C.getTime();
		
		long h = sum / (long) ONE_HOUR;
		long m = (sum % (long) ONE_HOUR) / 60000L;
		long s = sum % (long) ONE_MINUTE / 1000L;
		
		return h + ":" + (m < 10 ? "0": "") + m + ":" + (s < 10 ? "0" : "") + s;
	}
}
