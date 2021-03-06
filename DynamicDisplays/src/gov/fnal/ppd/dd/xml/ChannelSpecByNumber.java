package gov.fnal.ppd.dd.xml;

import javax.xml.bind.annotation.XmlElement;

import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.MapOfChannels;

/**
 * A dead-simple XML class to let a channel number (only) be transmitted
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelSpecByNumber extends ChannelSpec {
	
	private static final long serialVersionUID = -7250458242146986630L;
	private int						number	= 0;
	private static MapOfChannels	moc		= new MapOfChannels();

	/**
	 * 
	 */
	public ChannelSpecByNumber() {
		this(1);
	}

	/**
	 * Create a new instance, base on the channel number.
	 * 
	 * @param n
	 *            The channel number
	 */
	public ChannelSpecByNumber(final int n) {
		super(new ChannelImpl(moc.get(n))); // Clone this so the client can do what they want with this object.
		number = n;
	}

	/**
	 * @return the channel number
	 */
	@XmlElement
	public int getNumber() {
		return number;
	}

	/**
	 * @param number
	 *            the channel number to set
	 */
	public void setNumber(final int number) {
		this.number = number;
		content = new ChannelImpl(moc.get(this.number)); // Clone this so the client can do what they want.
	}

	
}
