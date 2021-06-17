/*
 * DisplayButtonGroup
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.specific;

import gov.fnal.ppd.dd.changer.DDButton;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of a button group for the Fermilab Dynamic Displays system. All buttons are of type DDButton
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class DisplayButtonGroup implements ActionListener {
	private static int		objCount				= 0;

	static long				CheckStatusSleepTime	= 5000;
	private boolean			play					= false;
	private List<DDButton>	buttons					= new ArrayList<DDButton>();
	private Display			display;
	private ActionListener	listener;

	/**
	 * @param display
	 *            The Display that owns these buttons
	 */
	public DisplayButtonGroup(final Display display) {
		this();
		this.display = display;
	}

	/**
	 * @param L
	 */
	public void setActionListener(final ActionListener L) {
		listener = L;
	}

	/**
	 * Create a group of DDButtons.
	 */
	public DisplayButtonGroup() {
		new Thread("ButtonGroupCheck." + (objCount++)) {

			public void run() {
				while (true) {
					try {
						sleep(CheckStatusSleepTime);
						if (play)
							synchronized (buttons) {
								for (int i = 0; i < buttons.size(); i++) {
									if (buttons.get(i).isSelected()) {
										int sel = (i + 1) % buttons.size();
										for (int j = 0; j < buttons.size(); j++)
											buttons.get(j).setSelected(j == sel);
										break;
									}
								}
							}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	/**
	 * @param b
	 *            The DDButton to add to this button group
	 */
	public void add(final DDButton b) {
		synchronized (buttons) {
			b.addActionListener(this);
			buttons.add(b);
		}
	}

	/**
	 * Whenever a button is pressed, this group learns of it here. It then propagates that click to all registered listeners
	 */
	public void actionPerformed(ActionEvent ev) {
		if (display != null)
			display.setContent(((DDButton) ev.getSource()).getChannel());
		synchronized (buttons) {
			for (DDButton b : buttons)
				b.setSelected(b.equals(ev.getSource()));
			if (listener != null)
				listener.actionPerformed(ev);
		}
	}

	/**
	 * @return The Display that owns this Button Group
	 */
	public Display getDisplay() {
		return display;
	}

	/**
	 * @param object
	 */
	public void disableAll(final Object object) {
		Channel c = null;
		if (object instanceof Channel)
			c = ((Channel) object);

		// if (display != null)
		// System.out.println("Setting Grid " + getClass().getSimpleName() + " (Display='" + display.getLocation()
		// + "') to not-responding");
		// else if (c != null)
		// System.out.println("Setting Grid " + getClass().getSimpleName() + " (Channel='" + c.getName() + "') to not-responding");

		synchronized (buttons) {
			for (DDButton B : buttons) {
				if (c != null)
					B.setSelected(B.getChannel().equals(c));
				B.setEnabled(false);
				B.setEnabled(false);
			}
		}
	}

	@SuppressWarnings("javadoc")
	public synchronized void enableAll() {
		synchronized (buttons) {
			for (DDButton B : buttons) {
				B.setEnabled(true);
				B.setEnabled(true); // Why two???
			}
		}
	}

	/**
	 * @return The number of buttons in this group
	 */
	public int getNumButtons() {
		synchronized (buttons) {
			return buttons.size();
		}
	}

	/**
	 * @param index
	 *            The button to return
	 * @return A particular button
	 */
	public DDButton getAButton(final int index) {
		synchronized (buttons) {
			return buttons.get(index);
		}
	}
}
