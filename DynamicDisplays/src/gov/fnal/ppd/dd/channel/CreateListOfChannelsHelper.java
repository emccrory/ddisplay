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
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
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
		JButton hide = new JButton("S/R & Help");
		hide.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
		//hide.setMargin(new Insets(2, 2, 2, 2));

		JComponent sr = lister.getSaveRestore();
		sr.setAlignmentX(JComponent.CENTER_ALIGNMENT);

		accept = new BigButton("Send this Channel List Sequence to the Display");
		if (SHOW_IN_WINDOW) {
			accept.setFont(accept.getFont().deriveFont(20.0f));
		}
		accept.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		final JLabel instructions = new JLabel(
				"<html><p align='center'>This page allows you to play a sequence of Channels on a Display.<br>"
						+ "Create the sequence by selecting the Channels in order below.  Also, set the dwell time.<br>"
						+ "Click \"Accept This Channel List Sequence\" (above) and the Display will play these Channels in sequence.</p></html>",
				JLabel.CENTER);

		instructions.setFont(instructions.getFont().deriveFont(Font.PLAIN, 11));
		instructions.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		instructions.setVisible(false);
		Box h = Box.createHorizontalBox();
		h.add(Box.createHorizontalGlue());
		h.add(accept);
		
		Dimension minSize = new Dimension(5, 5);
		Dimension prefSize = new Dimension(5, 5);
		Dimension maxSize = new Dimension(Short.MAX_VALUE, 5);
		h.add(new Box.Filler(minSize, prefSize, maxSize));

		h.add(hide);
		Box v = Box.createVerticalBox();
		v.add(h);
		v.add(instructions);
		v.add(sr);
		v.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		JScrollPane scroller = new JScrollPane(lister, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroller.getVerticalScrollBar().setUnitIncrement(12);
		scroller.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		listerPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		if (!SHOW_IN_WINDOW)
			scroller.getVerticalScrollBar().setPreferredSize(new Dimension(40, 0));
		listerPanel.add(scroller, BorderLayout.CENTER);
		listerPanel.add(v, BorderLayout.NORTH);

		hide.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				boolean visible = instructions.isVisible();
				instructions.setVisible(!visible);
				lister.getSaveRestore().setVisible(!visible);
			}
		});
		lister.getSaveRestore().setVisible(instructions.isVisible());
	}
}
