package gov.fnal.ppd.signage;

/**
 * <p>
 * Categorizes the Signage Content and the Displays. This is an authorization hierarchy:
 * <ol>
 * <li>XOC contains Experiment contains Public or "A XOC Display can show Content of type 'XOC', 'Experiment' and 'Public'"</li>
 * <li>"An Experiment Display can show content of type 'Experiment' and 'Public'"</li>
 * <li>"A Public Display can show content of type 'Public'"</li>
 * </ol>
 * </p>
 * <p>
 * The Display and the Content are both tagged with this hierarchy
 * </p>
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2013-14.
 */
public enum SignageType {
	/**
	 * The most restrictive sort of Display
	 */
	Public,
	/**
	 * A Display of type Experiment can show Experiment or Public content
	 */
	Experiment,
	/**
	 * A display of type XOC can show any type of content
	 */
	XOC;

	/**
	 * For a Display, check a given Channel to see if the Display can show it
	 * 
	 * @param c
	 *            The Content that this Display wants to show The Content that is to be shown
	 * @return Is this type of content displayable on this Display?
	 */
	public boolean isVisible(SignageContent c) {
		if (this == Public)
			return c.getType() == Public;
		if (this == Experiment)
			return c.getType() != XOC;
		return true;
	}

	/**
	 * For a Channel, can it be shown on a given Display?
	 * 
	 * @param d
	 *            The Display onto which this content is to be shown
	 * @return Can this Content be shown on this sort of Display?
	 */
	public boolean isVisible(Display d) {
		if (this == Public)
			return true;
		if (this == Experiment)
			return d.getCategory() != XOC;
		return d.getCategory() == XOC;
	}
}
