package gov.fnal.ppd.signage.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
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
