package gov.fnal.ppd.dd.changer;

import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple list of SignageContent at each Display. This is here to ensure that it can be streamed
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ListOfExistingContent extends HashMap<Display, SignageContent> implements Serializable {

	private static final long	serialVersionUID	= -1771561314944627805L;

	/**
	 * 
	 */
	public ListOfExistingContent() {
		super();
	}

	/**
	 * @param initialCapacity
	 * @param loadFactor
	 */
	public ListOfExistingContent(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * @param initialCapacity
	 */
	public ListOfExistingContent(final int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * @param m
	 */
	public ListOfExistingContent(final Map<? extends Display, ? extends SignageContent> m) {
		super(m);
	}

	// It might be necessary to create a method that returns the content as a function of the Display number
}