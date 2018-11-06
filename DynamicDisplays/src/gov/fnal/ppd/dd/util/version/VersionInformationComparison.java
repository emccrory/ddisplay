package gov.fnal.ppd.dd.util.version;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;

import java.util.Date;

/**
 * Compare local versioning information to the info on the web site. Error exit iff the web version is newer than the local version.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class VersionInformationComparison {

	// Ten seconds should do it. We are forced to at least a one second lee-way as the Database cannot store milliseconds.
	private static final long	MAXIMUM_ALLOWABLE_TIME_DIFFERENCE	= 10000;

	/**
	 * @param flavor
	 *            Which sort of release to lookup
	 * @param debug
	 *            Print descriptive stuff along the way
	 * 
	 * @return true if the latest FLAVOR of updates is measurably newer than the local version
	 */
	public static boolean lookup(final FLAVOR flavor, final boolean debug) {
		VersionInformation viWeb = VersionInformation.getDBVersionInformation(flavor);
		if (viWeb == null) {
			// No versions are available!
			if (debug)
				System.out.println("No version of flavor " + flavor + " is available from the database.");
			return false;
		}
		VersionInformation viLocal = VersionInformation.getVersionInformation();

		if (debug) {
			System.out.println("Database VERSION:\n" + viWeb);
			System.out.println("Time stamp: " + new Date(viWeb.getTimeStamp()));

			System.out.println();

			System.out.println("LOCAL VERSION:\n" + viLocal);
			System.out.println("Time stamp: " + new Date(viLocal.getTimeStamp()));

			System.out.println();
		}

		long diff = Math.abs(viWeb.getTimeStamp() - viLocal.getTimeStamp());

		if (diff < MAXIMUM_ALLOWABLE_TIME_DIFFERENCE) {
			if (debug)
				System.out.println("Time stamp of database version is the same as the local time stamp.  Delta=" + diff
						+ " milliseconds");
			return false;
		} else if (viWeb.getTimeStamp() > viLocal.getTimeStamp()) {
			double days = ((double) diff / 3600000.0) / 24.0;
			if (debug)
				System.out.println("Local version is older than the Database version.  Difference is " + days + " days - you should update!");
			return true;
		} else {
			double days = ((double) diff / 3600000.0) / 24.0;
			if (debug)
				System.out.println("Local time stamp is newer than the database version by " + days + " days - no update needed");
			return false;
		}
	}
}
