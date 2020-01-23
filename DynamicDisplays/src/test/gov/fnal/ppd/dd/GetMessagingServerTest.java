package test.gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GetMessagingServer.getDocentNameSelector;
import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameDisplay;
import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameSelector;
import static gov.fnal.ppd.dd.GlobalVariables.docentName;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class GetMessagingServerTest extends NeedsCredentials {

	@Test
	public void testGetMessagingServerNameDisplay() {
		if (!credentialsOK)
			return;
		// This is not necessarily satisfiable since not every node is a Display node
		assertNotNull(getMessagingServerNameDisplay());
	}

	@Test
	public void testGetMessagingServerNameSelector() {
		if (!credentialsOK)
			return;
		// This is not necessarily satisfiable since not every node is a Selector node
		assertNotNull(getMessagingServerNameSelector());
	}

	@Test
	public void testGetDocentNameSelector() {
		if (!credentialsOK)
			return;
		// This is not necessarily satisfiable since not every node is a Selector node
		getDocentNameSelector();
		assertNotNull(docentName);
	}

}
