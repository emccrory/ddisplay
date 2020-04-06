package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.util.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.GeneralUtilities.printlnErr;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import gov.fnal.ppd.dd.db.ConnectionToDatabase;

/**
 * Handles the Map of color names and hex codes
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class ColorNames extends HashMap<String, String> {

	private static final long serialVersionUID = 1727549736099975272L;

	/**
	 * Create the default color names map
	 */
	public ColorNames() {
		Statement stmt = null;
		ResultSet rs = null;
		Connection connection;
		int count = 0;

		try {
			connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				stmt = connection.createStatement();
				rs = stmt.executeQuery("USE " + DATABASE_NAME);
				String q = "SELECT ColorCode,ColorName FROM ColorNames";
				rs = stmt.executeQuery(q);
				if (rs.first()) { // Move to first returned row
					while (!rs.isAfterLast())
						try {
							String code = rs.getString("ColorCode");
							String name = rs.getString("ColorName");

							put(code, name);
							rs.next();
							count++;
						} catch (Exception e) {
							e.printStackTrace();
						}
					stmt.close();
					rs.close();
				} else
					printlnErr(getClass(), "Executed a query that returned no rows: " + q);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
			System.exit(1);
		}

		println(getClass(), ": Found " + count + " color names.");
	}

	public String toString() {
		String retval = "{";
		for (String N : keySet())
			retval += "[" + N + "->" + get(N) + "]";
		return retval;
	}
}
