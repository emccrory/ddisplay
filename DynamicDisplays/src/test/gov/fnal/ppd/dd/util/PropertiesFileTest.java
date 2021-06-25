package test.gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.util.nonguiUtils.PropertiesFile.getIntProperty;
import static gov.fnal.ppd.dd.util.nonguiUtils.PropertiesFile.getProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Tests of the class PropertiesFile
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class PropertiesFileTest {

	@Test
	public void testHasPortPorperty() {
		assertNotEquals(getIntProperty("messagingPort", 0), 0);
	}

	@Test
	public void testHasProtocolPorperty() {
		assertEquals(getProperty("defaultWebProtocol", "undefined"), "https");
	}

	@Test
	public void testDoesNotHaveUndefined() {
		assertEquals(getProperty("NotAProperty", "default"), "default");
	}
}
