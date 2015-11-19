package gov.fnal.ppd.dd.util.version;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;

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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Utility class that reads and writes the current version information on this project. It is stored as a streamed object both
 * locally and on the web server.
 * 
 * Rewrite to have all of this in the database **AND** in a local file.
 * 
 * The GIT hash code cannot be part of this class. If we were to create a new "version number" and we want this file to represent
 * the current version number of the repository when a client machine clones the repository, we won't know the new GIT hash code to
 * put into this file until this file is saved and committed to GIT.
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

	/**
	 * What is the disposition/"flavor" of this version?
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	public static enum FLAVOR {
		/**
		 * The first level of the disposition and the default for all new versions.
		 */
		DEVELOPMENT,

		/**
		 * The first level of the disposition that could be used in the field, for testing
		 */
		TEST,

		/**
		 * The final version, which has been "thoroughly tested" (I am being optimistic here)
		 */
		PRODUCTION
	};

	// Attributes
	private long	timeStamp			= 0L;
	private String	versionDescription	= null;
	private int[]	dotVersion			= new int[3];
	private FLAVOR	disposition			= FLAVOR.DEVELOPMENT;

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
	 * @return the disposition
	 */
	public FLAVOR getDisposition() {
		return disposition;
	}

	/**
	 * @param disposition
	 *            the disposition to set
	 */
	public void setDisposition(FLAVOR disposition) {
		this.disposition = disposition;
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
	 * @param val
	 *            The value for this version field
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 83;
		int result = 1;
		result = prime * result + ((disposition == null) ? 0 : disposition.hashCode());
		result = prime * result + Arrays.hashCode(dotVersion);
		result = prime * result + (int) (timeStamp ^ (timeStamp >>> 32));
		result = prime * result + ((versionDescription == null) ? 0 : versionDescription.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
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
		if (disposition != other.disposition)
			return false;

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
	 * @return the most recent saved version of this persistent object from a file in the local filesystem
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
	 * @param f
	 *            The sort of version to retrieve
	 * @return the most recent save of this persistent object as stored on the project's web site
	 */
	public static VersionInformation getDBVersionInformation(FLAVOR f) {
		VersionInformation vi = null;

		Connection connection;
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {

					String query = "SELECT * from GitHashDecode ORDER BY HashDate DESC LIMIT 1";
					if (f != null)
						query = "SELECT * from GitHashDecode WHERE Flavor='" + f + "' ORDER BY HashDate DESC LIMIT 1";

					try (ResultSet rs = stmt.executeQuery(query);) {
						if (rs.first()) { // Move to first returned row
							do {
								Date timeStampSQL = rs.getTimestamp("HashDate");
								FLAVOR disp = FLAVOR.valueOf(rs.getString("Flavor"));
								String dotVersion = rs.getString("Version");
								String description = rs.getString("Description");

								vi = new VersionInformation();
								vi.setTimeStamp(timeStampSQL.getTime());
								vi.setDisposition(disp);
								String[] vs = (dotVersion.split("\\."));
								vi.setVersionVal(0, Integer.parseInt(vs[0]));
								vi.setVersionVal(1, Integer.parseInt(vs[1]));
								vi.setVersionVal(2, Integer.parseInt(vs[2]));
								vi.setVersionDescription(description);

							} while (rs.next()); // Relly, there should only be one
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return vi;
	}

	/**
	 * @param vi
	 *            The instance of this object to save to file
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

	/**
	 * @param vi
	 *            The Version to save in the DB
	 * @param gitHashCode
	 *            the present value of the GIT-generated hash code.
	 */
	public static void saveDBVersionInformation(final VersionInformation vi, String gitHashCode) {
		Connection connection;
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {

					Calendar c = new GregorianCalendar();
					c.setTimeInMillis(vi.getTimeStamp());
					c.setTimeZone(TimeZone.getDefault());

					String dateString = c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH) + "-" + c.get(Calendar.DAY_OF_MONTH)
							+ " " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);

					String query = "INSERT INTO GitHashDecode (HashCode, HashDate, Flavor, Version, Description) VALUES (" + //
							"'" + gitHashCode + "', " + //
							"'" + dateString + "', " + //
							"'" + vi.getDisposition() + "', " + //
							"'" + vi.getVersionString() + "', " + //
							"'" + vi.getVersionDescription() + "'" + //
							")";

					stmt.executeUpdate(query);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String toString() {
		return getVersionString() + " " + timeStamp + " " + getDisposition() + "\n" + versionDescription;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		VersionInformation vi;
		int which = 0;
		String hash = "";

		if (args.length > 0 && args[0].length() > 1) {
			hash = args[0];
			which = 3;
		} else if (args.length > 0) {
			which = Integer.parseInt(args[0]);
		}

		switch (which) {
		case 0:
			// READ from disk
			System.out.println("Reading version information from the local disk");
			vi = getVersionInformation();
			System.out.println(vi);
			System.out.println(new Date(vi.getTimeStamp()));
			break;

		case 1:
			// WRITE to disk
			break;

		case 2:
			// READ from database
			System.out.println("Reading version information from the DB");
			credentialsSetup();

			vi = getDBVersionInformation(FLAVOR.DEVELOPMENT);
			System.out.println(vi);
			System.out.println(new Date(vi.getTimeStamp()));
			break;

		case 3:
			// WRITE to database
			System.out.println("First, reading version information from the local disk");
			vi = getVersionInformation();
			System.out.println("... Now saving the version to DB, hashCode='" + hash + "'");
			saveDBVersionInformation(vi, hash);
			break;
		}
		// For saving ...
		// VersionInformation vi = new VersionInformation();
		// vi.setTimeStamp(System.currentTimeMillis());
		// vi.setVersionDescription("Version description is here.  Blah, blah, blah.");
		// vi.setVersionVal(0, 2);
		// vi.setVersionVal(1, 1);
		// vi.setVersionVal(2, 10);
		//
		// saveVersionInformation(vi);

		// for retrieving ...
		// VersionInformation vi = getVersionInformation();
		// System.out.println(vi);
		// System.out.println("Time stamp: " + new Date(vi.getTimeStamp()));

		// From database

	}
}
