package gov.fnal.ppd.dd.signage.attic;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 *
 */
public class MultiCastTest implements Runnable {

	MulticastSocket socket;

	// InetAddress group;

	static final int PORTNUMBER = 50090;

	static final int PACKETLENGTH = 4000;

	byte [] data = new byte [PACKETLENGTH];

	DatagramPacket packet = new DatagramPacket(data, data.length);

	static final String [] clockIP = { "", "239.128.1.4", "239.128.4.4", "239.128.4.5" };

	static int clockType = 1;

	MultiCastTest() {
		start();
		new Thread(this).start();
	}

	/**
	 * @param args
	 */
	public static void main( String [] args ) {
		new MultiCastTest();

	}

	/**
	 * Open the socket.
	 */
	public void start() {
		System.out.println("clockType = " + clockType);
		System.out.println(clockIP[clockType]);
		try {
			InetAddress group = InetAddress.getByName(clockIP[clockType]);
			if (socket != null)
				socket.close();
			socket = new MulticastSocket(null);
			socket.setReuseAddress(true);
			try {
				socket.bind(new InetSocketAddress(clockIP[clockType], PORTNUMBER));
			} catch (Exception ee) {
				socket = new MulticastSocket(null);
				socket.setReuseAddress(true);
				socket.bind(new InetSocketAddress("0.0.0.0", PORTNUMBER));
			}
			socket.joinGroup(group);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		long currentTime = 0;
		int goodCnt = 0;
		packet.setLength(PACKETLENGTH);

		while (true) {
			try {
				System.out.println("Waiting...");
				socket.receive(packet);
				System.out.println("Got something");
				long total = (System.currentTimeMillis() - currentTime);
				if (total > 67) {
					System.out.print("-" + total);
				} else {
					goodCnt++;
					if (goodCnt >= 0) {
						System.out.println("\n" + total);
						goodCnt = 0;
					}
				}
				// byte [] b = packet.getData();
				currentTime = System.currentTimeMillis();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
