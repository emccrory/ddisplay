package gov.fnal.ppd.dd.util.attic;


/**
 * A class used during testing that creates a mock-up display on the same PC as the channel selector.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2013
 * @deprecated
 */
public class DisplayListDatabase { // extends ArrayList<Display> implements DisplayList {
//
//	private static final long	serialVersionUID	= -1865804684093297761L;
//
//	private Connection			connection;
//
//	DisplayListDatabase() {
//		super();
//		try {
//			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
//		} catch (SignageDatabaseNotVisibleException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		DisplayAsJxBrowser.webTesting = true;
//		getDisplays();
//	}
//
//	private void getDisplays() {
//		Statement stmt = null;
//		ResultSet rs = null;
//
//		try {
//			stmt = connection.createStatement();
//			rs = stmt.executeQuery("USE xoc");
//		} catch (SQLException ex) {
//			ex.printStackTrace();
//			System.exit(1);
//		}
//
//		/*
//		 * Display DB table: | Location | char(255) | NO | | | | | Content | int(11) | YES | | NULL | | | Type |
//		 * enum('Public','Experiment','XOC') | NO | | Public | | | DisplayID | int(11) | NO | PRI | NULL | auto_increment | | IPName
//		 * | char(100) | YES | | NULL | | | ScreenNumber
//		 */
//
//		int count = 0;
//		try {
//			rs = stmt.executeQuery("SELECT * FROM Display");
//			rs.first(); // Move to first returned row
//			while (!rs.isAfterLast()) {
//				String location = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Location"));
//				SignageType type = SignageType.valueOf(ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Type")));
//				String ipName = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("IPname"));
//				int displayID = rs.getInt("DisplayID");
//				int screenNumber = rs.getInt("ScreenNumber");
//				int colorCode = Integer.parseInt(rs.getString("ColorCode"), 16);
//				// TODO Need to make a DaemonProxy for the channel changer, which communicates with
//				// the actual Display thru the internet.
//				// Display p = new DisplayImpl(Color.white, type);
//				// Display p = new DisplayAsJFrame(Color.white, type);
//				Display p = new DisplayAsJxBrowser(ipName, displayID, screenNumber, location, new Color(colorCode), type);
//				// try {
//				// p.setIpName(ipName);
//				// } catch (UnknownHostException e) {
//				// e.printStackTrace();
//				// }
//				// p.setNumber(displayID);
//				// p.setScreenNumber(screenNumber);
//				// p.setLocation(location);
//				add(p);
//				rs.next();
//				count++;
//			}
//			stmt.close();
//			rs.close();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		//
//		// HSBColor.setNumColors(size());
//		// // Set the color for each Display
//		// for (int i = 0; i < size(); i++) {
//		// get(i).setPreferredHighlightColor(new HSBColor(i));
//		// }
//		System.out.println(getClass().getSimpleName() + ": Found " + count + " displays.");
//	}
//
//	@Override
//	public SignageType getCategory(int dn) {
//		return get(dn).getCategory();
//	}
//
//	@Override
//	public SignageType[] getCategories() {
//		// TODO Auto-generated method stub
//		return null;
//	}
}
