package gov.fnal.ppd.signage.changer;

import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageType;

import java.util.List;

/**
 * The holder for a list of channels
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 *
 */
public interface DisplayList extends List<Display>{

	/**
	 * @param dn
	 * @return The category of the channels in this list
	 */
	public SignageType getCategory( int dn );
	
	/**
	 * @return all of the categories of the channels in this list
	 */
	public SignageType [] getCategories();
	
}