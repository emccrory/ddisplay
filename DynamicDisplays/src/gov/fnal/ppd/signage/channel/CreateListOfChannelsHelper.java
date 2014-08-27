package gov.fnal.ppd.signage.channel;

import static gov.fnal.ppd.GlobalVariables.SHOW_IN_WINDOW;
import gov.fnal.ppd.signage.channel.CreateListOfChannels.BigButton;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 *
 */
public class CreateListOfChannelsHelper {
	/**
	 * The object to hold the list of channels
	 */
	public CreateListOfChannels	lister;
	/**
	 * The panel that shows the list of channels
	 */
	public JPanel				listerPanel;
	/**
	 * A button to launch the list of channels
	 */
	public JButton				accept;

	/**
	 * Construct this helper
	 */
	public CreateListOfChannelsHelper() {
		listerPanel = new JPanel(new BorderLayout());
		lister = new CreateListOfChannels();
		accept = new BigButton("Accept This Channel List");

		JScrollPane scroller = new JScrollPane(lister, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroller.getVerticalScrollBar().setUnitIncrement(12);
		if (!SHOW_IN_WINDOW)
			scroller.getVerticalScrollBar().setPreferredSize(new Dimension(40, 0));
		listerPanel.add(scroller, BorderLayout.CENTER);
		listerPanel.add(accept, BorderLayout.NORTH);
	}
}