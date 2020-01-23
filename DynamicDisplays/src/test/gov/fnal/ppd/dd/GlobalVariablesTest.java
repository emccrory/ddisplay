package test.gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.getDefaultDwellTime;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.GlobalVariables.getLogger;
import static gov.fnal.ppd.dd.GlobalVariables.getSoftwareVersion;
import static gov.fnal.ppd.dd.GlobalVariables.setLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import org.junit.Test;

import com.google.common.base.CharMatcher;

import gov.fnal.ppd.dd.chat.LoggerForDebugging;

public class GlobalVariablesTest {

	// Not much here to test. Maybe someday...

	@Test
	public void testGetSoftwareVersion() {
		String val = getSoftwareVersion();

		assertNotNull(val);

		int count = CharMatcher.is('.').countIn(val);
		assertEquals(count, 2);
	}

	@Test
	public void testGetFullURLPrefix() {
		String val = getFullURLPrefix();

		assert (val.startsWith("https://"));
	}

	// This test does not work (it fails silently) within the context of Jenkins - that environment does not know the credentials.
//	@Test
//	public void testCredentialsSetup() {
//		credentialsSetup();
//		// If it does not crash, then everything should be OK
//
//		assertNotNull(DATABASE_SERVER_NAME);
//		assertNotNull(DATABASE_NAME);
//		assertNotNull(DATABASE_USER_NAME);
//		assertNotNull(DATABASE_PASSWORD);
//	}

	@Test
	public void testGetLogger() {
		LoggerForDebugging logger = new LoggerForDebugging(GlobalVariablesTest.class.getName());
		setLogger(logger.getLogger());
		Logger lgr = getLogger();
		assertEquals(logger.getLogger(), lgr);
	}

	@Test
	public void testGetDefaultDwellTime() {
		long first = getDefaultDwellTime();
		long second = getDefaultDwellTime();

		assert (first > 0);
		assert (second > 0);
		assert (Math.abs(first - second) < 10000L);
	}
}
