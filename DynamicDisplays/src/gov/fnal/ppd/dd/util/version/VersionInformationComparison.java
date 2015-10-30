package gov.fnal.ppd.dd.util.version;

import java.util.Date;

/**
 * Compare local versioning information to the info on the web site. Error exit iff the web version is newer than the local version.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class VersionInformationComparison {

	public static void main(final String[] args) {

		boolean debug = args.length > 0 && args[0].equalsIgnoreCase("DEBUG");

		VersionInformation viWeb = VersionInformation.getWebVersionInformation();
		VersionInformation viLocal = VersionInformation.getVersionInformation();

		if (debug) {
			System.out.println("WEB VERSION:\n" + viWeb);
			System.out.println("Time stamp: " + new Date(viWeb.getTimeStamp()));

			System.out.println();

			System.out.println("LOCAL VERSION:\n" + viLocal);
			System.out.println("Time stamp: " + new Date(viLocal.getTimeStamp()));
		}

		if (viWeb.getTimeStamp() > viLocal.getTimeStamp()) {
			long hours = (viWeb.getTimeStamp() - viLocal.getTimeStamp()) / 3600000L;
			if (debug)
				System.out.println("Web version is newer than the local version.  Difference is " + hours + " hours.");
			System.exit(-1);
		} else if (viWeb.getTimeStamp() == viLocal.getTimeStamp()) {
			if (debug)
				System.out.println("Time stamp of web version equals local time stamp.");
		} else {
			if (debug)
				System.out.println("Local time stamp is newer than the web version.");
		}
		System.exit(0);
	}
}
