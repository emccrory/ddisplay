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
import java.util.Arrays;

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
	private static ObjectOutputStream	sOutput				= null;
	private static ObjectInputStream	sInput				= null;

	// Attributes
	private long						timeStamp			= 0L;
	private String						versionDescription	= null;
	private int[]						dotVersion			= new int[3];

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
	 * @return The version as a dot string, e.g., "2.4.109"
	 */
	public String getVersionString() {
		String retval = "";
		for (int I : dotVersion)
			retval += I + ".";
		return retval.substring(0, retval.length() - 1);
	}

	/**
	 * @param field
	 *            the dot field to increment
	 */
	public void setVersionVal(final int field, int val) {
		dotVersion[field] = val;
	}

	/**
	 * 
	 * @param field
	 *            Which dot field to return
	 * @return The value of the dot field
	 */
	public int getVersionVal(int field) {
		return dotVersion[field];
	}

	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 73;
		int result = 1;
		result = prime * result + Arrays.hashCode(dotVersion);
		result = prime * result + (int) (timeStamp ^ (timeStamp >>> 32));
		result = prime * result + ((versionDescription == null) ? 0 : versionDescription.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof VersionInformation))
			return false;
		VersionInformation other = (VersionInformation) obj;
		if (timeStamp != other.timeStamp)
			return false;
		if (!Arrays.equals(dotVersion, other.dotVersion))
			return false;
		if (versionDescription == null) {
			if (other.versionDescription != null)
				return false;
		} else if (!versionDescription.equals(other.versionDescription))
			return false;
		return true;
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
		 VersionInformation vi = new VersionInformation();
		 vi.setTimeStamp(System.currentTimeMillis());
		 vi.setVersionDescription("Version description is here.  Blah, blah, blah.");
		 vi.setVersionVal(0, 2);
		 vi.setVersionVal(1, 1);
		 vi.setVersionVal(2, 10);
		 
		 saveVersionInformation(vi);

		// for retrieving ...
		//VersionInformation vi = getVersionInformation();
		//System.out.println(vi);
		//System.out.println("Time stamp: " + new Date(vi.getTimeStamp()));
	}
}
