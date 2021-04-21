package test.gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.changer.ChannelCatalogFactory.getInstance;
import static gov.fnal.ppd.dd.changer.ChannelCatalogFactory.refresh;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import gov.fnal.ppd.dd.CredentialsNotFoundException;
import test.gov.fnal.ppd.dd.NeedsCredentials;

public class ChannelCatalogFactoryTest extends NeedsCredentials {

	@Test
	public void testGetInstance() {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("A");
		assumeTrue(credentialsOK);
		assertNotNull(getInstance());
	}

	@Test
	public void testRefresh() {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("C");
		assumeTrue(credentialsOK);
		System.out.println("D");
		assertNotNull(refresh());
		System.out.println("E");
	}

}
