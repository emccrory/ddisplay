package gov.fnal.ppd.dd.xml;

import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.signage.SignageContent;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Message from a Control Panel to the Display Server to change the channel on a Display to a LIST of Channels
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012-16
 */
@SuppressWarnings("javadoc")
@XmlRootElement
public class ChangeChannelList extends EncodedCarrier {

	// TODO - Add absolute scheduling of the wall-clock time to play channels

	// We'd add a new field here that gives the exact wall-clock time to begin the list, and then
	// the rest of the list plays out accordingly. However, it is not clear when to restart the list.

	// Hmmm. This might not work.

	// Maybe the right way to do this is to make a new server app, on a dedicated server, to send out the
	// channel change commands at the right time. This way these changes can be synchronized. But where
	// is this server? And how do you turn it off? You'd want to turn off the schedule by simply sending
	// a new channel to the display.

	protected ChannelPlayList	cpl	= new ChannelPlayList();
	protected int				displayNum;
	protected int				screenNum;
	private long				dwell;

	@XmlElement
	public ChannelSpec[] getChannelSpec() {
		ChannelSpec[] retval = new ChannelSpec[cpl.getChannels().size()];
		int i = 0;
		for (SignageContent C : cpl.getChannels()) {
			retval[i++] = new ChannelSpec(C);
		}
		return retval;
	}

	@XmlElement
	public int getDisplayNumber() {
		return displayNum;
	}

	public void setDisplayNumber(int d) {
		displayNum = d;
	}

	@XmlElement
	public int getScreenNumber() {
		return screenNum;
	}

	public void setScreenNumber(int s) {
		screenNum = s;
	}

	public void setChannelSpec(ChannelSpec[] cs) {
		System.out.println(getClass().getSimpleName() + ": Adding a channelSpec list of length " + cs.length);

		for (ChannelSpec spec : cs) {
			cpl.getChannels().add(spec.getContent());
		}
	}

	public void setContent(SignageContent s) {
		cpl = (ChannelPlayList) s;
		dwell = cpl.getTime();
	}

	@XmlElement
	public long getTime() {
		return dwell;
	}

	public void setTime(long n) {
		dwell = n;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cpl == null) ? 0 : cpl.hashCode());
		result = prime * result + displayNum;
		result = prime * result + (int) (dwell ^ (dwell >>> 32));
		result = prime * result + screenNum;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChangeChannelList other = (ChangeChannelList) obj;
		if (cpl == null) {
			if (other.cpl != null)
				return false;
		} else if (!cpl.equals(other.cpl))
			return false;
		if (displayNum != other.displayNum)
			return false;
		if (dwell != other.dwell)
			return false;
		if (screenNum != other.screenNum)
			return false;
		return true;
	}

	public String toString() {
		String retval = "List of length " + cpl.getChannels().size() + " [";
		for (int i = 0; i < cpl.getChannels().size(); i++) {
			retval += cpl.getChannels().get(i).getURI().toString() + " (" + cpl.getChannels().get(i).getTime() + "), ";
		}
		return retval + "]";
	}

}
