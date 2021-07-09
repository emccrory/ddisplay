package test.gov.fnal.ppd.dd.db;

import static gov.fnal.ppd.dd.db.ConnectionToDatabase.getDbConnection;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;

import org.junit.Test;

import test.gov.fnal.ppd.dd.NeedsCredentials;

public class ConnectionToDatabaseTest extends NeedsCredentials {

	@Test
	public void testGetDbConnection() {
		assumeTrue(NeedsCredentials.credentialsOK);
		try {
			Connection c = getDbConnection();
			assertTrue(c.isValid(1));
			c.close();
		} catch (Exception e) {
			fail("Exception caught: " + e.getClass().getSimpleName() + " -- " + e.getLocalizedMessage());
		}
	}
}
