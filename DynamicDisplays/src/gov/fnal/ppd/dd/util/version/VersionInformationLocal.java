package gov.fnal.ppd.dd.util.version;

/**
 * Simply return the version number of the local software.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class VersionInformationLocal  {
	
	private VersionInformationLocal() {
		
	}
	@SuppressWarnings("javadoc")
	public static void main(final String[] args) {
		System.out.println("" + VersionInformation.getVersionInformation().getVersionString());
	}

}
