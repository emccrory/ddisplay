package gov.fnal.ppd.signage.xml;

import gov.fnal.ppd.signage.SignageType;

/**
 * Read the configuration information for the ChannelSelector GUI
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class ChannelSelectorConfigure {
	private int[]		categories	= { 0, 1, 3, 4, 2, 6, 5, -1 };
	private String[]	labels		= { "Public", "Details", "NOvA", "NuMI", "EXP", "BEAM", "Videos", "MISC" };
	
	public ChannelSelectorConfigure(int locationCode, SignageType typ){
		
	}
}
