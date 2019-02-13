package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Get the version of the JVM that is running now. It gets this at the beginning of the JVM and allows you to see if the version has
 * changed.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class JavaVersion {

	private static final String InitialVersion = System.getProperty("java.version");

	/**
	 * What is the version of the currently-running JVM?
	 * 
	 * @return String the version
	 */
	public static String getInitialVersion() {
		return InitialVersion;
	}

	/**
	 * If we were to restart the JVM, what version would that be?
	 * 
	 * @return String the new version
	 */
	public static String getCurrentVersion() {
		// This can only work in Linux environment (but it really does not work very well, as it is now)
		
		Runtime r = Runtime.getRuntime();
		try {
			// Not sure why I cannot do exec("java -version")
			// String [] ca = {"/usr/bin/java", "-version"};
			String[] ca = { "ls", "-l", "/etc/alternatives/java" };
			// Expect the output to look like this:
			// lrwxrwxrwx. 1 root root 73 Dec 4 08:44 /etc/alternatives/java -> /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.191.b12-1.el7_6.x86_64/jre/bin/java
			Process p = r.exec(ca);
			int exitVal = p.waitFor();
			if (exitVal != 0) {
				printlnErr(JavaVersion.class, "Unexpected non-zero error return, " + exitVal);
				return null;
			}
			BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String retval = "", line = "";
			while ((line = b.readLine()) != null) {
				int startAt = line.indexOf(ca[2]) + ca[2].length() + 4;
				String subString1 = line.substring(startAt);
				startAt = subString1.indexOf("/jvm/") + 24;
				retval = subString1.substring(startAt, startAt + 9);
			}
			b.close();
			return retval;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean hasVersionChanged() {
		return !((getCurrentVersion().replace(".", "_").equals(getInitialVersion().replace(".",  "_"))));
	}

	private JavaVersion() {
		// Not instantiated
	}

	public static void main(String[] args) {
		System.out.println(getInitialVersion());

		System.out.println(getCurrentVersion());
		
		System.out.println(hasVersionChanged() ? "Changed" : "Unchanged");
	}
}
