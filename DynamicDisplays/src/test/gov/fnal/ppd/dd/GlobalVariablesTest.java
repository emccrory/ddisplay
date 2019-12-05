package test.gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_PASSWORD;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_USER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getLogger;
import static gov.fnal.ppd.dd.GlobalVariables.setLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import org.junit.Test;

import gov.fnal.ppd.dd.chat.LoggerForDebugging;

public class GlobalVariablesTest {

	// Not much here to test. Maybe someday...

	@Test
	public void testCredentialsSetup() {
		credentialsSetup();
		// If it does not crash, then everything should be OK

		assertNotNull(DATABASE_SERVER_NAME);
		assertNotNull(DATABASE_NAME);
		assertNotNull(DATABASE_USER_NAME);
		assertNotNull(DATABASE_PASSWORD);
	}

	@Test
	public void testGetLogger() {
		LoggerForDebugging logger = new LoggerForDebugging(GlobalVariablesTest.class.getName());
		setLogger(logger.getLogger());
		Logger lgr = getLogger();
		assertEquals(logger.getLogger(), lgr);
	}

}
