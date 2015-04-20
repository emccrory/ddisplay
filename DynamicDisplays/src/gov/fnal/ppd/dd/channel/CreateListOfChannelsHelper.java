/*
 * CreateListOfChannelsHelper
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import gov.fnal.ppd.dd.channel.CreateListOfChannels.BigButton;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
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
		accept = new BigButton("Accept This Channel List Sequence");
		JLabel instructions = new JLabel("<html><p align='center'>This page allows you to play a sequence of content on a Display.  " +
				"Create the sequence by selecting the Channels in order below.  Also, set the dwell time.<br>" +
				"Click \"Accept This Channel List Sequence\" and the Display will play these Channels in sequence.</html>");

		Box v = Box.createVerticalBox();
		v.add(instructions);
		JScrollPane scroller = new JScrollPane(lister, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroller.getVerticalScrollBar().setUnitIncrement(12);
		v.add(scroller);
		if (!SHOW_IN_WINDOW)
			scroller.getVerticalScrollBar().setPreferredSize(new Dimension(40, 0));
		listerPanel.add(v, BorderLayout.CENTER);
		listerPanel.add(accept, BorderLayout.NORTH);
	}
}