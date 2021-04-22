package gov.fnal.ppd.dd.interfaces;

import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;

/**
 * This interface allows the client to show update information during the process of doing the update. There are two concrete
 * classes: A GUI class and a text class. The GUI class is used by the ChannelSelector and the Displays, and the text client is used
 * by the MessagingServer.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public interface NotificationClient {

	// When the update task starts grabbing an update, tell the client
	void showNewUpdateInformation(FLAVOR flavor, String versionString);

	// If the update task gets an error, tell the client.
	void failure();

	// When the update task succeeds, tell the client.
	void success();

}
