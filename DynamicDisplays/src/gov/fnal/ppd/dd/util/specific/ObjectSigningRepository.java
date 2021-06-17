/*
 * ObjectSigningRepository
 * 
 * Elliott McCrory, Fermilab
 * 
 * Copyright (c) 2015 by Fermilab/FRA
 */
package gov.fnal.ppd.dd.util.specific;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * I am not sure about this class yet. It seems that the class ObjectSigning is doing two sublty different things that should
 * probably be separated: (1) It is an object that can be signed; (2) it checks the validity of an object's signature.
 * </p>
 * <p>
 * At this time I have these so intertwined that I cannot unmix them easily. (Or it may be that these two functions are in my
 * imagination.)
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ObjectSigningRepository {
	private static Map<String, ObjectSigning>	keys				= new HashMap<String, ObjectSigning>();
	private static Map<String, List<String>>	clientControlList	= new HashMap<String, List<String>>();

	private ObjectSigningRepository() {
	}

	/**
	 * The problem referenced in the comment for the class is shown here. How does one implement this method? Does one even NEED to
	 * implement this method? I'm lost.
	 * 
	 * @param client
	 *            -- The name of the client for which you'll need to check the signature
	 * @return -- The object that knows about this client's public key
	 */
	public static ObjectSigning getPublicSigning(final String client) {
		// if (keys.containsKey(client) && keys.get(client) != null)
		// return keys.get(client);
		//
		// ObjectSigning thatObject = new ObjectSigning();
		// if (thatObject.loadPublicKeyFromDB(client)) {
		// keys.put(client, thatObject);
		// clientControlList.put(client, loadDisplayListFromDB(client));
		// return thatObject;
		// }
		// keys.remove(client);
		return null;
	}

	/**
	 * Remove a client from the cache of public keys so it can reconnect with different credentials
	 * 
	 * @param client
	 *            The name of the client to remove
	 * @return if this client was actually in the list.
	 */
	public static boolean dropClient(final String client) {
		if (keys.containsKey(client) && keys.get(client) != null) {
			keys.remove(client);
			return true;
		}
		return false;
	}

	/**
	 * @param client
	 *            The client trying to make changes to the display
	 * @param displayName
	 *            The ID of the display (i.e., the display number)
	 * @return Is this client authorized to change the channel on this display?
	 */
	public static boolean isClientAuthorized(final String client, final String displayName) {
		println(ObjectSigning.class, ": Checking if " + client + " is authorized to send to display '" + displayName + "'");
		if (!clientControlList.containsKey(client)) {
			println(ObjectSigning.class, ": Client is not in the DB; we don't know what displays it can change!");
			return false;
		}
		List<String> thisClientsDisplays = clientControlList.get(client);
		// return thisClientsDisplays.contains("-1") || thisClientsDisplays.contains(displayName);
		if (thisClientsDisplays.contains("-1") || thisClientsDisplays.contains(displayName))
			return true;

		String theList = "";
		for (String S : thisClientsDisplays)
			theList += "\t\t" + S + "\n";
		println(ObjectSigning.class, ": I wonder why this client is not in the list.  Here is the list:\n" + theList);
		return false;
	}

	static List<String> loadDisplayListFromDB(final String clientName) {
		return null;
	}

	@SuppressWarnings("unused")
	private static String getIPName(String ipName) {
		try {
			InetAddress address = InetAddress.getByName(ipName);
			return address.getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		try {
			InetAddress address = InetAddress.getByName(ipName + ".dhcp.fnal.gov");
			return address.getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

}
