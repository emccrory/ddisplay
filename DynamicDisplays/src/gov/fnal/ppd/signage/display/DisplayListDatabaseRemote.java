package gov.fnal.ppd.signage.display;

import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageDatabaseNotVisibleException;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.signage.changer.DisplayList;

import java.awt.Color;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * The actual list of Displays in the system.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2013
 * 
 */
public class DisplayListDatabaseRemote extends ArrayList<Display> implements DisplayList {

	private static final long	serialVersionUID	= -1865804684093297761L;

	private static Connection	connection			= null;

	/**
	 * @return a connection to the Dynamic Displays database
	 */
	public static Connection getConnection() {
		if (connection == null)
			try {
				connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			} catch (SignageDatabaseNotVisibleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		try {
			Statement stmt = connection.createStatement();
			stmt.executeQuery("USE xoc");
			stmt.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		return connection;
	}

	/**
	 * Create a connection and the receive a list of Displays in the system.
	 */
	public DisplayListDatabaseRemote() {
		super();

		// DisplayAsJxBrowser.webTesting = true;
		getDisplays();
	}

	private void getDisplays() {
		Statement stmt = null;
		ResultSet rs = null;

		/*
		 * Display DB table: | Location | char(255) | NO | | | | | Content | int(11) | YES | | NULL | | | Type |
		 * enum('Public','Experiment','XOC') | NO | | Public | | | DisplayID | int(11) | NO | PRI | NULL | auto_increment | | IPName
		 * | char(100) | YES | | NULL | | | ScreenNumber
		 */

		int count = 0;
		try {
			stmt = getConnection().createStatement();
			rs = stmt.executeQuery("SELECT * FROM Display");
			rs.first(); // Move to first returned row
			while (!rs.isAfterLast()) {
				String location = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Location"));
				String ipName = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("IPname"));
				int portNumber = rs.getInt("Port");
				int displayID = rs.getInt("DisplayID");
				int screenNumber = rs.getInt("ScreenNumber");
				int colorCode = Integer.parseInt(rs.getString("ColorCode"), 16);
				SignageType type = SignageType.valueOf(ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Type")));

				Display p = new DisplayFacade(portNumber, ipName, screenNumber, displayID, location, new Color(colorCode), type);

				add(p);
				rs.next();
				count++;
			}
			stmt.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.out.println(getClass().getSimpleName() + ": Found " + count + " displays.");
	}

	@Override
	public SignageType getCategory(int dn) {
		return get(dn).getCategory();
	}

	@Override
	public SignageType[] getCategories() {
		// TODO Auto-generated method stub
		return null;
	}
}
