package test.gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.changer.ChannelCatalogFactory.getInstance;
import static gov.fnal.ppd.dd.changer.ChannelCatalogFactory.refresh;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ChannelCatalogFactoryTest {

	static {
		credentialsSetup();
	}
	
	@Test
	public void testGetInstance() {
		assertNotNull(getInstance());
	}

	@Test
	public void testRefresh() {
		assertNotNull(refresh());
	}

}
