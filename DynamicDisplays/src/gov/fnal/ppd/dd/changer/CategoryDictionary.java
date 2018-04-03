/*
 * CategoryDictionary
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getNumberOfLocations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Retrieve the categories that are to be used by the ChannelSelector GUI
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class CategoryDictionary {
	private static ChannelCategory[]	categories	= null;

	private CategoryDictionary() {
	}

	/**
	 * @return The array of Channel categories that are appropriate for this instance of the ChannelSelector GUI
	 */
	public static ChannelCategory[] getCategories() {
		if (categories == null) {
			getCategoriesDatabase();
		}
		return categories;

	}

	private static void getCategoriesDatabase() {
		List<ChannelCategory> cats = new ArrayList<ChannelCategory>();

		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();

			stmt = connection.createStatement();
			rs = stmt.executeQuery("USE " + DATABASE_NAME);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}

		synchronized (connection) {
			String q = "SELECT DISTINCT TabName,Abbreviation FROM LocationTab";
			try {
				// TODO Multiple location code adaptation would be here!
				if (getNumberOfLocations() == 1) {
					if (IS_PUBLIC_CONTROLLER) {
						if (getLocationCode() < 0)
							q += " WHERE Type='Public'";
						else
							q += " WHERE LocationCode=" + getLocationCode() + " AND Type='Public'";
					} else {
						if (getLocationCode() >= 0)
							q += " WHERE LocationCode=" + getLocationCode();
					}
				} else {
					String extra = " WHERE (";
					for (int i = 0; i < getNumberOfLocations(); i++) {
						int lc = getLocationCode(i);
						if (i > 0)
							extra += " OR ";
						extra += " LocationCode=" + lc;
					}
					if (IS_PUBLIC_CONTROLLER)
						extra += ") AND Type='Public'";
					else
						extra += ")";
					if (!extra.equals(" WHERE ()"))
						q += extra;
				}

				q += " ORDER BY Abbreviation";
				System.out.println(q);
				rs = stmt.executeQuery(q);
				if (rs.first()) // Move to first returned row
					while (!rs.isAfterLast())
						try {

							// | TabName .... | char(64)
							// | LocationCode | int(11)
							// | LocalID .... | int(11)
							// | Type ....... | enum('Public','Experiment','XOC')
							// | Abbreviation | char(15)

							String cat = decode(rs.getString("TabName"));
							String abb = decode(rs.getString("Abbreviation"));
							cats.add(new ChannelCategory(cat, abb));

							rs.next();
						} catch (Exception e) {
							e.printStackTrace();
						}
				else {
					System.err.println("No definition of what tabs to show for locationCode=" + getLocationCode()
							+ " and Controller Type=" + (IS_PUBLIC_CONTROLLER ? "Public" : "XOC"));
					System.exit(-1);
				}
				stmt.close();
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
				System.err.println("Query was '" + q + "'");
			}
		}
		categories = cats.toArray(new ChannelCategory[0]);
	}

	private static final String decode(final String in) {
		String working = in;
		int index;
		index = working.indexOf("\\u");
		while (index > -1) {
			int length = working.length();
			if (index > (length - 6))
				break;
			int numStart = index + 2;
			int numFinish = numStart + 4;
			String substring = working.substring(numStart, numFinish);
			int number = Integer.parseInt(substring, 16);
			String stringStart = working.substring(0, index);
			String stringEnd = working.substring(numFinish);
			working = stringStart + ((char) number) + stringEnd;
			index = working.indexOf("\\u");
		}
		return working;
	}

	
}
