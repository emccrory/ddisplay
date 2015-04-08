package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2015
 * 
 */
public class GetColorsFromDatabase {
	private GetColorsFromDatabase() {
	}

	/**
	 * @return The color names from the database
	 */
	public static HashMap<String, String> get() {
		HashMap<String, String> retval = new HashMap<String, String>();
		Statement stmt = null;
		ResultSet rs = null;
		Connection connection;
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();

			stmt = connection.createStatement();
			rs = stmt.executeQuery("USE " + DATABASE_NAME);
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
		}

		int count = 0;
		try {
			rs = stmt.executeQuery("select * from ColorNames");
			rs.first(); // Move to first returned row
			while (!rs.isAfterLast())
				try {
					String code = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("ColorCode"));
					String name = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("ColorName"));

					retval.put(code, name);
					rs.next();
					count++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			stmt.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println(GetColorsFromDatabase.class.getSimpleName() + ": Found " + count + " color names.");
		return retval;
	}
}
