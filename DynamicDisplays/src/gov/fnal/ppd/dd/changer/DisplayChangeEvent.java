/*
 * DisplayChangeEvent
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import java.awt.event.ActionEvent;

/**
 * The event that tells the world that a Display has changed (or reiterated) its status.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class DisplayChangeEvent extends ActionEvent {

	private static final long	serialVersionUID	= -2282928868265941338L;
	private static int			localID				= 1001 + (int) Math.random() * 1001;  // Who knows?

	/**
	 * The type of event this is
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	@SuppressWarnings("javadoc")
	public static enum DisplayChangeType {
		CHANGE_RECEIVED, CHANGE_COMPLETED, ALIVE, ERROR, IDLE,
	};

	private DisplayChangeType type = DisplayChangeType.IDLE;

	/**
	 * @param source
	 *            The source of this ActionEvent
	 * @param id
	 *            The ID number for this ActionEvent
	 * @param t
	 *            The type of event this is
	 */
	public DisplayChangeEvent(final Object source, final int id, final DisplayChangeType t) {
		super(source, (id <= 0 ? localID++ : id), "" + t);
		// The type becomes the command
		this.type = t;
	}

	/**
	 * @param source
	 *            The source of this ActionEvent
	 * @param id
	 *            The ID number for this ActionEvent
	 * @param t
	 *            The type of event this is
	 * @param moreInfo
	 *            Extra information to tag onto the event type
	 */
	public DisplayChangeEvent(final Object source, final int id, final DisplayChangeType t, final String moreInfo) {
		super(source, (id <= 0 ? localID++ : id), "" + t + " -- " + moreInfo);
		this.type = t;
	}

	@Override
	public String getActionCommand() {
		return "" + type;
	}

	/**
	 * @return the type of DisplayChangeEvent this is
	 */
	public DisplayChangeType getType() {
		return type;
	}

	@Override
	public String toString() {
		return super.toString().replace("unknown type", "Display Change Action");
	}
}
