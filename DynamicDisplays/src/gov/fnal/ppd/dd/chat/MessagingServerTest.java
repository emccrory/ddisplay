/*
 * MessagingServerTest
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;

/**
 * Test if the messaging server is running somewhere
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class MessagingServerTest {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		int portNumber = MESSAGING_SERVER_PORT;
		String userName = "TestForMessagingServer";

		MessagingClient client = new MessagingClient(System.getProperty("ddisplay.messagingserver", "mccrory.fnal.gov"),
				portNumber, userName);

		if (!client.start())
			System.exit(-1); // No messaging server is present

		System.exit(0); // Messaging server *is* present!
	}
}
