package gov.fnal.ppd.dd.display.client.simplified;

import static gov.fnal.ppd.dd.util.Util.println;

import gov.fnal.ppd.dd.db.GetDefaultContentForDisplay;
import gov.fnal.ppd.dd.display.client.ConnectionToBrowserInstance;
import gov.fnal.ppd.dd.util.PropertiesFile;

public class SimplifiedBrowserConnection extends ConnectionToBrowserInstance {

	private Process	browserProcess;
	private String	browserLocation	= null;
	private int		channelNumber	= 0;
	private String	channelURL		= null;

	public SimplifiedBrowserConnection() {
		channelNumber = 0;
		channelURL = null;
	}

	public SimplifiedBrowserConnection(int n) {
		channelNumber = n;
		channelURL = null;
	}

	public SimplifiedBrowserConnection(String url) {
		channelURL = url;
		channelNumber = 0;
	}

	@Override
	public void openConnection() {
		String osName = System.getProperty("os.name").toUpperCase();

		try {
			browserLocation = PropertiesFile.getProperty("binLinux");

			if (osName.toUpperCase().contains("WINDOWS")) {
				browserLocation = PropertiesFile.getProperty("binWindows");
			} else if (osName.toUpperCase().contains("MAC")) {
				browserLocation = PropertiesFile.getProperty("binMac");
			}

			if (channelURL == null) {
				if (channelNumber == 0) {
					channelNumber = new GetDefaultContentForDisplay().getChannelNumber();
				}
				String thing = "";
				if (channelNumber < 0) {
					thing = browserLocation + " --new-instance --new-window https://dynamicdisplays.fnal.gov/playChannelList.php?n="
							+ (-channelNumber);
				} else {
					thing = browserLocation
							+ " --new-instance --new-window https://dynamicdisplays.fnal.gov/launchPageFromDB.php?n="
							+ channelNumber;
				}
				println(getClass(), "Launching browser like this: [" + thing + "]");
				browserProcess = Runtime.getRuntime().exec(thing);
			} else {
				browserProcess = Runtime.getRuntime().exec(browserLocation + " --new-instance --new-window " + channelURL);
			}

			// Launch the browser with the default channel

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void exit() {
		// TODO Auto-generated method stub
		browserProcess.destroyForcibly();
	}

	@Override
	public Object getTheReply() {
		return "It is cool";
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
