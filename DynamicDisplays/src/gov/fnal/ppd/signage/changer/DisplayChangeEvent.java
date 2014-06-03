package gov.fnal.ppd.signage.changer;

import java.awt.event.ActionEvent;

/**
 * The event that tells the world that a Display has changed (or reiterated) its status.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 *
 */
public class DisplayChangeEvent extends ActionEvent {

	private static final long serialVersionUID = -2282928868265941338L;

	/**
	 * The type of event this is
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * @copy 2014
	 *
	 */
	@SuppressWarnings("javadoc")
	public static enum Type {
		CHANGE_RECEIVED, CHANGE_COMPLETED, ALIVE, ERROR, IDLE,
	};

	private Type type = Type.IDLE;

	public DisplayChangeEvent( Object source, int id, Type t ) {
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
}
