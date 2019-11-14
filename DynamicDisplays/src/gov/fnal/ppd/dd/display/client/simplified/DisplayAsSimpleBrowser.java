package gov.fnal.ppd.dd.display.client.simplified;

import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameDisplay;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.prepareUpdateWatcher;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Color;

import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.display.client.DisplayControllerMessagingAbstract;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.ExitHandler;
import gov.fnal.ppd.dd.util.JavaVersion;
import gov.fnal.ppd.dd.util.SendEmailCommand;
import gov.fnal.ppd.dd.util.version.VersionInformation;

public class DisplayAsSimpleBrowser extends DisplayControllerMessagingAbstract {

	public DisplayAsSimpleBrowser(String ipName, int vNumber, int dbNumber, int screenNumber, boolean showNumber, String location,
			Color color) {
		super(ipName, vNumber, dbNumber, screenNumber, showNumber, location, color);
	}

	@Override
	public void initiate() {
		println(getClass(), "Opening a browser with the default content");

		browserInstance = new SimplifiedBrowserConnection();
		browserInstance.openConnection();
		super.initiate();
	}

	@Override
	public SignageContent setContent(SignageContent c) {
		// Intercept the "change channel" directive here - none of the more detailed stuff is required for this instance.

		browserInstance.exit();
		// Do we need a delay here?

		println(getClass(), "The new channel is [" + c + "] of class " + c.getClass().getCanonicalName());
		if (c instanceof ChannelPlayList) {
			// TODO - How do we implement this?
			// At this spot in the running of the display, we only have the URL lists. The channel number (in this case, the list
			// number) was lost way back in the process. So I think we'll have to add back the list number so this can be
			// implemented down here.

			int num = ((ChannelPlayList) c).getNumber();
			println(getClass(), "Changing to list number " + num);
			browserInstance = new SimplifiedBrowserConnection(-num);
		} else {
			int num = ((Channel) c).getNumber();
			String url = ((Channel) c).getURI().toASCIIString();
			if (num != 0) {
				println(getClass(), "Changing to channel number " + num);
				browserInstance = new SimplifiedBrowserConnection(num);
			} else {
				println(getClass(), "Changing to URL " + url);
				browserInstance = new SimplifiedBrowserConnection(url);
			}
		}
		browserInstance.openConnection();
		return c;
	}

	/**
	 * @param args
	 *            Expect no command line arguments
	 */
	public static void main(final String[] args) {
		println(DisplayAsSimpleBrowser.class, "Running from java version " + JavaVersion.getCurrentVersion());

		prepareUpdateWatcher(false);

		credentialsSetup();

		getMessagingServerNameDisplay();

		try {
			System.out.println("Version: " + VersionInformation.getVersionInformation().getVersionString());

			@SuppressWarnings("unused")
			DisplayAsSimpleBrowser add = (DisplayAsSimpleBrowser) makeTheDisplays(DisplayAsSimpleBrowser.class);
			ExitHandler.addFinalCommand(new SendEmailCommand());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}