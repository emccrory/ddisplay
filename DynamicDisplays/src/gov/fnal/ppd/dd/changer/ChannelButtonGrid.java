/*
 * ChannelButtonGrid
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.lastDisplayChange;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DisplayButtonGroup;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Base class for the display grids that hold the channel buttons in the ChannelSelector
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public abstract class ChannelButtonGrid extends JPanel implements ActionListener {

	private static final long		serialVersionUID	= -3589107759837109844L;

	protected final Display			display;

	protected DisplayButtonGroup	bg;

	protected JComponent			expGrid;

	private static ReentrantLock	imBusy				= new ReentrantLock();

	/**
	 * @param cat
	 *            make a grid of channels of this category
	 */
	public abstract void makeGrid(ChannelCategory cat);

	/**
	 * @param cat
	 *            make a grid of channels of this category
	 * @param frameNumber
	 *            Modify the channels to appear on this frame number (if not zero)
	 */
	public abstract void makeGrid(ChannelCategory cat, int frameNumber);

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
		int wid = (SHOW_IN_WINDOW ? 1 : 2);
		p.setBorder(BorderFactory.createLineBorder(display.getPreferredHighlightColor(), wid));
		p.add(comp, BorderLayout.CENTER);
		return super.add((Component) p);
	}

	public void actionPerformed(ActionEvent e) {
		// System.err.println("Event " + e.getClass().getSimpleName() + " (" + e.getActionCommand() + ") received ");

		// Observation (10/10/2014): This method is called for every tab on the screen. So, (at this time) for a general
		// case, it is called 4 to 10 times, depending on the number of tabs of channels there are on the main screen.
		// So, there is a bit of a bug below, in that the buttons in the entire container bg are "enabled" or "disabled" each time,
		// over and over again.

		lastDisplayChange = System.currentTimeMillis();

		if (e instanceof DisplayChangeEvent) {
			DisplayChangeEvent ev = (DisplayChangeEvent) e;
			SignageContent to = null;
			switch (ev.getType()) {
			case CHANGE_RECEIVED:
				if (e.getSource() instanceof Display)
					to = ((Display) e.getSource()).getContent();
				else if (e.getSource() instanceof Channel)
					to = (Channel) e.getSource();
				else
					System.err.println(getClass().getSimpleName() + ": Unknown source " + e.getSource().getClass().getSimpleName());

				bg.disableAll(to);
				if (!SHOW_IN_WINDOW)
					if (imBusy.tryLock()) {
						// Throw up a dialog box saying,
						// "Well done!  You've changed the channel on display #5 to 'Something or other', URL='http://some.url'"
						if (e.getSource() instanceof Display) {
							Display di = (Display) e.getSource();
							new TemporaryDialogBox(this, di);
							imBusy.unlock();
						}
					} // else another instance is doing it so I don't have to
				break;

			case CHANGE_COMPLETED:
				bg.enableAll();
				break;

			case ALIVE:
				setAlive(true);
				break;

			case ERROR:
				// System.out.println("Got the error indication in ChannelButtonGrid");
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
		// FIXME !!!
		// Object o1 = getComponent(0);
		// if (o1 instanceof JPanel) {
		// JPanel outer = (JPanel) o1;
		// Object o2 = outer.getComponent(0); // This line fails with ArrayINdexOutOfBoundsException
		// if (o2 instanceof JPanel) {
		// JPanel expGrid = (JPanel) o2;
		// for (Component C : expGrid.getComponents())
		// if (C instanceof DDButton) {
		// if (((DDButton) C).isSelected())
		// return true;
		// }
		// } else if (o2 instanceof DDButton) {
		// for (Component C : outer.getComponents())
		// if (C instanceof DDButton) {
		// if (((DDButton) C).isSelected())
		// return true;
		// }
		// }
		// } else if (o1 instanceof JScrollPane) {
		// JViewport internal = (JViewport) ((JScrollPane) o1).getComponent(0);
		// JPanel c0 = (JPanel) internal.getComponent(0);
		// for (Component C : c0.getComponents())
		// if (C instanceof Box) {
		// JComponent c1 = (JComponent) ((Box) C).getComponent(0);
		// if (c1 instanceof DDButton)
		// if (((DDButton) c1).isSelected())
		// return true;
		// }
		// }

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
