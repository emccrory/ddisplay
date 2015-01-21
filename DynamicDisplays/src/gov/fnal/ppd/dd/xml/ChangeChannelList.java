package gov.fnal.ppd.dd.xml;

import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.net.URISyntaxException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Message from a Control Panel to the Display Server to change the channel on a Display to a LIST of Channels
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012
 */
@SuppressWarnings("javadoc")
@XmlRootElement
public class ChangeChannelList extends EncodedCarrier {

	protected ChannelPlayList	cpl	= new ChannelPlayList();
	protected int				displayNum;
	private long				dwell;

	@XmlElement
	public ChannelSpec[] getChannelSpec() {
		ChannelSpec[] retval = new ChannelSpec[cpl.getChannels().size()];
		int i = 0;
		for (SignageContent C : cpl.getChannels()) {
			ChannelSpec cs = new ChannelSpec();
			cs.setContent(C);
			// See "TODO" comment in setChannelSpec(), below.
			cs.setTime(-1);
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
				EmptyChannel content = new EmptyChannel(spec.getName(), spec.getCategory());

				content.setURI(spec.getUri());
				content.setCode(spec.getCode());

				// TODO -- There may be some clever way to use the time attribute in each channel in the spec
				// intelligently. But for now, all channels in the spec will change at a fixed period. Set the
				// time in each individual channel so that it skips the refresh entirely.
				content.setTime(-1);

				cpl.getChannels().add(content);
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public void setContent(SignageContent s) {
		cpl = (ChannelPlayList) s;
		dwell = cpl.getTime();
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
