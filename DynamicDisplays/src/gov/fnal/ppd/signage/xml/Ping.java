package gov.fnal.ppd.signage.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author mccrory
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
