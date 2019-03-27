package gov.fnal.ppd.dd.util;

import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;

public interface NotificationClient {

	// When the update task starts grabbing an update, tell the client
	void showNewUpdateInformation(FLAVOR flavor, String versionString);

	// If the update task gets an error, tell the client.
	void failure();

	// When the update task succeeds, tell the client.
	void success();

}
