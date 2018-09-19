package gov.fnal.ppd.dd.util;

import java.io.File;
import java.io.IOException;

/**
 * Manage the system virtual keyboard. As of this writing. Scientific Linux does not have this feature most of the time, but Windows
 * does. Not sure about MAC.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class VirtualKeyboard {

	private VirtualKeyboard() {
	}

	private static Process p = null;
	private static boolean viable = false;
	private static final String OSK ="C:/Windows.osk.exe";  
	
	static {
		File f = new File(OSK);
		viable = f.exists();
	}

	public static final void launch() {
		if ( isViable() ) {
		// On Windows only - the Linux virtual keyboard is not commonly installed through Scientific Linux
		if (p == null || !p.isAlive())
			try {
				p = Runtime.getRuntime().exec("cmd /C start " + OSK);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// What if the keyboard is hidden??
	}

	public static final void remove() {
		if (p != null && p.isAlive()) {
			p.destroy();
			p = null;
		}
	}

	public static boolean isViable() {
		return viable;
	}

}
