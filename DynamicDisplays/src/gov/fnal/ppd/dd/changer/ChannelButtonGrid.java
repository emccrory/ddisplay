/*
 * ChannelButtonGrid
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.userHasDoneSomething;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.specific.DisplayButtonGroup;

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

	// private static ReentrantLock imBusy = new ReentrantLock();

	/**
	 * @param catget
	 *            make a grid of channels of this category
	 */
	public abstract void makeGrid(ChannelClassification cat);

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
		return super.add(p);
	}

	public void actionPerformed(ActionEvent e) {
		// Observation (10/10/2014): This method is called for every tab on the screen. So, (at this time) for a general
		// case, it is called many, many times, depending on the number of tabs of channels there are on the main screen.
		// So, there is a bit of a bug below, in that the buttons in the entire container bg are "enabled" or "disabled" each time,
		// over and over again. Also, the TemporaryDialogBox could be called lots of times (but the ReentrantLock attribute is
		// used to block the other entries).

		userHasDoneSomething();

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

				// The feedback to the user that the channel changed is now done correctly - it waits for the reply message!
				// // This "imBusy" prevents the other times this is called (see bug note, above) from getting a TemporaryDialogBox.
				// if (imBusy.tryLock()) {
				// // Throw up a dialog box saying, "Well done! You've changed the channel on display #5 to
				// // 'Something or other', URL='http://some.url'"
				//
				// if (e.getSource() instanceof Display) {
				// Display di = (Display) e.getSource();
				// new TemporaryDialogBox(this, di);
				//
				// // Sleep here to assure that no other entries can call up this dialog. This is necessary most of the time.
				// // When this delay was 10msec, I'd get multiple dialog boxes up from time to time.
				// catchSleep(100);
				//
				// imBusy.unlock();
				// } // Question: Is this ever NOT an instance of Display?
				// } // else another instance is doing it so I don't have to

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
	 * @return Does this panel contain the selected channel?  This worked in the early days of the system, but not anymore.
	 * and it really is not necessary.
	 */
	public boolean hasSelectedChannel() {
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
}
