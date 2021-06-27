package test.gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_PASSWORD;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_USER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.getDefaultDwellTime;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.GlobalVariables.getLogger;
import static gov.fnal.ppd.dd.GlobalVariables.getSoftwareVersion;
import static gov.fnal.ppd.dd.GlobalVariables.setLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.util.logging.Logger;

import org.junit.Test;

import com.google.common.base.CharMatcher;

import gov.fnal.ppd.dd.chat.LoggerForDebugging;

/**
 * Check the validity and integrity of several of the global variables and constants used throughout the suite.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class GlobalVariablesTest extends NeedsCredentials {

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

	@Test
	public void testCredentialsSetup() {
		assumeTrue(credentialsOK);

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

	@Test
	public void testGetDefaultDwellTime() {
		long first = getDefaultDwellTime();
		long second = getDefaultDwellTime();

		assert (first > 0);
		assert (second > 0);
		assert (Math.abs(first - second) < 10000L);
	}
}
