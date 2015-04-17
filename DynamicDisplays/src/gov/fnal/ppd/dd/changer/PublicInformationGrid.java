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
import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A grid of Channel buttons that represent the front page of the Public channel changer
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 * @deprecated -- Replaced completely by DetailedInformationGrid.
 */
public class PublicInformationGrid extends ChannelButtonGrid {

	private static final long	serialVersionUID		= -2796931031800511192L;
	private static final int	LONG_WAIT_OF_INACTIVITY	= 600;						// 5 minutes
	private int					seconds					= LONG_WAIT_OF_INACTIVITY;
	protected boolean			autoChannelChange		= false;

	/**
	 * @return if auto change channel is on or not
	 */
	public boolean isAutoChannelChange() {
		return autoChannelChange;
	}

	/**
	 * @param autoChannelChange
	 */
	public void setAutoChannelChange(final boolean autoChannelChange) {
		this.autoChannelChange = autoChannelChange;
	}

	/**
	 * @param display
	 *            The Display represented by this grid of channel buttons
	 * @param bg
	 *            The ButtonGroup into which these Channel buttons go
	 */
	public PublicInformationGrid(final Display display, final DisplayButtonGroup bg) {
		super(display, bg);
		add(makeExpGrid());
		new Thread("PubGridInactivity") {
			public void run() {
				while (autoChannelChange) {
					try {
						sleep(1000);
						if (seconds-- <= 0) {
							// Change the channel automatically!
							PublicInformationGrid.this.bg.next(); 
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	/**
	 * @return The panel that contains all the buttons
	 */
	public JComponent makeExpGrid() {
		GridLayout g = new GridLayout(0, 2);
		if (!SHOW_IN_WINDOW) {
			g.setHgap(10);
			g.setVgap(10);
		}
		expGrid = new JPanel(g);
		expGrid.setOpaque(true);
		expGrid.setBackground(display.getPreferredHighlightColor());
		for (SignageContent exp : ChannelCatalogFactory.getInstance().getChannelCatalog(ChannelCategory.PUBLIC)) {
			final DDButton button = new DDButton((Channel) exp, display, 60);
			if (SHOW_IN_WINDOW) {
				button.setFont(button.getFont().deriveFont(FONT_SIZE));
			} else {
				button.setFont(button.getFont().deriveFont(FONT_SIZE));
				button.setMargin(new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE, INSET_SIZE));
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

	@Override
	public void actionPerformed(ActionEvent ev) {
		try {
			if (display.getContent().getCategory() == ChannelCategory.PUBLIC)
				seconds = LONG_WAIT_OF_INACTIVITY;
		} catch (NullPointerException ex) {
			// IGNORE this exception -- we see it during startup when content is not specified.
		}
		super.actionPerformed(ev);
	}

	@Override
	public void makeGrid(ChannelCategory cat) {
		add(makeExpGrid());		
	}

	@Override
	public void makeGrid(ChannelCategory cat, int frameNumber) {
		// TODO Auto-generated method stub
		
	}
}
