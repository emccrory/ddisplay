package gov.fnal.ppd.signage.util;

import static gov.fnal.ppd.GlobalVariables.PING_INTERVAL;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.changer.ChannelButtonGrid;
import gov.fnal.ppd.signage.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.signage.changer.DDButton;
import gov.fnal.ppd.signage.changer.DisplayButtons;
import gov.fnal.ppd.signage.display.DisplayListDatabaseRemote;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.swing.JLabel;

/**
 * Utility class for the main Channel Selector class.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 *
 */
public class CheckDisplayStatus extends Thread {

	private Display		display;
	private int			index;
	private JLabel	footer;
	private List<List<ChannelButtonGrid>>	grids;

	public CheckDisplayStatus(final Display display, final int index, final JLabel footer, final List<List<ChannelButtonGrid>> grids) {
		super("Display." + display.getNumber() + "." + display.getScreenNumber() + ".StatusUpdate");
		this.display = display;
		this.index = index;
		this.footer = footer;
		this.grids = grids;
	}

	public void run() {
		try {
			sleep(25 * index); // Put in a slight delay to get the Pings to each Display offset from each other a little
								// bit.
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Connection connection = DisplayListDatabaseRemote.getConnection();
		while (true) {
			try {
				sleep(PING_INTERVAL);
				// read from the database to see what's up with this Display

				String query = "SELECT Content,ContentName FROM DisplayStatus WHERE DisplayID=" + display.getNumber();
				// Use ARM (Automatic Resource Management) to assure that things get closed properly (a new Java 7
				// feature)
				try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query);) {
					if (rs.first()) { // Move to first returned row
						while (!rs.isAfterLast()) {
							InputStream cn = rs.getAsciiStream("Content");
							if (cn != null) {
								String contentName = ConnectionToDynamicDisplaysDatabase.makeString(cn);
								if (contentName.startsWith("0: "))
									contentName = contentName.substring(3);
								if (contentName.contains(" is being displayed"))
									contentName = contentName.substring(0, contentName.indexOf(" is being displayed"));
								// Create a new footer
								String text = "Disp " + display.getNumber() + ": " + (contentName);
								footer.setText(text);
								footer.setToolTipText(contentName);
								DisplayButtons.setToolTip(display);

								// Enable the Channel buttons, too
								for (List<ChannelButtonGrid> allGrids : grids)
									if (allGrids.get(0).getDisplay().getNumber() == display.getNumber())
										for (ChannelButtonGrid cbg : allGrids)
											for (int i = 0; i < cbg.getBg().getNumButtons(); i++) {
												DDButton myButton = cbg.getBg().getAButton(i);
												boolean selected = myButton.getChannel().toString().equals(contentName);
												myButton.setSelected(selected);
											}
							}
							rs.next();
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
