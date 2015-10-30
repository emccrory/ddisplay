package gov.fnal.ppd.dd.util.version;

import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Date;

/**
 * Utility class that reads and writes the current version information on this project.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class VersionInformation implements Serializable {

	private static final long			serialVersionUID	= 667424596967348921L;
	private static final String			FILE_NAME			= "versionInformation.dat";
	private static final String			WEB_FILE_NAME		= getFullURLPrefix() + "/versionInformation.dat";
	private long						timeStamp			= 0L;
	private String						versionDescription	= null;
	private static ObjectOutputStream	sOutput				= null;
	private static ObjectInputStream	sInput				= null;

	/**
	 * Create an empty instance in order to save a new one in persistent storage
	 */
	public VersionInformation() {
	}

	/**
	 * @return the timeStamp of the latest version of the project
	 */
	public long getTimeStamp() {
		return timeStamp;
	}

	/**
	 * @param timeStamp
	 *            the timeStamp to set for the current version of the project
	 */
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * @return the complete description of the project
	 */
	public String getVersionDescription() {
		return versionDescription;
	}

	/**
	 * @param versionDescription
	 *            the complete description to set for this project
	 */
	public void setVersionDescription(String versionDescription) {
		this.versionDescription = versionDescription;
	}

	/**
	 * @return the most recent save of this persistent object
	 */
	public static VersionInformation getVersionInformation() {
		try {
			InputStream in = new FileInputStream(FILE_NAME);
			sInput = new ObjectInputStream(in);

			Object read = sInput.readObject();

			if (read instanceof VersionInformation) {
				return (VersionInformation) read;
			} else {
				System.err.println("unexpectedly got an object of type " + read.getClass().getCanonicalName());
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return new VersionInformation();
	}

	/**
	 * @return the most recent save of this persistent object as stored on the project's web site
	 */
	public static VersionInformation getWebVersionInformation() {
		try {
			InputStream in = new URL(WEB_FILE_NAME).openStream();
			sInput = new ObjectInputStream(in);
			Object read = sInput.readObject();

			if (read instanceof VersionInformation) {
				return (VersionInformation) read;
			} else {
				System.err.println("unexpectedly got an object of type " + read.getClass().getCanonicalName());
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return new VersionInformation();
	}

	/**
	 * @param vi
	 *            The instance of this object to save
	 */
	public static void saveVersionInformation(final VersionInformation vi) {

		try {
			if (sOutput == null) {
				OutputStream out = new FileOutputStream(FILE_NAME);
				sOutput = new ObjectOutputStream(out);
			}
			sOutput.writeObject(vi);
			sOutput.reset();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String toString() {
		return timeStamp + "\n" + versionDescription;
	}

	public static void main(final String[] args) {
		// For saving ...
		// VersionInformation vi = new VersionInformation();
		// vi.setTimeStamp(System.currentTimeMillis());
		// vi.setVersionDescription("Version description is here.  Blah, blah, blah.");
		// saveVersionInformation(vi);

		// for retrieving ...
		VersionInformation vi = getVersionInformation();
		System.out.println(vi);
		System.out.println("Time stamp: " + new Date(vi.getTimeStamp()));
	}
}
