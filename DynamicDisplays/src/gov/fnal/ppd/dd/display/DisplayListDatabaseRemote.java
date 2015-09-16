package gov.fnal.ppd.dd.display;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.awt.Color;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
				e.printStackTrace();
				return connection = null;
			}
		try (Statement stmt = connection.createStatement();) {
			stmt.executeQuery("USE " + DATABASE_NAME);
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
	 * 
	 * @param locationCode
	 *            Which Dynamic Display system are we dealing with?
	 */
	public DisplayListDatabaseRemote(int locationCode) {
		super();
		this.locationCode = locationCode;
		getDisplays();
	}

	/**
	 * Create a connection and the receive one display, if it is controllable.
	 * 
	 * @param locationCode
	 * @param displayDBNum
	 */
	public DisplayListDatabaseRemote(final int locationCode, final int displayDBNum) {
		super();
		this.locationCode = locationCode;
		getADisplay(displayDBNum);
	}

	private void getDisplays() {
		int count = 0;
		// Use ARM to simplify this try block
		List<Integer> dID = new ArrayList<Integer>();
		try (Statement stmt = getConnection().createStatement();
				ResultSet rs = stmt.executeQuery("SELECT DisplaySort.LocationCode as LocationCode,Display.LocationCode as TickerCode" +
						"Display.DisplayID as DisplayID,VirtualDisplayNumber,ScreenNumber,Location,IPName,ColorCode,Type " +
						"FROM Display LEFT JOIN DisplaySort ON (Display.DisplayID=DisplaySort.DisplayID) ORDER BY VirtualDisplayNumber;");) {
			rs.first(); // Move to first returned row
			while (!rs.isAfterLast())
				try {
					String location = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Location"));
					String ipName = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("IPname"));
					int locCode = rs.getInt("LocationCode");
					int displayID = rs.getInt("DisplayID");
					int vDisplayNum = rs.getInt("VirtualDisplayNumber");
					int screenNumber = rs.getInt("ScreenNumber");
					int colorCode = Integer.parseInt(rs.getString("ColorCode"), 16);
					if ((locationCode < 0 || locCode == locationCode) && !dID.contains(displayID)) { 
						// Negative locationCode will select ALL displays everywhere
						SignageType type = SignageType.valueOf(ConnectionToDynamicDisplaysDatabase.makeString(rs
								.getAsciiStream("Type")));

						dID.add(displayID);
						Display p = new DisplayFacade(locCode, ipName, vDisplayNum, displayID, screenNumber, location, new Color(
								colorCode), type);
						p.setVirtualDisplayNumber(vDisplayNum);

						add(p);
						count++;
					}
					rs.next();
				} catch (Exception e) {
					e.printStackTrace();
				}
			stmt.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.out.println(getClass().getSimpleName() + ": Found " + count + " displays at locationCode=" + locationCode + ".");
	}

	private void getADisplay(int displayDBNumber) {
		int count = 0;
		// Use ARM to simplify this try block
		try (Statement stmt = getConnection().createStatement();
				ResultSet rs = stmt.executeQuery(
						"SELECT Location,IPName,Display.DisplayID as DisplayID,VirtualDisplayNumber,DisplaySort.LocationCode as LocationCode," +
						"ScreenNumber,ColorCode,Type FROM Display LEFT JOIN DisplaySort ON (Display.DisplayID=DisplaySort.DisplayID) " +
						"ORDER BY VirtualDisplayNumber");) {
			rs.first(); // Move to first returned row
			while (!rs.isAfterLast())
				try {
					String location = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Location"));
					String ipName = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("IPName"));
					int locCode = rs.getInt("LocationCode");
					int displayID = rs.getInt("DisplayID");
					int vDisplayNum = rs.getInt("VirtualDisplayNumber");
					int screenNumber = rs.getInt("ScreenNumber");
					int colorCode = Integer.parseInt(rs.getString("ColorCode"), 16);
					if ((locationCode < 0 || locCode == locationCode) && displayDBNumber == displayID) {
						SignageType type = SignageType.valueOf(ConnectionToDynamicDisplaysDatabase.makeString(rs
								.getAsciiStream("Type")));

						Display p = new DisplayFacade(locCode, ipName, vDisplayNum, displayID, screenNumber, location, new Color(
								colorCode), type);
						p.setVirtualDisplayNumber(vDisplayNum);

						add(p);
						count++;
					}
					rs.next();
				} catch (Exception e) {
					e.printStackTrace();
				}
			stmt.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.out.println(getClass().getSimpleName() + ": Found " + count + " displays at locationCode=" + locationCode
				+ " and displayDBNumber=" + displayDBNumber);
	}

}
