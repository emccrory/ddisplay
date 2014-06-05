package gov.fnal.ppd.signage.changer.attic;

import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.changer.ChannelCatalogFactory;
import gov.fnal.ppd.signage.changer.ChannelCategory;
import gov.fnal.ppd.signage.changer.DisplayChangeEvent;
import gov.fnal.ppd.signage.changer.DisplayChangeEvent.Type;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

/**
 * When a Display changes channel, it may be necessary to do some other stuff.
 * 
 * @author Elliott McCrory, Fermilab, 2012.
 */
public class DisplayMonitor implements ActionListener {
	private static final int AUTO_CHANGE_SECONDS = 60;

	private static final int INITIAL_DWELL_SECONDS = 15 * 60;

	private static int autoChangeSeconds = AUTO_CHANGE_SECONDS;

	private Display myDisplay;

	private Thread waitToChangeChannel = null;

	private int countDown = 0;

	private SignageContent lastChannel;

	private Object [] channelCatalog = ChannelCatalogFactory.getInstance() 
			.getChannelCatalog(ChannelCategory.PUBLIC).toArray();

	private int index;

	private boolean	autoTimeoutOnChannels = false;

	/**
	 * @param d
	 */
	public DisplayMonitor( final Display d ) {
		myDisplay = d;
		d.addListener(this);
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		try {
			Thread.sleep(105);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		if (myDisplay.getContent().equals(lastChannel) || !(e instanceof DisplayChangeEvent))
			return;
		DisplayChangeEvent ev = (DisplayChangeEvent) e;
		if (ev.getType() != Type.CHANGE_COMPLETED)
			return;
		lastChannel = myDisplay.getContent();

		if (myDisplay.getContent().getCategory() == ChannelCategory.PUBLIC) {
			// Setup or update a Thread to change the channel in a little while

			System.out.println(getClass().getSimpleName() + ".actionPerformed() called, source='"
					+ e.getSource() + "' " + ev.getType());
			countDown = INITIAL_DWELL_SECONDS;
			index = 0;
			for (int i = 0; i < channelCatalog.length; i++)
				if (channelCatalog[i].equals(lastChannel)) {
					index = i;
					break;
				}

			if (autoTimeoutOnChannels  && (waitToChangeChannel == null || !waitToChangeChannel.isAlive())) {
				System.out.println(getClass().getSimpleName() + ": Starting thread...");
				waitToChangeChannel = new Thread("AutoChangeChannel" + myDisplay.getNumber()) {
					public void run() {
						while (myDisplay.getContent().getCategory() == ChannelCategory.PUBLIC) {
							while (countDown-- > 0)
								try {
									sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}

							// Done waiting. Change the channel
							if (myDisplay.getContent().getCategory() == ChannelCategory.PUBLIC) {
								index = (index + 1) % channelCatalog.length;
								myDisplay.setContent((lastChannel = (Channel) channelCatalog[index]));
								countDown = autoChangeSeconds;
							}
						}
						System.out.println("Exiting thread " + getName());
						waitToChangeChannel = null;
					}
				};
				waitToChangeChannel.start();
			} else
				System.out.println(getClass().getSimpleName() + ": Did not start the thread.");
		} else
			countDown = 0;
	}

	/**
	 * Reset the channel list to the public channels
	 */
	public void resetChannelCatalog() {
		channelCatalog = ChannelCatalogFactory.getInstance().getChannelCatalog(ChannelCategory.PUBLIC).toArray();
		autoChangeSeconds = AUTO_CHANGE_SECONDS;
		index = 0;
	}

	/**
	 * Set the list of channels to a user-defined list of channels
	 * 
	 * @param list
	 * @param secondsBetweenChanges
	 */
	public void setChannelList( Object [] list, int secondsBetweenChanges ) {
		channelCatalog = Arrays.copyOf(list, list.length);
		autoChangeSeconds = secondsBetweenChanges;
		index = 0;
	}
}
