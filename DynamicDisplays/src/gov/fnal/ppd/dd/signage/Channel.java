package gov.fnal.ppd.dd.signage;

/**
 * Abstraction for a specific sort of content, that which exists in the database as a URL with a channel number, in the Fermilab Dynamic Displays system
 * 
 * @author Elliott McCrory, Fermilab XOC, 2012-21
 */
public interface Channel extends SignageContent {

	/**
	 * @return The channel number that this content represents 
	 */
	public int getNumber();
}
