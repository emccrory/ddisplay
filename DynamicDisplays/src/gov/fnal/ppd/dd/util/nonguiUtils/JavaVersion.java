package gov.fnal.ppd.dd.util.nonguiUtils;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Get the version of the JVM that is running now. It gets this at the beginning of the JVM and allows you to see if the version has
 * changed.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class JavaVersion {

	private static JavaVersion	me				= null;
	private static boolean		watch			= true;
	private final String		InitialVersion	= System.getProperty("java.version");

	public static JavaVersion getInstance() {
		if (me == null)
			me = new JavaVersion(watch);
		return me;
	}

	/**
	 * What is the version of the currently-running JVM?
	 * 
	 * @return String the version
	 */
	private String getInitialVersion() {
		return InitialVersion;
	}

	/**
	 * What is the version of the currently-running JVM?
	 * 
	 * @return String the version
	 */
	public String get() {
		return getInitialVersion();
	}

	/**
	 * Get the Java version from running the command 'java -version' and then parsing the output
	 * 
	 * @return The Java version, like "1.8.0_123"
	 */
	private String executeJavaCommand() {
		try {

			// Expect the output to look like this:
			// openjdk version "1.8.0_272"

			Process process = Runtime.getRuntime().exec(new String[] { "/usr/bin/java", "-version" });

			StringBuilder output = new StringBuilder();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			String line;
			String firstLine = null;
			while ((line = reader.readLine()) != null) {
				if (firstLine == null)
					firstLine = line;
				output.append(line + "\n");
			}

			int exitVal = process.waitFor();
			if (exitVal == 0) {
				return firstLine.replace("openjdk version ", "").replace("\"", "");
			} else {
				return "Error";
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "Not expected";
	}

	/**
	 * Get the Java version number from looking at the name of the folder in which the java commands sits. This is likely to be
	 * brittle.
	 * 
	 * @return String the new version
	 */
	private String getCurrentVersionThroughLS() {
		// This can only work in Linux environment (and it assumes a lot about the way the ls command works)

		boolean isWindows = System.getProperty("os.name").toUpperCase().contains("WINDOWS");
		if (isWindows) {
			println(JavaVersion.class,
					"Microsoft Windows installation: We haven't implemented a way to determine accurately which version of Java is installed.");
			return "Not Known (Running Windows)";
		}
		Runtime r = Runtime.getRuntime();
		try {
			// Not sure why I cannot do exec("java -version") - *SOLVED* The '-version' argument send output to stderr
			String[] ca = { "ls", "-l", "/etc/alternatives/java" };
			// Expect the output to look like this:
			// lrwxrwxrwx. 1 root root 73 Dec 4 08:44 /etc/alternatives/java ->
			// /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.191.b12-1.el7_6.x86_64/jre/bin/java
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
			// Windows comes here because there is no "ls" command.
			e.printStackTrace();
		}
		System.out.println("Hmmm");
		return null;
	}

	/**
	 * Get the version of Java as determined from outside of the JVM (that is, through the operating system)
	 * 
	 * @return The Java version, like "1.8.0_123"
	 */
	public String getCurrentVersion() {
		// return getCurrentVersionThroughLS();
		return executeJavaCommand();
	}

	/**
	 * ONE_MINUTE, ONE_HOUR Is the Java version in the JVM different from the Java version if we were to restart the JVM?
	 * 
	 * @return The version is unchanged?
	 */
	public boolean hasVersionChanged() {
		return !((getCurrentVersion().replace(".", "_").equals(getInitialVersion().replace(".", "_"))));
	}

	private JavaVersion(Boolean doWatch) {
		if (doWatch) {
			println(getClass(), "Initiating watch for the Java version to change away from " + getInitialVersion());
			TimerTask t = new TimerTask() {
				@Override
				public void run() {
					if (hasVersionChanged()) {
						System.out.println("The java version has changed from " + getInitialVersion() + " to "
								+ executeJavaCommand() + ". This app should be restarted.");

						for (JavaChangeListener jcl : listener)
							jcl.javaHasChanged();

						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						System.exit(-1);
					} else {
						System.out.println(
								new Date() + ": The Java version, " + executeJavaCommand() + ", remains " + getInitialVersion());
					}
				}
			};

			Timer timer = new Timer();
			timer.scheduleAtFixedRate(t, ONE_HOUR, ONE_HOUR);
		}
	}

	private List<JavaChangeListener> listener = new ArrayList<JavaChangeListener>();

	public void addJavaChangeListener(JavaChangeListener jcl) {
		listener.add(jcl);
	}

	public static void main(String[] args) {
		watch = false;
		System.out.println("The version as seen through the JVM:           " + getInstance().getInitialVersion());

		System.out.println("The version as seen from the operating system: " + getInstance().getCurrentVersionThroughLS());

		System.out
				.println("(Sanity check) The Java version " + (getInstance().hasVersionChanged() ? "has changed" : "is unchanged"));

		// Check to see that this causes the Jenkins task to fail.
		System.exit(-1);
	}
}
