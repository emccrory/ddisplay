package gov.fnal.ppd.signage.changer;

import gov.fnal.ppd.signage.SignageContent;

import java.util.Map;
import java.util.Set;

/**
 * An interface for dealing with the various sorts of catalogs of channels that can be
 * in the system.  This is a bit of an historical artifact, allowing me at one point to have
 * an ad-hoc catalog.  Now, we only have a catalog that is read from a remote database.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 *
 */
public interface ChannelCatalog {

	/**
	 * @return all the "Public" channels
	 */
	public abstract Map<String, SignageContent> getPublicChannels();

	/**
	 * @return all the "details" channels
	 */
	public abstract Map<String, SignageContent> getDetailsChannels();

	/**
	 * @param cat The category to return
	 * @return All the channels of a specified category
	 */
	public abstract Set<SignageContent> getChannelCatalog( ChannelCategory cat );

	/**
	 * @return all the channels considered to be "Details"
	 */
	public abstract Map<String, SignageContent> getAllDetailsChannels();

	/**
	 * @return This display's "default" channel
	 */
	public abstract SignageContent getDefaultChannel();

}