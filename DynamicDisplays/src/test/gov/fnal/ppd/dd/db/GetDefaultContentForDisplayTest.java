package test.gov.fnal.ppd.dd.db;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static org.junit.Assert.*;

import org.junit.Test;

import gov.fnal.ppd.dd.CredentialsNotFoundException;
import gov.fnal.ppd.dd.db.GetDefaultContentForDisplay;

public class GetDefaultContentForDisplayTest {
	private GetDefaultContentForDisplay defaultContent = new GetDefaultContentForDisplay();

	static {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGetChannelNumber() {
		int d = defaultContent.getChannelNumber();
		assertNotEquals(d, 0);
	}

	@Test
	public void testGetDisplayType() {
		String t = defaultContent.getDisplayType();
		assertNotNull(t);
		assertTrue(t.equals("Regular/Selenium") || t.equals("FirefoxOnly"));
	}

	@Test
	public void testGetDefaultURL() {
		String u = defaultContent.getDefaultURL();
		assertNotNull(u);
	}

}
