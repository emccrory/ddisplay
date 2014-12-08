package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.INSET_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DisplayButtonGroup;

import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Create a grid of the channels that represent "Detailed Information"
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class DetailedInformationGrid extends ChannelButtonGrid {

	private static final long	serialVersionUID	= 1983175217360627923L;

	private float				FS;

	private int					IS;

	/**
	 * @param display
	 *            The Display for which these buttons relate
	 * @param bg
	 *            The button group into which these buttons go
	 * @param set
	 */
	public DetailedInformationGrid(final Display display, final DisplayButtonGroup bg, final ChannelCategory set) {
		super(display, bg);
		add(makeExpGrid(set));
	}

	protected JComponent makeExpGrid(ChannelCategory set) {

		Set<SignageContent> list = ChannelCatalogFactory.getInstance().getChannelCatalog(set);
		int cols = 4;
		int gap = 5;
		int maxLen = 50;

		if (list.size() > 48) {
			FS = 0.5f * FONT_SIZE;
			IS = INSET_SIZE / 6;
			cols = 5;
			gap = 1;
			maxLen = 17;
		} else if (list.size() > 18) {
			FS = 0.6f * FONT_SIZE;
			IS = INSET_SIZE / 6;
			cols = 4;
			gap = 2;
			maxLen = 15;
		} else {
			switch (list.size()) {
			case 18:
			case 17:
			case 16:
			case 15:
				FS = 0.8f * FONT_SIZE;
				IS = INSET_SIZE / 6;
				cols = 3;
				gap = 2;
				maxLen = 13;
				break;

			case 14:
				FS = 0.75f * FONT_SIZE;
				IS = INSET_SIZE / 3;
				cols = 2;
				gap = 3;
				maxLen = 14;
				break;

			case 13:
			case 12:
			case 11:
			case 10:
			case 9:
				FS = 0.94f * FONT_SIZE;
				IS = INSET_SIZE / 3;
				cols = 2;
				gap = 2;
				maxLen = 13;
				break;
			case 8:
			case 7:
				FS = FONT_SIZE;
				IS = INSET_SIZE / 2;
				cols = 2;
				gap = 4;
				maxLen = 12;
				break;
			default:
				FS = 1.2f * FONT_SIZE;
				IS = INSET_SIZE;
				cols = 1;
				gap = 4;
				maxLen = 80;
				break;
			}
		}

		GridLayout g = new GridLayout(0, cols);
		if (!SHOW_IN_WINDOW) {
			g.setHgap(gap);
			g.setVgap(gap);
		}

		expGrid = new JPanel(g);
		expGrid.setOpaque(true);
		expGrid.setBackground(display.getPreferredHighlightColor());
		for (SignageContent exp : list) {
			final DDButton button = new DDButton((Channel) exp, display, maxLen);
			if (button.getText().toUpperCase().startsWith(set.getValue().toUpperCase()))
				button.setText(button.getText().substring(set.getValue().length() + 1)); // Assume trailing space
			
			if (button.getText().toUpperCase().startsWith(("<html><center>" + set.getValue()).toUpperCase()))
				button.setText("<html><center>" + button.getText().substring(set.getValue().length() + 15)); // Assume trailing space

			// if (SHOW_IN_WINDOW) {
			// button.setFont(button.getFont().deriveFont(FS));
			// } else {
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

			button.setMargin(new Insets(IS, IS, IS, IS));
			// }
			button.setSelected(exp.equals(display.getContent()));
			button.addActionListener(this); // Order is important for the GUI. This one goes last.
			button.addActionListener(display);

			bg.add(button);

			expGrid.add(button);
		}

		expGrid.setAlignmentX(JComponent.TOP_ALIGNMENT);
		return expGrid;
	}
}
