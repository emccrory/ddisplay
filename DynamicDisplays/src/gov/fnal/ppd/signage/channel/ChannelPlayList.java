package gov.fnal.ppd.signage.channel;

import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.ChannelCategory;

import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ChannelPlayList implements Channel {

	// FIXME -- No, the playing of a list of channels has to be handled by the Real Display.

	private static final long				serialVersionUID	= 7785042152443699996L;
	private List<SignageContent>			channels			= new ArrayList<SignageContent>();
	private ListIterator<SignageContent>	chanIterator;
	private SignageContent					currentChannel;
	private long							dwell				= 5000l;
	private boolean							running				= true;
	// private Thread playThread = null;
	private Display							myDisplay			= null;

	/**
	 * Create a channel list to play out on the Display.
	 * 
	 * @param list
	 *            - The list of channels to play (must be 2 or greater)
	 * @param millisecondDwellTime
	 *            - How long to wait on each channel (must be 5 seconds or longer)
	 */
	public ChannelPlayList(List<SignageContent> list, long millisecondDwellTime) {
		assert (list.size() > 1);
		assert (millisecondDwellTime > 5000);

		this.dwell = millisecondDwellTime;
		this.channels = list;
		chanIterator = channels.listIterator();
		currentChannel = chanIterator.next(); // Get to the first channel
	}

	// public ChannelPlayList(String rawMessage) {
	// // Read in the list of Channels to play
	// String[] urls = rawMessage.split(",");
	// for (String U : urls)
	// try {
	// channels.add(new PlainURLChannel(new URL(U)));
	// } catch (Exception e) {
	// try {
	// dwell = Long.parseLong(U);
	// } catch (Exception el) {
	// e.printStackTrace();
	// el.printStackTrace();
	// }
	// if (dwell <= 0)
	// e.printStackTrace();
	// }
	// }

	public ChannelPlayList() {
	}

	@Override
	public String getName() {
		return currentChannel.getName() + " (List)";
	}

	@Override
	public Object getContent() {
		return currentChannel.getContent();
	}

	protected void informDisplay() {
		if (myDisplay != null) {
			myDisplay.setContent(currentChannel);
		}
	}

	public void setDisplay(Display d) {
		myDisplay = d;
	}

	@Override
	public String getDescription() {
		return "List of channels, now showing " + currentChannel.getDescription();
	}

	@Override
	public ChannelCategory getCategory() {
		return currentChannel.getCategory();
	}

	@Override
	public SignageType getType() {
		return currentChannel.getType();
	}

	@Override
	public URI getURI() {
		return currentChannel.getURI();
	}

	@Override
	public int getNumber() {
		if (currentChannel instanceof Channel)
			return ((Channel) currentChannel).getNumber();
		return 0;
	}

	public void start() {
		new Thread("PlayingChannelList") {
			public void run() {
				try {
					sleep(dwell);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				while (running) {
					if (chanIterator.hasNext()) {
						currentChannel = chanIterator.next();
					} else {
						chanIterator = channels.listIterator();
						currentChannel = chanIterator.next();
					}
					informDisplay();
					try {
						sleep(dwell);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	public void stop() {
		running = false;
	}

	@Override
	public void setName(String name) {
		// Not relevant for a list of channels
	}

	@Override
	public void setContent(Serializable content) {
		// Not relevant for a list of content
	}

	@Override
	public void setDescription(String d) {
		// Not relevant for a list of content
	}

	@Override
	public void setCategory(ChannelCategory c) {
		// Not relevant for a list of content
	}

	@Override
	public void setType(SignageType t) {
		// Not relevant for a list of content
	}

	@Override
	public void setURI(URI i) {
		// Not relevant for a list of content
	}

	public long getDwell() {
		return dwell;
	}

	public void setDwell(long dwell) {
		this.dwell = dwell;
	}

	public List<SignageContent> getChannels() {
		return channels;
	}

	public void setChannels(List<SignageContent> channels) {
		this.channels = channels;
		chanIterator = this.channels.listIterator();
	}
}
