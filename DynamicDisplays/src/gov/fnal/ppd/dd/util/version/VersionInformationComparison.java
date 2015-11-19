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

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		credentialsSetup();

		int index = 0;
		boolean debug = false;
		FLAVOR flavor = FLAVOR.PRODUCTION;

		while (args.length > index) {
			if (args[index].equalsIgnoreCase("DEBUG"))
				debug = true;
			else
				try {
					flavor = FLAVOR.valueOf(args[index]);
				} catch (Exception e) {
					flavor = FLAVOR.DEVELOPMENT;
				}
			index++;
		}

		VersionInformation viWeb = VersionInformation.getDBVersionInformation(flavor);
		if (viWeb == null) {
			// No versions are available!
			if (debug)
				System.out.println("No version is available from the database (null).");

			System.exit(0);
		}
		VersionInformation viLocal = VersionInformation.getVersionInformation();

		if (debug) {
			System.out.println("Database VERSION:\n" + viWeb);
			System.out.println("Time stamp: " + new Date(viWeb.getTimeStamp()));

			System.out.println();

			System.out.println("LOCAL VERSION:\n" + viLocal);
			System.out.println("Time stamp: " + new Date(viLocal.getTimeStamp()));
		}

		if (viWeb.getTimeStamp() > viLocal.getTimeStamp()) {
			long hours = (viWeb.getTimeStamp() - viLocal.getTimeStamp()) / 3600000L;
			if (debug)
				System.out.println("Database version is newer than the local version.  Difference is " + hours + " hours.");
			System.exit(-1);
		} else if (viWeb.getTimeStamp() == viLocal.getTimeStamp()) {
			if (debug)
				System.out.println("Time stamp of database version equals local time stamp.");
		} else {
			if (debug)
				System.out.println("Local time stamp is newer than the database version.");
		}
		System.exit(0);
	}
}
