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

	/**
	 * The type of event this is
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	@SuppressWarnings("javadoc")
	public static enum Type {
		CHANGE_RECEIVED, CHANGE_COMPLETED, ALIVE, ERROR, IDLE,
	};

	private Type	type	= Type.IDLE;

	/**
	 * @param source
	 *            The source of this ActionEvent
	 * @param id
	 *            The ID number for this ActionEvent
	 * @param t
	 *            The type of event this is
	 */
	public DisplayChangeEvent(final Object source, final int id, final Type t) {
		super(source, id, "" + t);
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
	public DisplayChangeEvent(final Object source, final int id, final Type t, final String moreInfo) {
		super(source, id, "" + t + " -- " + moreInfo);
		this.type = t;
	}

	@Override
	public String getActionCommand() {
		return "" + type;
	}

	/**
	 * @return the type of DisplayChangeEvent this is
	 */
	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return super.toString().replace("unknown type", "Display Change Action");
	}
}
