package test.gov.fnal.ppd.dd;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import gov.fnal.ppd.dd.GlobalVariables;

/**
 * The ideas for the tests here are not implemented - a general testing agent won't be in the database as a display node or as a
 * selector node. This is necessary to perform these tests. But, the development node *is* all of these things, so it works there.
 * And if Jenkins is run on the same node, then those tests will also work.
 * 
 * So, beware.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class GetMessagingServerTest extends NeedsCredentials {

	@Test
	public void testGetMessagingServerNameDisplay() {
		assumeTrue(credentialsOK);

		// This is not necessarily satisfiable since not every node is a Display node
		assertNotNull(gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameDisplay());
	}

	@Test
	public void testGetMessagingServerNameSelector() {
		assumeTrue(credentialsOK);
		// This is not necessarily satisfiable since not every node is a Selector node
		assertNotNull(gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameSelector());
	}

	@Test
	public void testGetDocentNameSelector() {
		assumeTrue(credentialsOK);
		// This is not necessarily satisfiable since not every node is a Selector node
		gov.fnal.ppd.dd.GetMessagingServer.getDocentNameSelector();
		assertNotNull(GlobalVariables.docentName);
	}

}
