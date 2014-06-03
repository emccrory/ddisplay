package gov.fnal.ppd.signage;

/**
 * Categorizes the Signage Content and the Displays. This is an authorization hierarchy: XOC
 * contains Experiment contains Public or
 * "A XOC Display can show Content of type 'XOC', 'Experiment' and 'Public'"
 * "An Experiment Display can show content of type 'Experiment' and 'Public'"
 * "A Public Display can show content of type 'Public'"
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2013-14.
 */
public enum SignageType {
	Public, Experiment, XOC;

	/**
	 * For a Display, check a given Channel to see if the Display can show it
	 * 
	 * @param c
	 * @return
	 */
	public boolean isVisible( SignageContent c ) {
		if (this == Public)
			return c.getType() == Public;
		if (this == Experiment)
			return c.getType() != XOC;
		return true;
	}

	/**
	 * For a Channel, can it be shown on a given Display?
	 * @param d
	 * @return
	 */
	public boolean isVisible( Display d ) {
		if (this == Public)
			return true;
		if (this == Experiment)
			return d.getCategory() != XOC;
		return d.getCategory() == XOC;
	}
}
