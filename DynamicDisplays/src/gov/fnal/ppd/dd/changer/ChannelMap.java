/*
 * ChannelsFromDatabase
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.db.ChannelsFromDatabase.getChannels;
import static gov.fnal.ppd.dd.db.ChannelsFromDatabase.getImages;
import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Return all the channels from the database
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelMap extends HashMap<String, SignageContent> implements ChannelCatalog {

	private static final long							serialVersionUID	= 8020508216810250903L;
	private static Comparator<? super SignageContent>	comparator			= new Comparator<SignageContent>() {

																				/**
																				 * Compare by the name of the channel
																				 * 
																				 * @param o1
																				 * @param o2
																				 * @return the comparison result
																				 */
																				public int compare(SignageContent o1,
																						SignageContent o2) {
																					return o1.getName().compareTo(o2.getName());
																				}

																			};

	/**
	 * @return The existing comparator being used by the channel creation method
	 */
	public static Comparator<? super SignageContent> getComparator() {
		return comparator;
	}

	/**
	 * @param comparator
	 *            The replacement comparator for creating the channel list.
	 */
	public static void setComparator(final Comparator<? super SignageContent> comparator) {
		ChannelMap.comparator = comparator;
	}

	/**
	 * 
	 */
	public ChannelMap() {
	}

	/**
	 * @throws DatabaseNotVisibleException
	 */
	public void load() throws DatabaseNotVisibleException {
		getChannels(this);
		getImages(this);
	}

	public Set<SignageContent> getChannelCatalog(ChannelCategory cat) {
		TreeSet<SignageContent> retval = new TreeSet<SignageContent>(comparator);

		for (String key : this.keySet())
			if (this.get(key).getCategory().equals(cat)) {
				SignageContent C = this.get(key);
				if (C instanceof ChannelImage)
					retval.add(new ChannelImage(C));
				else if (C instanceof Channel)
					retval.add(new ChannelImpl(C));
			}
		return retval;
	}
}
