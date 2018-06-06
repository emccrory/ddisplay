package gov.fnal.ppd.dd.testing;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * This class will try to confuse the messaging server by connecting to it and then sending it garbage
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class BreakMessagingServer {

	private ObjectInputStream		sInput;		// to read from the socket
	protected ObjectOutputStream	sOutput;	// to write on the socket
	private Socket					socket;

	public BreakMessagingServer(String server, int port) {
		try {
			socket = new Socket(server, port);
			sOutput = new ObjectOutputStream(socket.getOutputStream());
			sInput = new ObjectInputStream(socket.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String server = "mccrory.fnal.gov";
		int port = 49999;

		BreakMessagingServer client = new BreakMessagingServer(server, port);
		client.close();
		// if (client.start()) {
		// client.sendBadMessage();
		// }
	}

	private void close() {
		// TODO Auto-generated method stub

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
			println(getClass(), " sendBadMessage(): Exception writing to server");
			catchSleep(1);
			e.printStackTrace();
		}

	}
}
