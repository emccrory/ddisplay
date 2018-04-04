/*
 * GetColorsFromDatabase
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Retrieve the color names from the DD database.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class GetColorsFromDatabase {
	private GetColorsFromDatabase() {
	}

	/**
	 * @return The color names from the database
	 */
	public static Map<String, String> get() {
		Map<String, String> retval = new HashMap<String, String>();
		Statement stmt = null;
		ResultSet rs = null;
		Connection connection;
		int count = 0;

		try {
			connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				stmt = connection.createStatement();
				rs = stmt.executeQuery("USE " + DATABASE_NAME);
				rs = stmt.executeQuery("select * from ColorNames");
				rs.first(); // Move to first returned row
				while (!rs.isAfterLast())
					try {
						String code = rs.getString("ColorCode");
						String name = rs.getString("ColorName");

						retval.put(code, name);
						rs.next();
						count++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				stmt.close();
				rs.close();
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
			System.exit(1);
		}

		println(GetColorsFromDatabase.class, ": Found " + count + " color names.");
		return retval;
	}
}
