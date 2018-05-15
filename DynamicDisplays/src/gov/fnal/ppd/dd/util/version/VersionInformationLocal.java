package gov.fnal.ppd.dd.util.version;

public class VersionInformationLocal  {
	
	private VersionInformationLocal() {
		
	}
	public static void main(String[] args) {
		System.out.println("" + VersionInformation.getVersionInformation().getVersionString());
	}

}
