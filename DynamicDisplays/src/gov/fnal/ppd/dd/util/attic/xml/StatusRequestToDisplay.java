package gov.fnal.ppd.dd.util.attic.xml;

import gov.fnal.ppd.dd.xml.EncodedCarrier;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request from a Control Panel to the Display Server for the status of a Display
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012
 */
@XmlRootElement
public class StatusRequestToDisplay extends EncodedCarrier {

	private int displayNum;

	@XmlElement
	public int getDisplayNum() {
		return displayNum;
	}

	public void setDisplayNum( int displayNum ) {
		this.displayNum = displayNum;
	}
}
