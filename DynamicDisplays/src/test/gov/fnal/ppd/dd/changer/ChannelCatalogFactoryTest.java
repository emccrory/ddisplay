package test.gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.changer.ChannelCatalogFactory.getInstance;
import static gov.fnal.ppd.dd.changer.ChannelCatalogFactory.refresh;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import test.gov.fnal.ppd.dd.NeedsCredentials;

public class ChannelCatalogFactoryTest extends NeedsCredentials {

	@Test
	public void testGetInstance() {
		if (!credentialsOK)
			return;
		assertNotNull(getInstance());
	}

	@Test
	public void testRefresh() {
		if (!credentialsOK)
			return;
		assertNotNull(refresh());
	}

}
