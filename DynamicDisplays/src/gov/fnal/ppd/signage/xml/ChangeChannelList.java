package gov.fnal.ppd.signage.xml;

import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.channel.ChannelPlayList;

import java.net.URISyntaxException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Message from a Control Panel to the Display Server to change the channel on a Display to a LIST of Channels
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012
 */
@XmlRootElement
public class ChangeChannelList extends EncodedCarrier {

	protected ChannelPlayList	cpl			= new ChannelPlayList();
	protected int				displayNum;
	private long				dwell;

	@XmlElement
	public ChannelSpec[] getChannelSpec() {
		ChannelSpec[] retval = new ChannelSpec[cpl.getChannels().size()];
		int i = 0;
		for (SignageContent C : cpl.getChannels()) {
			ChannelSpec cs = new ChannelSpec();
			cs.setContent(C);
			cs.setTime(dwell);
			retval[i++] = cs;
		}
		return retval;
	}

	@XmlElement
	public int getDisplayNumber() {
		return displayNum;
	}

	public void setChannelSpec(ChannelSpec[] cs) {
		System.out.println(getClass().getSimpleName() + ": Adding a channelSpec list of length " + cs.length);
		try {
			for (ChannelSpec spec : cs) {
				EmptyChannel content = new EmptyChannel();
				content.setCategory(spec.getCategory());
				content.setName(spec.getName());
				content.setURI(spec.getUri());
				cpl.getChannels().add(content);
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public void setContent(SignageContent s) {
		cpl = (ChannelPlayList) s;
		dwell = cpl.getDwell();
	}

	public void setDisplayNumber(int d) {
		displayNum = d;
	}

	@XmlElement
	public long getTime() {
		return dwell;
	}

	public void setTime(long n) {
		dwell = n;
	}

}
