package gov.fnal.ppd.dd.testing;

import static gov.fnal.ppd.dd.util.Util.catchSleep;

import gov.fnal.ppd.dd.chat.MessagingClient;

/**
 * This class will try to confuse the messaging server by connecting to it and then sending it garbage
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class BreakMessagingServer extends MessagingClient {

	public BreakMessagingServer(String server, int port) {
		super(server, port, "IAmGoingToBreakYou");

	}

	public static void main(String[] args) {
		BreakMessagingServer client = new BreakMessagingServer("mccrory.fnal.gov", 49999);
		if (client.start()) {
			client.sendBadMessage();
		}
	}

	private void sendBadMessage() {

		try {
			sOutput.writeChars("Hey buddy.  Take this message");
			// The following line is controversial: Is it a bug or is it a feature? Sun claims it is a feature -- it allows the
			// sender to re-send any message easily. But others (myself included) see this as a bug -- if you remember every message
			// every time, you'll run out of memory eventually. The call to "reset()" causes the output object to ignore this,
			// and all previous, messages it has sent.
			sOutput.reset();
			return;
		} catch (Exception e) {
			displayLogMessage(MessagingClient.class.getSimpleName() + ".sendMessage(): Exception writing to server");
			catchSleep(1);
			e.printStackTrace();
		}

	}
}
