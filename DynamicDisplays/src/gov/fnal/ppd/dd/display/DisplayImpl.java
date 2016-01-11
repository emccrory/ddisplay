/*
 * DisplayImpl
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.display;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_EXTENDED_DISPLAY_NAMES;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_VIRTUAL_DISPLAY_NUMS;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.makeEmptyChannel;
import gov.fnal.ppd.dd.changer.DDButton;
import gov.fnal.ppd.dd.changer.DisplayChangeEvent;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple implementation of a Display
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public abstract class DisplayImpl implements Display {

	private static int				displayCount		= 1;
	// private int displayNumber = displayCount++;

	private int						dbDisplayNumber;
	private int						vDisplayNumber		= displayCount++;

	protected int					screenNumber		= 0;
	protected static AtomicInteger	internalThreadID	= new AtomicInteger(0);
	protected SignageContent		channel;
	protected SignageContent		previousChannel		= null;
	protected Color					highlightColor;
	protected List<ActionListener>	listeners			= new ArrayList<ActionListener>();
	private SignageType				category			= SignageType.Public;
	private InetAddress				ipAddress;
	private String					location;

	// protected TimerTask heartbeatThread = new TimerTask() {
	// public void run() {
	// informListeners(DisplayChangeEvent.Type.ALIVE);
	// }
	// };
	// protected String myName;

	/**
	 * @param ipName
	 * @param vDisplay
	 * @param dbDisplay
	 * @param screenNumber
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayImpl(final String ipName, final int vDisplay, final int dbDisplay, final int screenNumber, final String location,
			final Color color, final SignageType type) {
		assert (screenNumber >= 0);
		assert (vDisplay >= 0);
		assert (dbDisplay >= 0);

		// myName = ipName + ":" + screenNumber + " (" + displayNumber + ")";

		try {
			this.ipAddress = InetAddress.getByName(ipName);
		} catch (UnknownHostException e) {
			System.err.println("Host not found: " + ipName);
		}
		this.screenNumber = screenNumber;
		this.location = location;
		this.dbDisplayNumber = dbDisplay;
		this.vDisplayNumber = vDisplay;
		this.highlightColor = color;
		this.category = type;
		this.channel = makeEmptyChannel(null);
	}

	protected abstract boolean localSetContent();

	@Override
	public SignageContent setContent(SignageContent c) {
		if (getCategory().isVisible(c) && isVerifiedChannel(c)) {
			previousChannel = channel;
			if (c == null)
				channel = makeEmptyChannel(null);
			else
				channel = (Channel) c;
			System.out.println(getClass().getSimpleName() + ": Display " + getVirtualDisplayNumber() + " changed to [" + channel
					+ "] at " + (new Date()));

			// ----- Do we put this channel in a wrapper?
			// channel.setCode(channel.getCode() | 1);

			informListeners(DisplayChangeEvent.Type.CHANGE_RECEIVED, null);
			if (localSetContent())
				new Thread("FakeChangeComplete") {
					long	sleepTime	= (SHOW_IN_WINDOW ? 1000 : 5000);

					public void run() {
						// TODO -- This event needs to be triggered by the display actually indicating that it has changed the
						// channel.
						catchSleep(sleepTime);
						informListeners(DisplayChangeEvent.Type.CHANGE_COMPLETED, "Channel change succeeded");
					}
				}.start();
			else {
				informListeners(DisplayChangeEvent.Type.ERROR, "Channel change unsuccessful");
			}
			return previousChannel;
		}

		error("Content type of this Channel (" + c.getType() + ") is not appropriate for this Display (" + getCategory() + ")");
		return channel;
	}

	/**
	 * Override if this is something you need to check
	 * 
	 * @param c
	 * @return
	 */
	protected boolean isVerifiedChannel(SignageContent c) {
		return true;
	}

	/**
	 * A necessary mechanism to set the content when something strange is going on in the derived class. *Use sparingly*
	 * 
	 * @param c
	 *            the new content
	 */
	protected void setContentBypass(SignageContent c) {
		channel = c;
	}

	protected void error(String string) {
		System.err.println(string);
	}

	@Override
	public String toString() {
		if (SHOW_EXTENDED_DISPLAY_NAMES) {
			return "<html>Display " + getVirtualDisplayNumber()  + " | " + getDBDisplayNumber() + "<br>" + getLocation() + "</html>";
		} else {
			int displayNumber = getDBDisplayNumber();
			if (SHOW_VIRTUAL_DISPLAY_NUMS)
				displayNumber = getVirtualDisplayNumber();

			// FIXME Only works correctly up to 9999 total displays. That should be enough for now. :-)
			if (displayCount >= 1000) {
				if (displayNumber < 10)
					return "Display 000" + displayNumber;
				else if (displayNumber < 100)
					return "Display 00" + displayNumber;
				else if (displayNumber < 1000)
					return "Display 0" + displayNumber;

			} else if (displayCount >= 100) {
				if (displayNumber < 10)
					return "Display 00" + displayNumber;
				else if (displayNumber < 100)
					return "Display 0" + displayNumber;

			} else if (displayCount >= 10) {
				if (displayNumber < 10)
					return "Display 0" + displayNumber;
			}

			return "Display " + displayNumber;
		}
	}

	// @Override
	// public int getNumber() {
	// if (SHOW_VIRTUAL_DISPLAY_NUMS)
	// return getVirtualDisplayNumber();
	// return getDBDisplayNumber();
	// }

	@Override
	public int getDBDisplayNumber() {
		return dbDisplayNumber;
	}

	@Override
	public void setDBDisplayNumber(final int d) {
		dbDisplayNumber = d;
	}

	@Override
	public void setVirtualDisplayNumber(final int d) {
		vDisplayNumber = d;
	}

	@Override
	public int getVirtualDisplayNumber() {
		return vDisplayNumber;
	}

	@Override
	public Color getPreferredHighlightColor() {
		return highlightColor;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof DDButton) {
			setContent(((DDButton) e.getSource()).getChannel());
		} else
			throw new RuntimeException("Unexpected source: " + e.getSource().getClass().getCanonicalName());
	}

	// protected void informListeners(final ActionEvent e) {
	// new Thread("inform") {
	// public void run() {
	// try {
	// informListeners(e.getID(), DisplayChangeEvent.Type.CHANGE_RECEIVED);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	// }.start();
	// }

	protected void informListeners(final DisplayChangeEvent.Type t, String more) {
		final DisplayChangeEvent ev = new DisplayChangeEvent(this, internalThreadID.getAndIncrement(), t, more);
		for (final ActionListener L : listeners)
			new Thread("DisplayInform" + t + "_" + L.getClass().getSimpleName()) {
				public void run() {
					// System.out.println(DisplayImpl.this.getClass().getSimpleName() + ": " + t + "  sent to a "
					// + L.getClass().getName() + " (" + L.hashCode() + ")");
					L.actionPerformed(ev);
				}
			}.start();
	}

	@Override
	public void addListener(ActionListener listener) {
		if (listener != null)
			this.listeners.add(listener);
	}

	@Override
	public void errorHandler(String errorMessage) {
		informListeners(DisplayChangeEvent.Type.ERROR, errorMessage);
	}

	@Override
	public String getDescription() {
		String retval = getCategory() + "<br />";

		retval += " <b>Location:</b> " + location + "<br />";
		retval += (getContent() == null ? "<em>No channel selected</em>" : "<b>Channel:</b> '" + getContent().getName() + "'");
		retval += " <br /><b>IP:</b> (" + getIPAddress() + ")";
		retval += " <br /><b>Color:</b> " + getPreferredHighlightColor().toString().replace("gov.fnal.ppd.dd.util.HSBColor", "");
		return retval;
	}

	@Override
	public SignageType getCategory() {
		return category;
	}

	@Override
	public SignageContent getContent() {
		return channel;
	}

	@Override
	public int getScreenNumber() {
		return screenNumber;
	}

	@Override
	public InetAddress getIPAddress() {
		return ipAddress;
	}

	@Override
	public String getLocation() {
		return location;
	}

	@Override
	public String getStatus() {
		// return "Displaying: " + (getContent() != null ? getContent().getName() : "No channel");
		return "" + getContent();
	}

	public void disconnect() {
		// Nothing to do except for facade displays
	}
}
