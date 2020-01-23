package test.gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;

import gov.fnal.ppd.dd.CredentialsNotFoundException;

public class NeedsCredentials {

	protected static boolean credentialsOK = false;
	static {
		try {
			credentialsSetup();
			credentialsOK = true;
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
		}
	}
}
