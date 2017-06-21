package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import gov.fnal.ppd.dd.channel.ChannelImage;
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
		JLabel myLabel = null;
		if (value instanceof ChannelImage) {
			ChannelImage chan = (ChannelImage) value;
			myLabel = new JLabel("<html><b>" + chan.getName() + "</b> <br>Experiment: " + chan.getExp() + ", Dwell time="
					+ chan.getTime() / 1000L + " sec<br>"
					+ chan.getURI().toASCIIString() + "</html>");
			// + chan.getURI().toASCIIString().replace(getFullURLPrefix() + "/portfolioOneSlide.php?photo=", "") + "</html>");
		} else {
			Channel chan = (Channel) value;
			myLabel = new JLabel("<html><b>" + chan.getName() + "</b> <br>Channel # " + chan.getNumber() + ", Category: "
					+ chan.getCategory() + ", Dwell time=" + chan.getTime() / 1000L + " sec<br>" + chan.getURI().toASCIIString()
					+ "</html>");
		}
		myLabel.setFont(new Font("Arial", Font.PLAIN, SHOW_IN_WINDOW ? 10 : 14));
		myLabel.setOpaque(true);

		if (isSelected) {
			myLabel.setBackground(table.getSelectionBackground());
		} else {
			myLabel.setBackground(table.getBackground());
		}
		return myLabel;
	}
}
