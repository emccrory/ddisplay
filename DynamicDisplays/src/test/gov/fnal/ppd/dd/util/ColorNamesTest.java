package test.gov.fnal.ppd.dd.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import gov.fnal.ppd.dd.util.ColorNames;
import test.gov.fnal.ppd.dd.NeedsCredentials;

public class ColorNamesTest extends NeedsCredentials {
	

	private static String[] expectedColorCodes = //
			{ //
					"000000", // Black
					"004397", // FermiBlue
					"008000", // Green
					"f20019", // Red
					"ffcc00", // Yellow
					"ffffff", // White
			};

	public ColorNamesTest() {
	}

	@Test
	public void testIsEmpty() {
		if (!credentialsOK) return;
		ColorNames colorTest = new ColorNames();
		assertFalse(colorTest.isEmpty());
	}

	@Test
	public void testContainsKey() {
		if (!credentialsOK) return;
		ColorNames colorTest = new ColorNames();
		for (String colorCode : expectedColorCodes)
			assertNotNull(colorTest.get(colorCode));
	}

}
