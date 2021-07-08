package gov.fnal.ppd.security;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** 
 * This class is used during the key-pair generation to get the hostname, as determined by the JVM.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class CheckHostName {

	public static void main(String[] args) {
		String hostname = "localhost";
		// Get the local hostname
		try {
			String h = InetAddress.getLocalHost().getCanonicalHostName();
			hostname = h.substring(0, h.indexOf('.'));
			System.out.println("The host name, according to the JVM, is '" + hostname + "' (derived from '" + h + "')");
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}		
	}
}
