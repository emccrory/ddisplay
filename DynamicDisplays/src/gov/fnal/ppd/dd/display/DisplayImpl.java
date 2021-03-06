/*
 * DisplayImpl
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.display;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_EXTENDED_DISPLAY_NAMES;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_VIRTUAL_DISPLAY_NUMS;
import static gov.fnal.ppd.dd.GlobalVariables.userHasDoneSomething;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;
import static gov.fnal.ppd.dd.util.specific.PackageUtilities.makeEmptyChannel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import gov.fnal.ppd.dd.changer.DDButton;
import gov.fnal.ppd.dd.changer.DisplayChangeEvent;
import gov.fnal.ppd.dd.emergency.Severity;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.EmergencyCommunication;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.nonguiUtils.JavaChangeListener;
import gov.fnal.ppd.dd.util.nonguiUtils.JavaVersion;

/**
 * The implementation of a Display. This is made concrete on the controller side through DisplayFacade, and on the display side
 * through DisplayControllerMessagingAbstract (which is made concrete as DisplayAsConnectionThroughSelenium)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public abstract class DisplayImpl implements Display, JavaChangeListener {

	/**
	 * A way for the Display to handle errors from the messaging system
	 * 
	 * @param message
	 */

	private static int				displayCount			= 1;
	// private int displayNumber = displayCount++;

	private int						dbDisplayNumber;
	private int						vDisplayNumber			= displayCount++;

	protected int					screenNumber			= 0;
	protected static AtomicInteger	internalThreadID		= new AtomicInteger(0);
	protected SignageContent		channel;
	// May need to change this attribute to a stack, so we can pop off to older "previous channels". Things like "IDENTIFY" and
	// "REFRESH" screw this up.
	// protected SignageContent previousChannel = null;
	protected Stack<SignageContent>	previousChannelStack	= new Stack<SignageContent>();

	protected Color					highlightColor;
	protected List<ActionListener>	listeners				= new ArrayList<ActionListener>();
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
	 *            - the IPName of this node
	 * @param vDisplay
	 *            - the virtual (local) display number for this display
	 * @param dbDisplay
	 *            - the index number in the database for this display
	 * @param screenNumber
	 *            - the screen number for this display
	 * @param location
	 *            - The description of the location
	 * @param color
	 *            - the highlight color
	 */
	public DisplayImpl(final String ipName, final int vDisplay, final int dbDisplay, final int screenNumber, final String location,
			final Color color) {
		assert (screenNumber >= 0);
		assert (vDisplay >= 0);
		assert (dbDisplay >= 0);

		JavaVersion.getInstance().addJavaChangeListener(this);

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
		// this.previousChannel = this.channel = makeEmptyChannel(null);
		this.channel = makeEmptyChannel(null);
		this.previousChannelStack.push(this.channel);
	}

	protected abstract boolean localSetContent();

	@Override
	public SignageContent setContent(final SignageContent c) {

		// TODO -- When an emergency message is up, we should (probably) not allow the channel to change.
		//
		// The ideal way would be to change the underlying channel while keeping the emergency message up, but this is hard.
		// ACtually, the way to do this is to have the emergency message EXPIRE after a suitable amount of time (an hour?). Then
		// the last channel will reappear.

		if (c instanceof EmergencyCommunication) {
			EmergencyCommunication ec = (EmergencyCommunication) c;
			if (ec.getMessage().getSeverity() == Severity.REMOVE) {
				println(getClass(), ": Display " + getVirtualDisplayNumber() + " is removing its Emergency Message ["
						+ ec.getMessage() + "] at " + (new Date()));
			} else {
				println(getClass(), ": Display " + getVirtualDisplayNumber() + " being sent an EMERGENCY MESSAGE ["
						+ ec.getMessage() + "] at " + (new Date()));
			}
			informListeners(DisplayChangeEvent.DisplayChangeType.CHANGE_RECEIVED, null);

			channel = c;

			if (localSetContent())
				;

			return channel;
		}
		boolean verified = isVerifiedChannel(c);
		if (verified) {
			// Take care of a null argument and remembering the previous channel.
			if (!channel.equals(c)) {
				if (channel instanceof EmergencyCommunication)
					println(getClass(), " -- Current channel is an EmergencyCommunication.  Retaining the old previous channel of "
							+ previousChannelStack.peek());
				else
					previousChannelStack.push(channel);
			}
			if (c == null)
				channel = makeEmptyChannel(null);
			else
				channel = c;

			println(getClass(), ": Display " + getVirtualDisplayNumber() + " changed to [" + channel + "] at " + (new Date()));
			// informListeners(DisplayChangeEvent.Type.CHANGE_RECEIVED, null);

			// Actually do the channel change!
			respondToContentChange(localSetContent(), "");
			return previousChannelStack.peek();
		} else {
			error("The requested channel (URL=" + c.getURI().toString() + ") is not approved to be shown");
			informListeners(DisplayChangeEvent.DisplayChangeType.ERROR,
					"The requested channel (URL=" + c.getURI().toString() + ") is not approved to be shown");
		}

		return channel;
	}

	/**
	 * This should be overridden in the real display client
	 * 
	 * @param success
	 *            - did the channel change work?
	 * @param why
	 *            - Why did it fail
	 */
	protected void respondToContentChange(boolean success, final String why) {
		println(getClass(), " >>>> Setting the content in setConent(): " + (success ? "SUCCESS" : "F-A-I-L-U-R-E"));
		if (success) {
			// Prototyping - make a fake "Yay, it worked" thread.
			// new Thread("FakeChangeComplete") {
			// long sleepTime = (SHOW_IN_WINDOW ? 1000 : 5000);
			//
			// public void run() {
			// catchSleep(sleepTime);
			// informListeners(DisplayChangeEvent.Type.CHANGE_COMPLETED, "Channel change succeeded");
			// }
			// }.start();
		} else {
			informListeners(DisplayChangeEvent.DisplayChangeType.ERROR, "Channel change unsuccessful: " + why);
		}
	}

	/**
	 * Had the validness of this channel been checked, and is it valid? Override if this is something you need to check
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

	/**
	 * An error has been encountered.
	 * 
	 * This should probably be overridden by the classes that know how to write to the physical display. Pushing this error
	 * information into the log file is probably not the best choice for the disposition of this information.
	 * 
	 * @param string
	 */
	protected void error(String string) {
		printlnErr(getClass(), string);
	}

	@Override
	public String toString() {
		if (SHOW_EXTENDED_DISPLAY_NAMES) {
			return "<html>Display " + (getVirtualDisplayNumber() + " | " + getDBDisplayNumber() + "<br>" + getLocation())
					.replace("Display", "Disp").replace("Dynamic", "Dyn") + "</html>";
		}
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
			userHasDoneSomething();
		} else
			throw new RuntimeException("Unexpected source: " + e.getSource().getClass().getCanonicalName());
	}

	protected void informListeners(final DisplayChangeEvent.DisplayChangeType t, String more) {
		final DisplayChangeEvent ev = new DisplayChangeEvent(this, internalThreadID.getAndIncrement(), t, more);
		for (final ActionListener L : listeners)
			new Thread("DisplayInform" + t + "_" + L.getClass().getSimpleName()) {
				public void run() {
					// System.out.println(DisplayImpl.this.getClass().getSimpleName() + ": " + t + " sent to a "
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
		informListeners(DisplayChangeEvent.DisplayChangeType.ERROR, errorMessage);
	}

	@Override
	public String getDescription() {
		String retval = " <b>Location:</b> " + location + "<br />";
		retval += (getContent() == null ? "<em>No channel selected</em>" : "<b>Channel:</b> '" + getContent().getName() + "'");
		retval += " <br /><b>IP:</b> (" + getIPAddress() + ")";
		retval += " <br /><b>Color:</b> " + getPreferredHighlightColor().toString().replace("gov.fnal.ppd.dd.util.HSBColor", "");
		return retval;
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
		return "" + getContent();
	}

	/**
	 * Disconnect this display from the browser
	 */
	public void disconnect() {
		// Nothing to do except for facade displays
	}

	/*
	 * (non-Javadoc) The dbDisplayNumber is guaranteed by the database to be unique.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dbDisplayNumber;
		result = prime * result + getClass().getCanonicalName().hashCode();
		return result;
	}

	/*
	 * (non-Javadoc) The dbDisplayNumber is guaranteed by the database to be unique.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof DisplayImpl))
			return false;
		if (!(obj.getClass().getCanonicalName().equals(getClass().getCanonicalName())))
			return false;
		DisplayImpl other = (DisplayImpl) obj;
		if (dbDisplayNumber != other.dbDisplayNumber)
			return false;
		return true;
	}

	@Override
	public void javaHasChanged() {
		// By default, do nothing.
	}
}
