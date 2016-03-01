package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

/**
 * Look up a hash code in the Dynamic Displays database and see if there is a time stamp that matches it.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class TranslateGITHashCodeToDate {
	private String	hashCode	= null;
	private Date	timeStamp	= null;
	private String	version		= null;
	private String	flavor		= null;

	/**
	 * Simply return the newest hash code and time stamp
	 */
	public TranslateGITHashCodeToDate() {
		try {
			getNewest();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param code
	 *            The hash code to find
	 */
	public TranslateGITHashCodeToDate(final String code) {
		hashCode = code;
	}

	/**
	 * @return the hashCode
	 */
	public String getHashCode() {
		return hashCode;
	}

	/**
	 * @param hashCode
	 *            the hashCode to set
	 */
	public void setHashCode(String hashCode) {
		this.hashCode = hashCode;
		timeStamp = null;
	}

	/**
	 * @return the timeStamp
	 * @throws Exception
	 *             -- If the provided hash is not in the DB.
	 */
	public Date getTimeStamp() throws Exception {
		getVersioning();

		return timeStamp;

	}

	private void getVersioning() throws Exception {
		if (timeStamp == null || version == null || flavor == null) {
			// Look it up in the database

			try {
				Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();

				String query = "Select HashDate,Version,Flavor from GitHashDecode where HashCode='" + hashCode + "'";

				synchronized (connection) {
					try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
						try (ResultSet rs = stmt.executeQuery(query)) {
							if (rs.first())
								do {
									timeStamp = rs.getTimestamp("HashDate");
									version = rs.getString("Version");
									flavor = rs.getString("Flavor");
								} while (rs.next());
							else {
								// Oops. no first element!?
								throw new Exception("No elements in the GITHash table!");
							}
						} catch (SQLException e) {
							System.err.println(query);
							e.printStackTrace();
						}
					} catch (SQLException e) {
						System.err.println(query);
						e.printStackTrace();
					}
				}
			} catch (DatabaseNotVisibleException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public void getNewest() throws Exception {
		// Look it up in the database

		Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		String query = "Select HashDate,HashCode from GitHashDecode ORDER BY HashDate DESC LIMIT 1";

		synchronized (connection) {
			try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
				try (ResultSet rs = stmt.executeQuery(query)) {
					if (rs.first())
						do {
							timeStamp = rs.getTimestamp("HashDate");
							hashCode = rs.getString("HashCode");

						} while (rs.next());
					else {
						// Oops. no first element!?
						throw new Exception("No elements in the GITHash table!");
					}
				} catch (SQLException e) {
					System.err.println(query);
					e.printStackTrace();
				}
			} catch (SQLException e) {
				System.err.println(query);
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param timeStamp
	 *            the timeStamp to set
	 */
	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version
	 *            the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * @return the flavor
	 */
	public String getFlavor() {
		return flavor;
	}

	/**
	 * @param flavor
	 *            the flavor to set
	 */
	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	public static void main(String[] args) {
		boolean succinct = true;

		credentialsSetup();

		if (args.length == 0)
			try {
				TranslateGITHashCodeToDate hashcodeTranslation = new TranslateGITHashCodeToDate();
				if (succinct) {
					System.out.println(hashcodeTranslation.getHashCode() + "\t" + hashcodeTranslation.getTimeStamp());
				} else {
					System.out.println("For the hash code " + hashcodeTranslation.getHashCode()
							+ ".\n\tTime stamp " + hashcodeTranslation.getTimeStamp() + ".\n\tVersion number "
							+ hashcodeTranslation.getVersion() + ".\n\tFlavor: " + hashcodeTranslation.getFlavor());

				}
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		else {
			TranslateGITHashCodeToDate hashcodeTranslation = new TranslateGITHashCodeToDate(args[0]);
			for (String ARG : args)
				try {
					hashcodeTranslation.setHashCode(ARG);
					if (succinct)
						System.out.println(hashcodeTranslation.getTimeStamp() + ", Version " + hashcodeTranslation.getVersion());
					else
						System.out.println("Hash Code of '" + hashcodeTranslation.getHashCode() + "' has a timestamp of "
								+ hashcodeTranslation.getTimeStamp() + "; " + hashcodeTranslation.getFlavor() + ", "
								+ hashcodeTranslation.getVersion());
				} catch (Exception e) {
					if (succinct)
						System.out.println("(    No Time Stamp  )");
					else {
						e.printStackTrace();
						System.out.println("The hash code '" + hashcodeTranslation.getHashCode()
								+ "' has no corresponding time stamp in the DB");
					}
				}
		}
	}
}
