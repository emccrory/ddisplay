package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.util.Util.println;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Initialize the Firefox browser and the pmorch plugin
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class BrowserLauncherFirefox extends BrowserLauncher {

	private static final String	FIREFOX_USER	= "DynamicDisplay";
	private Process				browserProcess	= null;

	/**
	 * @param screenNumber
	 */
	public BrowserLauncherFirefox(int screenNumber) {
		super(screenNumber);
		println(getClass(), ": Launching Firefox browser on screen #" + screenNumber);

	}

	@Override
	public void startBrowser() {
		String firefoxUser = FIREFOX_USER;
		if (screenNumber > 0) {
			firefoxUser += (screenNumber + 1);
		}
		if (browserProcess != null)
			return;

		String geom = bounds.width + "x" + bounds.height + "+" + (int) bounds.getX() + "+" + (int) bounds.getY();

		try {
			// extensions.remotecontrol.portNumber is the environment variable that gives the port number, I think (9/15/2014)
			// I tried to set it (and several variations) using System.setProperty(), to no avail.
			File file = new File("remotecontrol_plugin_messages_for_" + firefoxUser + ".log");
			ProcessBuilder pb = new ProcessBuilder("firefox", "-new-instance", "-P", firefoxUser, "-remote-control",
					"-geometry=" + geom);
			pb.redirectErrorStream(true);
			pb.redirectOutput(file);
			List<String> c = pb.command();
			browserProcess = pb.start();

			println(getClass(), ": Firefox launch command: '");
			for (String S : c) {
				System.out.print(S + " ");
			}
			System.out.println("'");

			if (debug)
				println(getClass(), ": Launched Firefox browser, geometry=" + geom);
		} catch (IOException e) {
			// This probably failed because it could not find the browser executable.
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public void exit() {
		if (browserProcess != null) {
			browserProcess.destroy();
		}
	}
}
