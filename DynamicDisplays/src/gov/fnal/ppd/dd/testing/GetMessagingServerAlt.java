package gov.fnal.ppd.dd.testing;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class GetMessagingServerAlt {
	private GetMessagingServerAlt() {
	}

	/**
	 * @return The name of me
	 */
	public static String getName1() {
		try {
			InetAddress ip = InetAddress.getLocalHost();
			String myName = ip.getCanonicalHostName().replace(".dhcp", "");

			return myName;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	/**
	 * @return The name of me
	 */
	public static String getName2() {
		String retval = "";
		try {

			Enumeration<NetworkInterface> nInterfaces = NetworkInterface.getNetworkInterfaces();

			while (nInterfaces.hasMoreElements()) {
				Enumeration<InetAddress> inetAddresses = nInterfaces.nextElement().getInetAddresses();
				while (inetAddresses.hasMoreElements()) {
					retval += inetAddresses.nextElement().getHostName() + "\n";
				}
			}

		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return retval;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		System.out.println("Old way: " + getName1());
		System.out.println("New way: [[\n" + getName2() + "]]");
	}
}
