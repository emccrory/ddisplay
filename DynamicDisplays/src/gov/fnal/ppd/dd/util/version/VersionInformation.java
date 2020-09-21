package gov.fnal.ppd.dd.util.version;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import gov.fnal.ppd.dd.CredentialsNotFoundException;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;

/**
 * Utility class that reads and writes the current version information on this project. It is stored as a streamed object both
 * locally and on the web server.
 * 
 * Rewrite to have all of this in the database **AND** in a local file.
 * 
 * Changed to be an XML document (not a serialized Java object).
 * 
 * The GIT hash code cannot be part of this class. If we were to create a new "version number" and we want this file to represent
 * the current version number of the repository when a client machine clones the repository, we won't know the new GIT hash code to
 * put into this file until this file is saved and committed to GIT.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@XmlRootElement
public class VersionInformation implements Serializable {

	private static boolean		AS_XML				= true;

	private static final long	serialVersionUID	= 667424596967348921L;
	private static final String	FILE_NAME;
	private static final String	WEB_FILE_NAME;
	private static String		LOCAL_FILE_NAME;

	static {
		if (AS_XML) {
			FILE_NAME = "versionInformation.xml";
		} else {
			FILE_NAME = "versionInformation.dat";
		}
		WEB_FILE_NAME = getFullURLPrefix() + File.separator + FILE_NAME;
		LOCAL_FILE_NAME = "config" + File.separator + FILE_NAME;
		File t = new File(LOCAL_FILE_NAME);
		if (!t.exists()) {
			LOCAL_FILE_NAME = ".." + File.separator + "config" + File.separator + FILE_NAME;
			t = new File(LOCAL_FILE_NAME);
			if (!t.exists()) {
				printlnErr(VersionInformation.class,
						"Cannot find the local version information file.  Tried:\n\t" + getFullURLPrefix() + File.separator
								+ FILE_NAME + " and \n\t" + ".." + File.separator + "config" + File.separator + FILE_NAME);
				System.exit(-1);
			}
		}
	}

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
	private int[]	dotVersion			= { -1, -1, -1 };
	private FLAVOR	disposition			= FLAVOR.PRODUCTION;
	private String	versionString		= null;

	/**
	 * Create an empty instance in order to save a new one in persistent storage
	 */
	public VersionInformation() {
	}

	/**
	 * @return the timeStamp of the latest version of the project
	 */
	@XmlElement
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
	@XmlElement
	public FLAVOR getDisposition() {
		return disposition;
	}

	/**
	 * @param disposition
	 *            the disposition to set
	 */
	public void setDisposition(FLAVOR disposition) {
		if (disposition != null)
			this.disposition = disposition;
	}

	/**
	 * @return the complete description of the project
	 */
	@XmlElement
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
	@XmlElement
	public String getVersionString() {
		String retval = "";
		if (dotVersion[0] == -1) {
			parseDotVersion();
		}
		for (int I : dotVersion)
			retval += I + ".";
		return retval.substring(0, retval.length() - 1);
	}

	private void parseDotVersion() {
		try {
			String dots[] = versionString.split("\\.");
			for (int i = 0; i < 3; i++)
				dotVersion[i] = Integer.parseInt(dots[i]);
		} catch (Exception e) {
			System.err.println(versionString);
			e.printStackTrace();
		}
	}

	public void setVersionString(String vs) {
		versionString = vs;
	}

	/**
	 * @param field
	 *            the dot field to increment
	 * @param val
	 *            The value for this version field
	 */
	public void setVersionVal(final int field, int val) {
		System.out.println(hashCode() + " Setting field " + field + " to " + val);
		dotVersion[field] = val;
	}

	/**
	 * 
	 * @param field
	 *            Which dot field to return
	 * @return The value of the dot field
	 */
	public int getVersionVal(int field) {
		if (dotVersion[0] == -1)
			parseDotVersion();
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
	 * Get the XML string that describes this VersionInformation object
	 * 
	 * @param vi
	 *            The object to describe
	 * @return The string that describes this object
	 */
	public static String getXMLString(VersionInformation vi) {
		try {
			return MyXMLMarshaller.getXML(vi);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return the most recent saved version of this persistent object from a file in the local file system
	 */
	public static VersionInformation getVersionInformation() {
		try {
			// if (AS_XML) {
			byte[] b = Files.readAllBytes(Paths.get(LOCAL_FILE_NAME));
			return (VersionInformation) MyXMLMarshaller.unmarshall(VersionInformation.class, new String(b));
			// } else {
			// InputStream in = new FileInputStream(LOCAL_FILE_NAME);
			// ObjectInputStream sInput = new ObjectInputStream(in);
			// read = sInput.readObject();
			// sInput.close();
			// sInput = null;
			// }
		} catch (Exception e) {
			System.err.println("Cannot find the version information file, " + LOCAL_FILE_NAME);
			e.printStackTrace();
		}

		// Return a meaningless object!
		return new VersionInformation();
	}

	/**
	 * @return the most recent save of this persistent object as stored on the project's web site
	 */
	public static VersionInformation getWebVersionInformation() {
		try {
			InputStream in = new URL(WEB_FILE_NAME).openStream();
			Object read = null;

			if (AS_XML) {
				String theXMLDocument = "";
				BufferedReader receiver = new BufferedReader(new InputStreamReader(in));
				String receiveMessage;
				try {
					while ((receiveMessage = receiver.readLine()) != null) {
						theXMLDocument += receiveMessage + "\n";
					}
				} catch (Exception e) {
					// Expect an end of file exception.
				}
				try {
					read = MyXMLMarshaller.unmarshall(VersionInformation.class, theXMLDocument);
					return (VersionInformation) read;
				} catch (JAXBException e) {

				}

			} else {
				ObjectInputStream sInput = new ObjectInputStream(in);
				read = sInput.readObject();
			}
			if (read instanceof VersionInformation) {
				return (VersionInformation) read;
			}
			System.err.println("unexpectedly got an object of type " + read.getClass().getCanonicalName());

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
		assert (f != null);
		VersionInformation vi = null;

		Connection connection;
		try {
			connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {

					String whereClause = "";
					switch (f) {
					case PRODUCTION:
						whereClause += "WHERE Flavor='" + FLAVOR.PRODUCTION + "'";
						break;

					case TEST:
						whereClause += "WHERE Flavor='" + FLAVOR.PRODUCTION + "' OR Flavor='" + FLAVOR.TEST + "'";
						break;

					case DEVELOPMENT:
						break;
					}
					String query = "SELECT HashDate,Flavor,Version,Description from GitHashDecode " + whereClause
							+ " ORDER BY HashDate DESC LIMIT 1";

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

							} while (rs.next()); // Really, there should only be
													// one
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
			FileOutputStream fos = new FileOutputStream(LOCAL_FILE_NAME);
			System.out.println("Saving version information to the XML file " + new File(LOCAL_FILE_NAME).getPath());

			// if (AS_XML) {
			fos.write(getXMLString(vi).getBytes());
			fos.close();
			fos = null;
			// } else {
			// ObjectOutputStream sOutput = new ObjectOutputStream(fos);
			// sOutput.writeObject(vi);
			// sOutput.reset();
			// sOutput.close();
			// sOutput = null;
			// }
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
		String query = "";
		try {
			connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {

					Calendar c = new GregorianCalendar();
					c.setTimeInMillis(vi.getTimeStamp());
					c.setTimeZone(TimeZone.getDefault());

					String dateString = c.get(Calendar.YEAR) + "-" + (1 + c.get(Calendar.MONTH)) + "-"
							+ c.get(Calendar.DAY_OF_MONTH) + " " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":"
							+ c.get(Calendar.SECOND);

					query = "INSERT INTO GitHashDecode (HashCode, HashDate, Flavor, Version, Description) VALUES (" + //
							"'" + gitHashCode + "', " + //
							"'" + dateString + "', " + //
							"'" + vi.getDisposition() + "', " + //
							"'" + vi.getVersionString() + "', " + //
							"'" + vi.getVersionDescription().replace("'", "\\'") + "'" + //
							")";

					stmt.executeUpdate(query);
				}
			}
		} catch (Exception e) {
			System.err.println("Query was [" + query + "]");
			e.printStackTrace();
		}
	}

	public String toString() {
		return "[" + getVersionString() + ", " + timeStamp + ", " + getDisposition()
				+ "]\nDescription of the commit for this version number:\n" + versionDescription;
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
			if (which == 3) {
				System.err.println("Cannot ask for option=3 - supply a hash value instead.");
				System.exit(-1);
			}
		}

		FLAVOR flav = FLAVOR.PRODUCTION;

		switch (which) {
		case 0:
			// READ from disk
			System.out.println("Reading version information from the local disk");
			vi = getVersionInformation();
			System.out.println(vi);
			System.out.println("Date of this version: " + new Date(vi.getTimeStamp()));
			break;

		case 1:
			// WRITE to disk - ONLY FOR TESTING (it sets the version number to
			// 4.x.x)
			System.out.println("First, reading version information from the local disk");
			vi = getVersionInformation();
			flav = FLAVOR.TEST;
			if (args.length >= 2) {
				flav = FLAVOR.valueOf(args[1]);
			}
			vi.setDisposition(flav);
			vi.setVersionVal(1, 4);
			if (args.length >= 3) {
				vi.setVersionString(args[2]);
			}
			System.out.println("... Saving to disk, flavor = " + flav + ", version = " + vi.getVersionString());
			saveVersionInformation(vi);
			System.out.println(" .. now re-reading ...");

			System.out.println("Reading version information from the local disk");
			vi = getVersionInformation();
			System.out.println(vi);
			System.out.println("Date of this version: " + new Date(vi.getTimeStamp()));
			break;

		case 2:
			// READ from database
			System.out.println("Reading version information from the DB");
			try {
				credentialsSetup();
			} catch (CredentialsNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			vi = getDBVersionInformation(flav);
			System.out.println(vi);
			System.out.println(new Date(vi.getTimeStamp()));
			break;

		case 3:
			// WRITE to database - We have been supplied with a hash
			System.out.println("Will write the new Hash value to the DB based on the information in " + LOCAL_FILE_NAME
					+ ". \n... First, read that file.");
			vi = getVersionInformation();

			try {
				credentialsSetup();
			} catch (CredentialsNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			System.out.println("... Now saving the version (" + vi.getVersionString() + ", " + new Date(vi.getTimeStamp())
					+ ") to DB, hashCode='" + hash + "'");
			saveDBVersionInformation(vi, hash);
			System.out.println("... DB save complete.");
			break;

		case 4:
			// READ from database, but succinct output
			try {
				credentialsSetup();
			} catch (CredentialsNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			vi = getDBVersionInformation(flav);
			System.out.println(vi.getVersionString() + " " + vi.getDisposition());
			break;

		case 5:
			// Read from database and write to an XML file
			System.out.println("Reading version information from the DB");
			try {
				credentialsSetup();
			} catch (CredentialsNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			vi = getDBVersionInformation(flav);
			String xml = getXMLString(vi);
			System.out.println(xml);

		}
		// For saving ...
		// VersionInformation vi = new VersionInformation();
		// vi.setTimeStamp(System.currentTimeMillis());
		// vi.setVersionDescription("Version description is here. Blah, blah,
		// blah.");
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
