/*
 * DetailedInformationGrid
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.specific.DisplayButtonGroup;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Create a grid of the channels for the displays in the Dynamic Displays system
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class DetailedInformationGrid extends ChannelButtonGrid {

	private static final long	serialVersionUID	= 1983175217360627923L;

	private float				FS;

	/**
	 * @param display
	 *            The Display for which these buttons relate
	 * @param bg
	 *            The button group into which these buttons go
	 */
	public DetailedInformationGrid(final Display display, final DisplayButtonGroup bg) {
		super(display, bg);
	}

	public void makeGrid(final ChannelClassification set) {
		add(makeExpGrid(set));
	}

	protected JComponent makeExpGrid(final ChannelClassification set) {

		Set<SignageContent> list = ChannelCatalogFactory.getInstance().getChannelCatalog(set);
		int cols = 4;
		// int gap = 5;
		int maxLen = 50;

		if (list.size() > 40) {
			FS = 0.4f * FONT_SIZE;
			cols = 5;
			maxLen = 15;
		} else if (list.size() > 32) {
			FS = 0.5f * FONT_SIZE;
			cols = 4;
			maxLen = 17;
		} else if (list.size() > 18) {
			FS = 0.6f * FONT_SIZE;
			cols = 3;
			maxLen = 15;
		} else {
			switch (list.size()) {
			case 18:
			case 17:
			case 16:
				FS = 0.6f * FONT_SIZE;
				cols = 3;
				maxLen = 13;
				break;

			case 15:
			case 14:
			case 13:
				FS = 0.75f * FONT_SIZE;
				cols = 3;
				maxLen = 14;
				break;

			case 12:
			case 11:
				FS = 0.9f * FONT_SIZE;
				cols = 2;
				maxLen = 14;
				break;

			case 10:
			case 9:
			case 8:
				FS = 0.94f * FONT_SIZE;
				cols = 2;
				maxLen = 13;
				break;

			case 7:
			case 6:
				FS = FONT_SIZE;
				cols = 1;
				maxLen = 90;
				break;

			default:
				FS = 1.2f * FONT_SIZE;
				cols = 1;
				maxLen = 80;
				break;
			}
		}

		GridLayout g = new GridLayout(0, cols);

		expGrid = new JPanel(g);
		expGrid.setOpaque(true);
		expGrid.setBackground(display.getPreferredHighlightColor());
		int numButtons = 0;
		for (SignageContent exp : list) {
			numButtons++;
			final DDButton button = new DDButton((Channel) exp, display, maxLen);

			// TODO -- Move this title manipulation down into DDButton so it can break up the title into two rows properly
			if (button.getText().toUpperCase().startsWith(set.getValue().toUpperCase()))
				button.setText(button.getText().substring(set.getValue().length() + 1)); // Assume trailing space

			if (button.getText().toUpperCase().startsWith(("<html><center>" + set.getValue()).toUpperCase()))
				button.setText("<html><center>" + button.getText().substring(set.getValue().length() + 15));
			if (SHOW_IN_WINDOW) {
				button.setFont(button.getFont().deriveFont(FS));
			} else {
				switch (button.numLinesInTitle()) {
				case 2:
					button.setFont(button.getFont().deriveFont(FS * 0.8300f));
					break;
				case 3:
					button.setFont(button.getFont().deriveFont(FS * 0.7000f));
					break;
				default:
					if (cols == 2 && button.getText().length() > 15)
						button.setFont(button.getFont().deriveFont(FS * 0.8500f));
					else
						button.setFont(button.getFont().deriveFont(FS));
					break;
				}

				// button.setMargin(new Insets(IS, IS, IS, IS));
			}
			button.setSelected(exp.equals(display.getContent()));
			button.addActionListener(this); // Order is important for the GUI. This one goes last.
			button.addActionListener(display);

			bg.add(button);

			expGrid.add(button);
		}
		for (; numButtons < 5; numButtons++) {
			expGrid.add(Box.createRigidArea(new Dimension(1, 1)));
		}

		expGrid.setAlignmentX(JComponent.TOP_ALIGNMENT);
		return expGrid;
	}
}
