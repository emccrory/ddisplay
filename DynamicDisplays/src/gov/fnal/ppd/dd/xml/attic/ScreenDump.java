package gov.fnal.ppd.dd.xml.attic;

import gov.fnal.ppd.dd.xml.EncodedCarrier;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A request from a console to the Display Server to put (some of) its screen onto a Display
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012
 */
@XmlRootElement
public class ScreenDump extends EncodedCarrier {
	private int displayNum;

	// Not sure what this is yet
	private Object screenSpecification;

	@XmlElement
	public int getDisplayNum() {
		return displayNum;
	}

	public void setDisplayNum( int displayNum ) {
		this.displayNum = displayNum;
	}

	@XmlElement
	public String getScreenSpec() {
		return screenSpecification.toString();
	}

	public void setScreenSpec( Object spec ) {
		screenSpecification = spec;
	}
}
