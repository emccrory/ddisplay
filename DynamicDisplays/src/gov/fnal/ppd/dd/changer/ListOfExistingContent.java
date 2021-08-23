package gov.fnal.ppd.dd.changer;

import java.io.Serializable;
import java.util.HashMap;

import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;

/**
 * A simple list of SignageContent at each Display. It is intended to hold the contents of all the Displays under the control of a
 * ChannelSelector, in the context of doing a "Super Save". The implementing class is probably going to find out what content is
 * showing on each display - the results will be collected in that class and presented to the others with this signature.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ListOfExistingContent extends HashMap<Display, SignageContent> implements Serializable {

	private static final long serialVersionUID = -1771561314944627805L;

	/**
	 * 
	 */
	public ListOfExistingContent() {
		super();
	}

	// It is necessary to create a method that returns the content as a function of the Display _number_, rather than the
	// Display object itself

	/**
	 * @param displayID
	 *            The Display to return, based on the database ID number.
	 * @return the Display object that has the database ID of displayID
	 */
	public Display get(final int displayID) {
		for (Display D : keySet())
			if (D.getDBDisplayNumber() == displayID)
				return D;
		return null;
	}

	@Override
	public SignageContent put(Display key, SignageContent value) {
		if (key == null)
			throw new NullPointerException("Unexpected null display. SignageContent is " + value + " though.");
		return super.put(key, value);
	}

}
