package gov.fnal.ppd.signage.display;

import static gov.fnal.ppd.signage.util.Util.makeEmptyChannel;
import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.DDButton;
import gov.fnal.ppd.signage.changer.DisplayChangeEvent;

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
 * @copyright 2014
 * 
 */
public abstract class DisplayImpl implements Display {

	private static int				displayCount		= 1;
	protected int					displayNumber		= displayCount++;
	protected int					screenNumber		= 0;
	protected static AtomicInteger	internalThreadID	= new AtomicInteger(0);
	private SignageContent			channel;
	private Color					highlightColor;
	protected List<ActionListener>	listeners			= new ArrayList<ActionListener>();
	private SignageType				category			= SignageType.Public;
	private InetAddress				ipAddress;
	private String					location;
	// protected TimerTask heartbeatThread = new TimerTask() {
	// public void run() {
	// informListeners(DisplayChangeEvent.Type.ALIVE);
	// }
	// };
	protected String				myName;

	/**
	 * @param ipName
	 * @param screenNumber
	 * @param number
	 * @param location
	 * @param color
	 * @param type
	 */
	public DisplayImpl(final String ipName, final int screenNumber, final int number, final String location, final Color color,
			final SignageType type) {
		assert (screenNumber >= 0);
		assert (number >= 0);

		myName = ipName + ":" + screenNumber + " (" + number + ")";

		try {
			this.ipAddress = InetAddress.getByName(ipName);
		} catch (UnknownHostException e) {
			System.err.println("Host not found: " + ipName);
		}
		this.screenNumber = screenNumber;
		this.location = location;
		this.displayNumber = number;
		this.highlightColor = color;
		this.category = type;
		this.channel = makeEmptyChannel();
	}

	protected abstract void localSetContent();

	@Override
	public SignageContent setContent(SignageContent c) {
		if (getCategory().isVisible(c)) {
			SignageContent retval = channel;
			if (c == null)
				channel = makeEmptyChannel();
			else
				channel = (Channel) c;
			System.out.println(getClass().getSimpleName() + ": Display " + getNumber() + " changed to [" + channel + "] at "
					+ (new Date()));

			informListeners(DisplayChangeEvent.Type.CHANGE_RECEIVED);
			localSetContent();

			// TODO -- This event needs to be triggered by the display actually indicating that it has changed the channel.
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			informListeners(DisplayChangeEvent.Type.CHANGE_COMPLETED);
			return retval;
		}

		error("Content type of this Channel (" + c.getType() + ") is not appropriate for this Display (" + getCategory() + ")");
		return channel;
	}

	protected void error(String string) {
		System.err.println(string);
	}

	@Override
	public String toString() {
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

	@Override
	public int getNumber() {
		return displayNumber;
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

	protected void informListeners(final DisplayChangeEvent.Type t) {
		final DisplayChangeEvent ev = new DisplayChangeEvent(this, internalThreadID.getAndIncrement(), t);
		for (final ActionListener L : listeners)
			new Thread("DisplayInform" + t + "_" + L.getClass().getSimpleName()) {
				public void run() {
					System.out.println(DisplayImpl.this.getClass().getSimpleName() + ": " + t + "  sent to a "
							+ L.getClass().getName() + " (" + L.hashCode() + ")");
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
	public String getDescription() {
		String retval = getCategory() + "<br />";

		if (getContent() == null) {
			retval += "<em>No channel selected</em>";
		} else {
			retval += "<b>Channel:</b> '" + getContent().getName() + "'";
		}
		retval += " <br /><b>Location:</b> " + location;
		retval += " <br /><b>IP:</b> (" + getIPAddress() + ")";
		retval += " <br /><b>Color:</b> " + getPreferredHighlightColor().toString().replace("gov.fnal.ppd.xoc.util.HSBColor", "");
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

	/**
	 * A utility method to allow a well-behaved subclass set the channel underneath things, e.g., during initialization.
	 * 
	 * @param c
	 */
	protected void setChannel(Channel c) {
		channel = c;
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
		return getContent() + " is being displayed.";
	}

	public String getMessagingName() {
		return myName;
	}

	public void disconnect() {
		// Nothing to do except for facade displays
	}
}
