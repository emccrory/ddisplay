package gov.fnal.ppd.dd.util.attic.xml;

import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.EncodedCarrier;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * the "pong" object
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
@XmlRootElement
public class Pong extends EncodedCarrier {
	private ChannelSpec showing;

	private int displayNum;

	@XmlElement
	public ChannelSpec getChannelSpec() {
		return showing;
	}
	
	public void setChannelSpec(ChannelSpec spec) {
		showing = spec;
	}

	public int getDisplayNum() {
		return displayNum;
	}

	@XmlElement
	public void setDisplayNum( int displayNum ) {
		this.displayNum = displayNum;
	}

}
