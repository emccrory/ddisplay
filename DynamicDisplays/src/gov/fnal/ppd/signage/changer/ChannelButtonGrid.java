package gov.fnal.ppd.signage.changer;

import static gov.fnal.ppd.ChannelSelector.SHOW_IN_WINDOW;
import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.util.DisplayButtonGroup;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Base class for the display grids that hold the channel buttons in the ChannelSelector
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public abstract class ChannelButtonGrid extends JPanel implements ActionListener {

	private static final long		serialVersionUID	= -3589107759837109844L;

	protected final Display			display;

	protected DisplayButtonGroup	bg;

	protected JPanel				expGrid;

	/**
	 * @param display
	 * @param bg
	 */
	public ChannelButtonGrid(Display display, DisplayButtonGroup bg) {
		super(new BorderLayout());
		this.bg = bg;
		this.display = display;
		setAlive(false);
	}

	/**
	 * Override (sort of) the base class add so I can make it the right color
	 * 
	 * @param comp
	 * @return The component argument
	 */
	public Component add(JPanel comp) {
		JPanel p = new JPanel(new BorderLayout());
		int wid = (SHOW_IN_WINDOW ? 2 : 5);
		p.setBorder(BorderFactory.createLineBorder(display.getPreferredHighlightColor(), wid));

		p.add(comp, BorderLayout.CENTER);
		return super.add((Component) p);
	}

	public void actionPerformed(ActionEvent e) {
		// System.err.println("Event " + e.getClass().getSimpleName() + " (" + e.getActionCommand() + ") received ");
		if (e instanceof DisplayChangeEvent) {
			DisplayChangeEvent ev = (DisplayChangeEvent) e;
			switch (ev.getType()) {
			case CHANGE_RECEIVED:
				SignageContent to = null;
				if (e.getSource() instanceof Display)
					to = ((Display) e.getSource()).getContent();
				else if (e.getSource() instanceof Channel)
					to = (Channel) e.getSource();
				else
					System.err.println(getClass().getSimpleName() + ": Unknown source " + e.getSource().getClass().getSimpleName());

				bg.disableAll(to);
				break;
			case CHANGE_COMPLETED:
				bg.enableAll();
				break;
			case ALIVE:
				setAlive(true);
				break;

			case ERROR:
			case IDLE:
				// Ignore for now
				break;
			}
		} // else
			// System.err.println("Event " + e.getClass().getSimpleName() + " (" + e.getActionCommand() + ") from "
			// + e.getSource().getClass().getSimpleName() + " (" + ((JButton) e.getSource()).getText() + ") ignored");
	}

	/**
	 * Is the display associated with this panel alive, really?
	 * 
	 * @param b
	 */
	public void setAlive(boolean b) {
		if (b)
			bg.enableAll();
		else
			bg.disableAll(new Object());
	}

	/**
	 * @return Does this panel contain the selected channel?
	 */
	public boolean hasSelectedChannel() {
		JPanel outer = (JPanel) getComponent(0);
		JPanel expGrid = (JPanel) outer.getComponent(0);
		for (Component C : expGrid.getComponents())
			if (C instanceof DDButton) {
				if (((DDButton) C).isSelected())
					return true;
			}
		return false;
	}

	/**
	 * @return The Display associated with this grid
	 */
	public Display getDisplay() {
		return display;
	}

	/**
	 * Get the buttons that are controlled/owned by this grid
	 * 
	 * @return the button group
	 */
	public DisplayButtonGroup getBg() {
		return bg;
	}

	// public void addExitButton() {
	// if (expGrid != null) {
	// JButton exit = new JButton("Exit the GUI");
	// exit.addActionListener(new ActionListener() {
	//
	// @Override
	// public void actionPerformed(ActionEvent e) {
	// if (JOptionPane.showConfirmDialog(expGrid, "Do you _really_ want to exit the Channel Selector Application?",
	// "Exit?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
	// System.exit(0);
	// }
	// });
	//
	// Border bor = exit.getBorder();
	// exit.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED, 10), bor));
	// expGrid.add(exit);
	// }
	// }
}
