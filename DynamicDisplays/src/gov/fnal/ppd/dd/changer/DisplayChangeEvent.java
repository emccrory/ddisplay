package gov.fnal.ppd.dd.changer;

import java.awt.event.ActionEvent;

/**
 * The event that tells the world that a Display has changed (or reiterated) its status.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class DisplayChangeEvent extends ActionEvent {

	private static final long	serialVersionUID	= -2282928868265941338L;

	/**
	 * The type of event this is
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * @copyright 2014
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
