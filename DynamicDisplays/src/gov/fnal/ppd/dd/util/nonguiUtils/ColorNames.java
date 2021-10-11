package gov.fnal.ppd.dd.util.nonguiUtils;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.interfaces.DatabaseNotVisibleException;

/**
 * Handles the Map of color names and hex codes.
 * 
 * In the early days of the system, it was imagined that it would be tricky for the operator of the channel selector to keep track
 * of which display is which in a large setting, like the ROC-West. To that end, we invented the concept of a color for each
 * display. And, further, we thought that having the colors be the same from large room to large room would be required.
 * 
 * This has not really been the case.
 * 
 * Colors are nice, and they help a little. But naming the colors is an extra bit of complexity that can (realistically) be removed.
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
