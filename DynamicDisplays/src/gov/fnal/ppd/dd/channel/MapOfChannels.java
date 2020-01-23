package gov.fnal.ppd.dd.channel;

import static gov.fnal.ppd.dd.GlobalVariables.SINGLE_IMAGE_DISPLAY;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.db.ChannelsFromDatabase.readValidChannels;

import gov.fnal.ppd.dd.CredentialsNotFoundException;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.util.HashMap;

/**
 * Holds a map of ALL the channels in the Dynamic Displays database
 * 
 * Simple testing (see main(), below) indicates that the memory required is about 6.5MB + 1kB per channel
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class MapOfChannels extends HashMap<Integer, Channel> {

	private static final long	serialVersionUID	= 2215500235448793288L;

	/**
	 * Create a new instance by reading the list of valid URLs from the database
	 */
	public MapOfChannels() {
		super();
		readValidChannels(this);
	}

	

	/**
	 * Re-read the list of valid URLs from the database
	 */
	public void resetChannelList() {
		super.clear();
		readValidChannels(this);
	}

	/**
	 * Return a Channel object that is identified with this channel number.
	 * 
	 * @param number
	 *            The Channel number
	 * @return The Channel object that is identified with this channel number.
	 */
	public Channel getAChannel(final int number) {
		return get(number);
	}

	/**
	 * @param c
	 *            the content to check
	 * @return If this bit of content is "approved"
	 */
	public boolean contains(SignageContent c) {
		// FIXME -- this next line is partially right. The check if this hash contains this URL is OK, but the second part, the
		// check of a fixed URL against the given URL, is inelegant. This should somehow be in the database.

		boolean r = containsValue(c.getURI().toString()) || c.getURI().toString().startsWith(SINGLE_IMAGE_DISPLAY);
		if (!r) {
			System.out
					.println("----- signage content " + c + " " + c.getURI().toString() + "\n----- REJECTED!  Reloading the list");
			resetChannelList();
			r = containsValue(c.getURI().toString());
			System.out.println("---- "
					+ (r ? "accepted now." : "STILL REJECTED!  The URL " + c.getURI().toString()
							+ " is deemed to be inappropriate."));
		}
		return r;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		MapOfChannels v = new MapOfChannels();
		for (int number : v.keySet())
			System.out.println(number + ": " + v.get(number));
	}
}
