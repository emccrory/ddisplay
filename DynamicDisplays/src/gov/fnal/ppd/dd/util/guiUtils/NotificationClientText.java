package gov.fnal.ppd.dd.util.guiUtils;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;

import gov.fnal.ppd.dd.interfaces.NotificationClient;
import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;

/** Non-GUI clients, e.g., the messaging server, need this class for the update process. */
public class NotificationClientText implements NotificationClient {

	// I am not sure that we need to do anything (much) here.

	@Override
	public void showNewUpdateInformation(FLAVOR flavor, String versionString) {
		println(getClass(), "Starting an update, version=" + versionString + ", flavor=" + flavor);
	}

	@Override
	public void failure() {
		printlnErr(getClass(), "The update failed!");
	}

	@Override
	public void success() {
		println(getClass(), "The update succeeded");
	}

}
