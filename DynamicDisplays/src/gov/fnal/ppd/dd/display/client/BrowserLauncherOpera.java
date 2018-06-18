package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.util.Util.println;

import java.io.IOException;

/**
 * Launch an Opera browser - this is not used or tested!!
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class BrowserLauncherOpera extends BrowserLauncher {
	private Process browserProcess = null;

	/**
	 * @param screenNumber
	 */
	public BrowserLauncherOpera(int screenNumber) {
		super(screenNumber);
		println(getClass(), ": Launching Opera browser on screen #" + screenNumber);
	}

	@Override
	public void startBrowser() {
		try {
			if (browserProcess != null) {
				browserProcess.destroy();
				Thread.sleep(100);
				browserProcess = null;
				Thread.sleep(100); // Opera needs just over a full second to kill itself completely.
				if (debug)
					System.out.println("Killed last process...");
			}
			String geom = bounds.width + "x" + bounds.height + "+" + (int) bounds.getX() + "+" + (int) bounds.getY();
			String url = "";
			browserProcess = new ProcessBuilder("opera", "-nosession", "-geometry", geom, "-fullscreen", url).start();
			if (debug)
				println(getClass(), ": Launched Opera browser, geometry=" + geom);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void exit() {
		if (browserProcess != null) {
			browserProcess.destroy();
		}
	}
}
