package test.gov.fnal.ppd.dd;

import static org.junit.Assert.*;
import static gov.fnal.ppd.dd.GetMessagingServer.*;
import static gov.fnal.ppd.dd.GlobalVariables.*;

import org.junit.Test;

public class GetMessagingServerTest {
	static {
		credentialsSetup();
	}

	@Test
	public void testGetMessagingServerNameDisplay() {
		// This is not necessarily satisfiable since not every node is a Display node
		assertNotNull(getMessagingServerNameDisplay());
	}

	@Test
	public void testGetMessagingServerNameSelector() {
		// This is not necessarily satisfiable since not every node is a Selector node
		assertNotNull(getMessagingServerNameSelector());
	}

	@Test
	public void testGetDocentNameSelector() {
		// This is not necessarily satisfiable since not every node is a Selector node
		getDocentNameSelector();
		assertNotNull(docentName);
	}

}
