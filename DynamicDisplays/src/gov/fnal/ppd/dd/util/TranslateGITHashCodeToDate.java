package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;

import java.math.BigInteger;
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
	private BigInteger	hashCode;
	private Date		timeStamp;

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
		hashCode = new BigInteger(code, 16);
	}

	/**
	 * @return the hashCode
	 */
	public BigInteger getHashCode() {
		return hashCode;
	}

	/**
	 * @param code
	 *            the hash code as a String
	 */
	public void setHashCode(final String code) {
		setHashCode(new BigInteger(code, 16));
	}

	/**
	 * @param hashCode
	 *            the hashCode to set
	 */
	public void setHashCode(BigInteger hashCode) {
		this.hashCode = hashCode;
		timeStamp = null;
	}

	/**
	 * @return the timeStamp
	 * @throws Exception
	 *             -- If the provided hash is not in the DB.
	 */
	public Date getTimeStamp() throws Exception {
		if (timeStamp != null)
			return timeStamp;

		// Look it up in the database

		Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		String query = "Select HashDate from GitHashDecode where HashCode='" + hashCode.toString(16) + "'";

		synchronized (connection) {
			try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
				try (ResultSet rs = stmt.executeQuery(query)) {
					if (rs.first())
						do {
							timeStamp = rs.getTimestamp("HashDate");

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

		return timeStamp;
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
							hashCode = new BigInteger(rs.getString("HashCode"), 16);

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

	public static void main(String[] args) {
		boolean succinct = true;

		credentialsSetup();

		if (args.length == 0)
			try {
				TranslateGITHashCodeToDate hashcodeTranslation = new TranslateGITHashCodeToDate();
				if (succinct) {
					System.out.println(hashcodeTranslation.getHashCode().toString(16) + "\t" + hashcodeTranslation.getTimeStamp());
				} else {
					System.out.println("The newest time stamp is " + hashcodeTranslation.getHashCode().toString(16)
							+ ".  the hash code for this time is " + hashcodeTranslation.getTimeStamp());

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
						System.out.println(hashcodeTranslation.getTimeStamp());
					else
						System.out.println("Hash Code of '" + hashcodeTranslation.getHashCode().toString(16)
								+ "' has a timestamp of " + hashcodeTranslation.getTimeStamp());
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
