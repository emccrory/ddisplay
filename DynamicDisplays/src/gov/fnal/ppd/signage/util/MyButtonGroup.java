package gov.fnal.ppd.signage.util;

import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.changer.ChannelCategory;
import gov.fnal.ppd.signage.changer.MyButton;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple implementation of a button group, assuming buttons of type MyButton, which shows buttons as selected
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class MyButtonGroup implements ActionListener {
	private static int		objCount	= 0;

	static long				SleepTime	= 5000;
	private boolean			play		= false;
	private List<MyButton>	buttons		= new ArrayList<MyButton>();
	private Display			display;
	private ActionListener	listener;

	/**
	 * @param display
	 *            The Display that owns these buttons
	 */
	public MyButtonGroup(final Display display) {
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
	 * Create a group
	 */
	public MyButtonGroup() {
		new Thread("ButtonGroupCheck." + (objCount++)) {

			public void run() {
				while (true) {
					try {
						sleep(SleepTime);
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
					}
				}
			}
		}.start();
	}

	/**
	 * @param b
	 *            The MyButton to add to this button group
	 */
	public void add(final MyButton b) {
		synchronized (buttons) {
			b.addActionListener(this);
			buttons.add(b);
			// b.setSelected(buttons.size() == 1);
		}
	}

	public void actionPerformed(ActionEvent ev) {
		if (display != null)
			display.setContent(((MyButton) ev.getSource()).getChannel());
		synchronized (buttons) {
			for (MyButton b : buttons)
				b.setSelected(b.equals(ev.getSource()));
			if (listener != null)
				listener.actionPerformed(ev);
		}
	}

	// public void setPlay() {
	// play = true;
	// }
	//
	// public void setStop() {
	// play = false;
	// }

	/**
	 * @return The Display that owns this Button Group
	 */
	public Display getDisplay() {
		return display;
	}

	// public SignageContent getSelectedChannel() {
	// if (display != null)
	// return display.getContent();
	// return null;
	// }

	/**
	 * @param object
	 */
	public void disableAll(final Object object) {

		Channel c = null;
		if (object instanceof Channel)
			c = ((Channel) object);

		if (display != null)
			System.out.println("Setting Grid " + getClass().getSimpleName() + " (Display='" + display.getLocation()
					+ "') to not-responding");
		else if (c != null)
			System.out.println("Setting Grid " + getClass().getSimpleName() + " (Channel='" + c.getName() + "') to not-responding");

		synchronized (buttons) {
			for (MyButton B : buttons) {
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
			for (MyButton B : buttons) {
				B.setEnabled(true);
				B.setEnabled(true); // Why two???
			}
		}
	}

	/**
	 * In some situations the channel changer can opt to change the channel automatically, for example, for a public display that
	 * has not had a channel change in a while. This method makes this happen.
	 */
	public void next() {
		synchronized (buttons) {
			for (int i = 0; i < buttons.size(); i++) {
				if (buttons.get(i).isSelected()) {
					int sel = (i + 1) % buttons.size();
					if (buttons.get(sel).getChannel().getCategory() != ChannelCategory.PUBLIC) {
						// Rats! have to find the next Public Channel
						for (; sel < buttons.size() && buttons.get(sel).getChannel().getCategory() != ChannelCategory.PUBLIC; sel++)
							; // Just keep looking
						if (sel >= buttons.size()) {
							for (sel = 0; sel < i && buttons.get(sel).getChannel().getCategory() != ChannelCategory.PUBLIC; sel++)
								; // Just keep looking
						}
					}
					for (int j = 0; j < buttons.size(); j++)
						buttons.get(j).setSelected(j == sel);
					buttons.get(sel).doClick();
					break;
				}
			}
		}
	}

	public int getNumButtons() {
		synchronized (buttons) {
			return buttons.size();
		}
	}

	public MyButton getAButton(int index) {
		synchronized (buttons) {
			return buttons.get(index);
		}
	}
}
