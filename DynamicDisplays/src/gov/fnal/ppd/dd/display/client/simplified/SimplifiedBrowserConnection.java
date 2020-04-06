package gov.fnal.ppd.dd.display.client.simplified;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import gov.fnal.ppd.dd.db.GetDefaultContentForDisplay;
import gov.fnal.ppd.dd.display.client.ConnectionToBrowserInstance;
import gov.fnal.ppd.dd.util.nonguiUtils.PropertiesFile;

public class SimplifiedBrowserConnection extends ConnectionToBrowserInstance {

	private Process	browserProcess;

	private int		channelNumber	= 0;

	public SimplifiedBrowserConnection() {
		channelNumber = 0;
	}

	public SimplifiedBrowserConnection(int n) {
		channelNumber = n;
	}

	@Override
	public void openConnection() {
		connected = false;
		String osName = System.getProperty("os.name").toUpperCase();
		try {
			String browserLocation = PropertiesFile.getProperty("binLinux");

			if (osName.toUpperCase().contains("WINDOWS")) {
				browserLocation = PropertiesFile.getProperty("binWindows");
			} else if (osName.toUpperCase().contains("MAC")) {
				browserLocation = PropertiesFile.getProperty("binMac");
			}

			if (channelNumber == 0) {
				channelNumber = new GetDefaultContentForDisplay().getChannelNumber();
			}
			String thing = "";
			if (channelNumber < 0) {
				thing = browserLocation + " --new-instance --new-window https://dynamicdisplays.fnal.gov/playChannelList.php?n="
						+ (-channelNumber);
			} else {
				thing = browserLocation + " --new-instance --new-window https://dynamicdisplays.fnal.gov/launchPageFromDB.php?n="
						+ channelNumber;
			}
			println(getClass(), "Launching browser like this: [" + thing + "]");
			browserProcess = Runtime.getRuntime().exec(thing);

			// A bit of a lie
			connected = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void exit() {
		browserProcess.destroyForcibly();
	}

	@Override
	public Object getTheReply() {
		return "No information";
	}

	@Override
	public String getConnectionCode() {
		return "Irrelevant";
	}

	@Override
	public void send(String javaScriptCommands) {
		// TODO - When we receive a string that would change the channel, we need to restart the browser.
		// It look like this is too far down - we'll need to catch this higher up, e.g., when we have the Channel List in hand.
		throw new RuntimeException("This method, " + getClass().getCanonicalName() + "send(), should not have been called.");
	}

}
