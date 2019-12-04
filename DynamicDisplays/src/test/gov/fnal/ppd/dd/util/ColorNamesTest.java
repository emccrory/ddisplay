package test.gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import gov.fnal.ppd.dd.util.ColorNames;

public class ColorNamesTest {

	private static String[] expectedColorCodes = //
			{ //
					"000000", // Black
					"004397", // FermiBlue
					"008000", // Green
					"f20019", // Red
					"ffcc00", // Yellow
					"ffffff", // White
			};

	static {
		credentialsSetup();
	}

	public ColorNamesTest() {
	}

	@Test
	public void testIsEmpty() {
		ColorNames colorTest = new ColorNames();
		assertFalse(colorTest.isEmpty());
	}

	@Test
	public void testContainsKey() {
		ColorNames colorTest = new ColorNames();
		for (String colorCode : expectedColorCodes)
			assertNotNull(colorTest.get(colorCode));
	}

}
