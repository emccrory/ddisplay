package gov.fnal.ppd.dd.channel.list;

import gov.fnal.ppd.dd.signage.Channel;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public class ChannelCellRenderer implements TableCellRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
			int column) {
		Channel chan = (Channel) value;
		JLabel myLabel = new JLabel("<html><b>" + chan.getName() + "</b> <br>Channel # " + chan.getNumber() + ", Category: "
				+ chan.getCategory() + ", Dwell time=" + chan.getTime() / 1000L + " sec<br>" + chan.getURI().toASCIIString()
				+ "</html>");
		myLabel.setFont(new Font("Arial", Font.PLAIN, 10));
		myLabel.setOpaque(true);
		if (isSelected) {
			myLabel.setBackground(table.getSelectionBackground());
		} else {
			myLabel.setBackground(table.getBackground());
		}
		return myLabel;
	}
}
