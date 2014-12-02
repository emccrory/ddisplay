package gov.fnal.ppd.dd.signage;

/**
 * Abstraction for the XOC Dynamic Display Channel
 * 
 * @author Elliott McCrory, Fermilab XOC, 2012-14
 */
public interface Channel extends SignageContent {

	/**
	 * @return The channel number that this content represents (not really used)
	 */
	public int getNumber();
}
