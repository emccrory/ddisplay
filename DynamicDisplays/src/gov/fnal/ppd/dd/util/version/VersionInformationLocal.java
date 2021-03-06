package gov.fnal.ppd.dd.util.version;

/**
 * Return the version number of the local software.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class VersionInformationLocal {

	private VersionInformationLocal() {
	}

	@SuppressWarnings("javadoc")
	public static void main(final String[] args) {
		System.out.println("" + VersionInformation.getVersionInformation().getVersionString());
		// System.out.println("" + new Date(VersionInformation.getVersionInformation().getTimeStamp()));
	}
}
