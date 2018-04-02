package gov.fnal.ppd.dd.channel.list.table;

import gov.fnal.ppd.dd.channel.ChannelInList;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public abstract class AbstractChannelTableModel extends AbstractTableModel {

	private static final long			serialVersionUID	= 7906711135279253878L;

	private int[]								relativeWidths		= null;
	protected String[]					columnNames			= null;

	protected List<ChannelInList>	allChannels			= new ArrayList<ChannelInList>();

	/**
	 * @return the allChannels
	 */
	public List<ChannelInList> getAllChannels() {
		return allChannels;
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col].toString();
	}

	@Override
	public int getRowCount() {
		return allChannels.size();
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		// default is nothing can be edited
	}

	public int[] getRelativeWidths() {
		return relativeWidths;
	}

	public void setRelativeWidths(int[] relativeWidths) {
		this.relativeWidths = relativeWidths;
	}
}
