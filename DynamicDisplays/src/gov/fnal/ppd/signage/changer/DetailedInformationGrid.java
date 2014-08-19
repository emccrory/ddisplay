package gov.fnal.ppd.signage.changer;

import static gov.fnal.ppd.ChannelSelector.FONT_SIZE;
import static gov.fnal.ppd.ChannelSelector.INSET_SIZE;
import static gov.fnal.ppd.ChannelSelector.SHOW_IN_WINDOW;
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
 * @copy 2014
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
		default:
			cat = ChannelCategory.MISCELLANEOUS;
			break;
		}

		Set<SignageContent> list = ChannelCatalogFactory.getInstance().getChannelCatalog(cat);
		int cols = 4;
		int gap = 5;

		if (list.size() > 48) {
			FS = 0.5f * FONT_SIZE;
			IS = INSET_SIZE / 3;
			cols = 5;
			gap = 4;
		} else if (list.size() > 18) {
			FS = 0.6f * FONT_SIZE;
			IS = INSET_SIZE / 2;
			cols = 4;
			gap = 5;
		} else if (list.size() > 10) {
			FS = 0.75f * FONT_SIZE;
			IS = INSET_SIZE / 2;
			cols = 3;
			gap = 7;
		} else if (list.size() > 6) {
			FS = 1.2f * FONT_SIZE;
			IS = INSET_SIZE / 2;
			cols = 2;
			gap = 10;
		} else {
			FS = 1.5f * FONT_SIZE;
			IS = INSET_SIZE;
			cols = 1;
			gap = 12;
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
			final DDButton button = new DDButton((Channel) exp, display);
			if (SHOW_IN_WINDOW) {
				button.setFont(button.getFont().deriveFont(FS));
			} else {
				button.setFont(button.getFont().deriveFont(FS));
				button.setMargin(new Insets(IS, IS, IS, IS));
			}
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
