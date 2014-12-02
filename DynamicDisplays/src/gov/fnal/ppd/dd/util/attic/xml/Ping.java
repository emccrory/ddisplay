package gov.fnal.ppd.dd.util.attic.xml;

import gov.fnal.ppd.dd.xml.EncodedCarrier;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Implementation of the "ping" message
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 *
 */
@XmlRootElement
public class Ping extends EncodedCarrier {
	private int displayNum;

	public int getDisplayNum() {
		return displayNum;
	}

	@XmlElement
	public void setDisplayNum( int displayNum ) {
		this.displayNum = displayNum;
	}

}
