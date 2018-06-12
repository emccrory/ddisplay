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

	private static final String FIREFOX_USER = "DynamicDisplay";

	/**
	 * @param screenNumber
	 */
	public BrowserLauncherFirefox(int screenNumber) {
		super(screenNumber);
		println(getClass(), ": Launching Firefox browser on screen #" + screenNumber);

	}

	@Override
	public void changeURL(String url) {

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
			// ,"javascript:%20" + moveTo, url);
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
			System.err.println("Assuming that this is Windows now.  We'll try again!");
			try {
				browserProcess = new ProcessBuilder("c:/Program Files (x86)/Mozilla Firefox/firefox.exe", "-remote-control",
						"-P " + firefoxUser, "--geometry=" + geom, "-new-instance", "-fullscreen", url).start();
				if (debug)
					System.out.println("Launched Firefox browser, geometry=" + geom);
			} catch (IOException e1) {
				System.err.println("All options exhausted -- abort");
				e1.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
