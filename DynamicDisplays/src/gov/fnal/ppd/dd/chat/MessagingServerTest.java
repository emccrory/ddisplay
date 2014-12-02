package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;

/**
 * Test if the messaging server is running somewhere
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class MessagingServerTest {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		int portNumber = MESSAGING_SERVER_PORT;
		String userName = "TestForMessagingServer";

		MessagingClient client = new MessagingClient(MESSAGING_SERVER_NAME, portNumber, userName);

		if (!client.start())
			System.exit(-1); // No messaging server is present
		
		System.exit(0); // Messaging server *is* present!
	}
}
