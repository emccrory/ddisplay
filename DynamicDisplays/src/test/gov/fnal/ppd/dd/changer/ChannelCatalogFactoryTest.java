package test.gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.changer.ChannelCatalogFactory.getInstance;
import static gov.fnal.ppd.dd.changer.ChannelCatalogFactory.refresh;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import test.gov.fnal.ppd.dd.NeedsCredentials;
/**
 * Tests of the class ChannelCatalogFactory
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class ChannelCatalogFactoryTest extends NeedsCredentials {

	@Test
	public void testAnything() {
		assertTrue(true);
	}
	
	@Test
	public void testGetInstance() {
		assumeTrue(credentialsOK);
		assertNotNull(getInstance());
	}

	@Test
	public void testRefresh() {
		assumeTrue(credentialsOK);
		assertNotNull(refresh());
	}

}
