package gov.fnal.ppd.signage.display;

import static gov.fnal.ppd.GlobalVariables.DATABASE_NAME;
import gov.fnal.ppd.signage.DatabaseNotVisibleException;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.ConnectionToDynamicDisplaysDatabase;

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
public class DisplayListDatabaseRemote extends ArrayList<Display> {

	private static final long	serialVersionUID	= -1865804684093297761L;

	private static Connection	connection			= null;

	/**
	 * @return a connection to the Dynamic Displays database
	 */
	public static Connection getConnection() {
		if (connection == null)
			try {
				connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		try (Statement stmt = connection.createStatement();) {
			@SuppressWarnings("unused")
			ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);
			stmt.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		return connection;
	}

	private int	locationCode;

	/**
	 * Create a connection and the receive a list of Displays in the system.
	 */
	public DisplayListDatabaseRemote(int locationCode) {
		super();
		this.locationCode = locationCode;
		// DisplayAsJxBrowser.webTesting = true;
		getDisplays();
	}

	private void getDisplays() {

		/*
		 * Display DB table: <pre> | Location | char(255) | NO | | | | | Content | int(11) | YES | | NULL | | | Type |
		 * enum('Public','Experiment','XOC') | NO | | Public | | | DisplayID | int(11) | NO | PRI | NULL | auto_increment | | IPName
		 * | char(100) | YES | | NULL | | | ScreenNumber
		 */

		int count = 0;
		// Use ARM to simplify this try block
		try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM Display");) {
			rs.first(); // Move to first returned row
			while (!rs.isAfterLast()) {
				String location = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Location"));
				String ipName = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("IPname"));
				int portNumber = rs.getInt("Port");
				int locCode = portNumber; // Repurposing "Port" column as a location code!!
				int displayID = rs.getInt("DisplayID");
				int screenNumber = rs.getInt("ScreenNumber");
				int colorCode = Integer.parseInt(rs.getString("ColorCode"), 16);
				if (locationCode < 0 || locCode == locationCode) { // Negative locationCode will select ALL displays everywhere
					SignageType type = SignageType
							.valueOf(ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Type")));

					Display p = new DisplayFacade(portNumber, ipName, screenNumber, displayID, location, new Color(colorCode), type);

					add(p);
					count++;
				}
				rs.next();
			}
			stmt.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.out.println(getClass().getSimpleName() + ": Found " + count + " displays at locationCode=" + locationCode + ".");
	}

}
