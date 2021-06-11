package gov.fnal.ppd.dd.signage;

/**
 * Abstraction for the XOC Dynamic Display Channel
 * 
 * @author Elliott McCrory, Fermilab XOC, 2012-21
 */
public interface Channel extends SignageContent {

	/**
	 * @return The channel number that this content represents 
	 */
	public int getNumber();
}
