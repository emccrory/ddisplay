package gov.fnal.ppd.signage.changer;

import static gov.fnal.ppd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.GlobalVariables.INSET_SIZE;
import static gov.fnal.ppd.GlobalVariables.SHOW_IN_WINDOW;
import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.util.DisplayButtonGroup;

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
	public DetailedInformationGrid(final Display display, final DisplayButtonGroup bg, final int set) {
		super(display, bg);
		add(makeExpGrid(set));
	}

	protected JPanel makeExpGrid(int set) {
		ChannelCategory cat = ChannelCategory.MISCELLANEOUS;
		switch (set) {
		case 0:
			cat = ChannelCategory.PUBLIC;
			break;
		case 1:
			cat = ChannelCategory.PUBLIC_DETAILS;
			break;
		case 2:
			cat = ChannelCategory.EXPERIMENT_DETAILS;
			break;
		case 3:
			cat = ChannelCategory.NOVA_DETAILS;
			break;
		case 4:
			cat = ChannelCategory.NUMI_DETAILS;
			break;
		case 5:
			cat = ChannelCategory.VIDEOS;
			break;

		case 6:
			cat = ChannelCategory.ACCELERATOR;
			break;
		default:
			cat = ChannelCategory.MISCELLANEOUS;
			break;
		}

		Set<SignageContent> list = ChannelCatalogFactory.getInstance().getChannelCatalog(cat);
		int cols = 4;
		int gap = 5;
		int maxLen = 50;

		if (list.size() > 48) {
			FS = 0.5f * FONT_SIZE;
			IS = INSET_SIZE / 12;
			cols = 5;
			gap = 4;
			maxLen = 17;
		} else if (list.size() > 18) {
			FS = 0.6f * FONT_SIZE;
			IS = INSET_SIZE / 6;
			cols = 4;
			gap = 5;
			maxLen = 15;
		} else {
			switch (list.size()) {
			case 18:
			case 17:
			case 16:
			case 15:
				FS = 0.8f * FONT_SIZE;
				IS = INSET_SIZE / 4;
				cols = 3;
				gap = 7;
				maxLen = 13;
				break;

			case 14:
				FS = 0.75f * FONT_SIZE;
				IS = INSET_SIZE / 4;
				cols = 2;
				gap = 6;
				maxLen = 14;
				break;

			case 13:
			case 12:
			case 11:
			case 10:
			case 9:
				FS = 0.9f * FONT_SIZE;
				IS = INSET_SIZE / 3;
				cols = 2;
				gap = 8;
				maxLen = 13;
				break;
			case 8:
			case 7:
				FS = FONT_SIZE;
				IS = INSET_SIZE / 2;
				cols = 2;
				gap = 10;
				maxLen = 12;
				break;
			default:
				FS = 1.2f * FONT_SIZE;
				IS = INSET_SIZE;
				cols = 1;
				gap = 12;
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
