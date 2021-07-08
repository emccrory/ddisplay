package test.gov.fnal.ppd.dd;

/**
 * Several tests in this part of the suite need to make a connection to the database. They should all extend this class so that the
 * static block, here, is executed. And then the classes that need the connection should check the value of credentialsOK. This code
 * is simple enough, but why copy/paste when inheritance works better?
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class NeedsCredentials {

	protected static boolean credentialsOK = false;
	static {
		try {
			System.out.println("Hi there");
			gov.fnal.ppd.dd.GlobalVariables.credentialsSetup();
			credentialsOK = true;
		} catch (gov.fnal.ppd.dd.util.nonguiUtils.CredentialsNotFoundException e) {
			e.printStackTrace();
		}
	}
}
