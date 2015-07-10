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
			// getCategoriesCoded();
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

				System.out.println(q);
				rs = stmt.executeQuery(q);
				if (rs.first()) // Move to first returned row
					while (!rs.isAfterLast())
						try {

							// | TabName | char(64)
							// | LocationCode | int(11)
							// | LocalID | int(11)
							// | Type | enum('Public','Experiment','XOC')
							// | Abbreviation | char(15)

							String cat = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("TabName"));
							String abb = ConnectionToDynamicDisplaysDatabase.makeString(rs.getAsciiStream("Abbreviation"));
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

	// @SuppressWarnings("unused")
	// private static void getCategoriesCoded() {
	// switch (locationCode) {
	// case 0: // ROC-West
	//
	// categories = new ChannelCategory[] { ChannelCategory.PUBLIC, ChannelCategory.PUBLIC_DETAILS,
	// ChannelCategory.NOVA_DETAILS, ChannelCategory.NUMI_DETAILS,
	// new ChannelCategory("MINERVA_DETAILS", "MINER\u03BDA"), ChannelCategory.ACCELERATOR, ChannelCategory.VIDEOS,
	// ChannelCategory.MISCELLANEOUS };
	//
	// if (IS_PUBLIC_CONTROLLER) {
	// categories = new ChannelCategory[] { ChannelCategory.PUBLIC, ChannelCategory.PUBLIC_DETAILS,
	// ChannelCategory.VIDEOS, ChannelCategory.MISCELLANEOUS };
	// }
	// break;
	//
	// case 1:// ROC-East
	// categories = new ChannelCategory[] { new ChannelCategory("CMS"), new ChannelCategory("LHC"),
	// ChannelCategory.MISCELLANEOUS };
	// break;
	//
	// case 2:
	// // Test regime in Elliott's office
	// case 3:
	// // WH2E -- CMS Remote Operations Center
	// default:
	// categories = new ChannelCategory[] { ChannelCategory.PUBLIC, ChannelCategory.PUBLIC_DETAILS,
	// ChannelCategory.NOVA_DETAILS, ChannelCategory.NUMI_DETAILS, ChannelCategory.EXPERIMENT_DETAILS,
	// ChannelCategory.ACCELERATOR, ChannelCategory.VIDEOS, ChannelCategory.MISCELLANEOUS, new ChannelCategory("CMS"),
	// new ChannelCategory("MINERVA_DETAILS", "MINER\u03BDA"), new ChannelCategory("LHC"),
	// new ChannelCategory("FERMILAB") };
	// }
	//
	// }

	private static final String[]	FermilabExperiments	= { "gMinus2", "g-2", "g - 2", "DUNE-LBNF", "MicroBooNE", "MiniBooNE",
			"MINERvA", "MINOS", "Mu2E", "NOvA", "SeaQuest", "NuMI", "LArIAT", "Fermilab", "Accelerator", "UUP", "MICE", "CHIPS",
			"DES"										};

	private static final String[]	CERNExperiments		= { "CMS", "ATLAS", "LHC", "AEGIS", "ALICE", "ALPHA", "AMS", "ASACUSA",
			"ATRAP", "AWAKE", "BASE", "CAST", "CLOUD", "CMS", "ACE", "AEGIS", "ALICE", "ALPHA", "AMS", "ASACUSA", "ATRAP", "AWAKE",
			"BASE", "CAST", "CLOUD", "COMPASS", "DIRAC", "ISOLDE", "LHCb", "LHCf", "MOEDAL", "NA61/SHINE", "NA62", "nTOF", "OSQAR",
			"TOTEM", "UA9", "Fermilab"					};

	/**
	 * (For future expansion) Check if an experiment name is an experiment that is "relevant" at the specified location
	 * 
	 * @param exp
	 *            The name of the experiment to check
	 * @return Is this experiment relevant to the GUI at the designated locationCode?
	 */
	public static boolean isExperiment(final String exp) {
		// TODO -- This needs to be constructed from the database, not hard-coded as it is here.
		switch (getLocationCode()) {
		case 0:
			// ROC-West
			for (String EXP : FermilabExperiments)
				if (exp.equalsIgnoreCase(EXP))
					return true;
			break;

		case 1:
			// ROC-East
			for (String EXP : CERNExperiments)
				if (exp.equalsIgnoreCase(EXP))
					return true;
			break;

		case 2:
			// Test regime in Elliott's office
		case 3:
			// WH2E
		case 4:
			// AD cross gallery computer room
		case 5:
			// FESS
		case 6:
			// ESH&Q, WH7W
		case 7:
			// Neutrino Division, WH12W
		default:
			// Everything else
			return true;
		}

		return false;
	}
}
