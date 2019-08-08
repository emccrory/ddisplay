/*
 * ObjectSigning
 * 
 * Taken from <a
 * href="http://examples.javacodegeeks.com/core-java/security/signing-a-java-object-example/">examples.javacodegeeks.com/</a>
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.checkSignedMessages;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A class to sign any seiralizable object
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ObjectSigning {

	private static final String					ALG_TYPE			= "DSA";

	private static ObjectSigning				me					= new ObjectSigning();

	private static Map<String, ObjectSigning>	keys				= new HashMap<String, ObjectSigning>();

	private static Map<String, List<String>>	clientControlList	= new HashMap<String, List<String>>();

	/**
	 * @return the instance of this ObjectSigning object for this JVM
	 */
	public static ObjectSigning getInstance() {
		return me;
	}

	private KeyPairGenerator		keyPairGenerator;
	private PrivateKey				privateKey			= null;
	private PublicKey				publicKey			= null;
	private Map<String, Boolean>	emergMessAllowed	= new HashMap<String, Boolean>();

	private KeyFactory				keyFactory;
	private Signature				signature			= null;

	private Signature				sig					= null;

	// All the public keys will be stored in the database and the private keys will be stored on the local
	// disk of the sender, but not in a place that can normally be read. For example, ~/keystore

	/**
	 * Initialize the object signing mechanism -- PRIVATE class. Use getInstance();
	 */
	private ObjectSigning() {
		try {
			keyFactory = KeyFactory.getInstance(ALG_TYPE);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param client
	 *            -- The name of the client for which you'll need to check the signature
	 * @return -- The object that knows about this client's public key
	 */
	public static ObjectSigning getPublicSigning(final String client) {
		if (keys.containsKey(client) && keys.get(client) != null)
			return keys.get(client);

		ObjectSigning thatObject = new ObjectSigning();
		if (thatObject.loadPublicKeyFromDB(client)) {
			keys.put(client, thatObject);
			clientControlList.put(client, loadDisplayListFromDB(client));
			return thatObject;
		}
		keys.remove(client);
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
		if (clientControlList.containsKey(client) && clientControlList.get(client) != null)
			clientControlList.remove(client);

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
		if (thisClientsDisplays.contains("-1") || thisClientsDisplays.contains(displayName)) {
			println(ObjectSigning.class, ": " + client + " *IS* authorized to send to display '" + displayName + "'");
			return true;
		}

		String theList = "";
		for (String S : thisClientsDisplays)
			theList += "\t\t" + S + "\n";
		println(ObjectSigning.class, ": Client '" + client + "' does not have target '" + displayName
				+ "' in the list.  Here is the list:\n" + theList + "\tForgetting this client ");
		dropClient(client);
		return false;
	}

	private void generateNewKeys() throws NoSuchAlgorithmException {
		// Generate a 1024-bit Digital Signature Algorithm (DSA) key pair.
		keyPairGenerator = KeyPairGenerator.getInstance(ALG_TYPE);

		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.genKeyPair();
		privateKey = keyPair.getPrivate();
		publicKey = keyPair.getPublic();
	}

	/**
	 * @param filenamePublic
	 *            -- The name of the public keystore file
	 * @param filenamePrivate
	 *            -- The name of the private keystore file
	 * @throws NoSuchAlgorithmException
	 *             -- A problem with the encryption service
	 * @throws IOException
	 *             -- A problem writing the public or private keystores.
	 */
	public void generateNewKeys(final String filenamePublic, final String filenamePrivate) throws NoSuchAlgorithmException,
			IOException {

		// Generate a 1024-bit Digital Signature Algorithm (DSA) key pair.
		keyPairGenerator = KeyPairGenerator.getInstance(ALG_TYPE);

		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.genKeyPair();
		privateKey = keyPair.getPrivate();
		publicKey = keyPair.getPublic();

		// Store Public Key.
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());

		try (FileOutputStream fos = new FileOutputStream(filenamePublic)) {
			fos.write(x509EncodedKeySpec.getEncoded());
		}
		// Store Private Key.
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());

		try (FileOutputStream fos = new FileOutputStream(filenamePrivate)) {
			fos.write(pkcs8EncodedKeySpec.getEncoded());
		}
	}

	/**
	 * @param clientName
	 *            -- the name of the client, in the database, that is associated with the public key we need to retrieve
	 */
	private boolean loadPublicKeyFromDB(final String clientName) {
		Connection connection;
		try {
			connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {
					String query = "SELECT PublicKey from PublicKeys WHERE ClientName= '" + clientName + "'";

					try (ResultSet rs = stmt.executeQuery(query);) {
						if (rs.first()) { // Move to first returned row (there should only be one)
							Blob pk = rs.getBlob("PublicKey");
							int len = (int) pk.length();
							byte[] bytes = pk.getBytes(1, len);
							publicKey = KeyFactory.getInstance(ALG_TYPE).generatePublic(new X509EncodedKeySpec(bytes));

							println(getClass(), ": Got the public key for client " + clientName);
							return true;
						}
						// Likely culprit here: The source of this message does not have a public key in the DB
						publicKey = null;
						System.err.println("No public key for client='" + clientName + "' -- it cannot have signed messages.");

					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		return false;
	}

	private static List<String> loadDisplayListFromDB(final String clientName) {
		List<String> retval = new ArrayList<String>();

		Connection connection;
		try {
			connection = ConnectionToDatabase.getDbConnection();
			int lc = 999;

			synchronized (connection) {
				String ipNameOfClient = clientName.substring(0, clientName.indexOf(' '));
				String instanceOfClient = clientName.substring(clientName.indexOf("selector ") + "selector ".length());
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {

					// Modified, Nov. 6, 2015, to allow for a controller to control more than one (but less that all of the)
					// display(s).

					// Using two queries ..
					//
					// String query1 = "SELECT LocationCode from SelectorLocation WHERE IPName like '" + ipNameOfClient
					// + "%' AND Instance='" + instanceOfClient + "'";
					// try (ResultSet rs1 = stmt.executeQuery(query1);) {
					// if (rs1.first()) { // Move to first returned row
					// do {
					// lc = rs1.getInt("LocationCode");
					//
					// if (lc < 0) {
					// retval = new ArrayList<String>();
					// retval.add("-1");
					// println(ObjectSigning.class, ": This client can control all the displays!");
					// return retval;
					// }
					//
					//
					// String query2 = "SELECT VirtualDisplayNumber,IPName,ScreenNumber FROM DisplaySort,Display WHERE "
					// + "DisplaySort.DisplayID=Display.DisplayID AND DisplaySort.LocationCode=" + lc;
					//
					// try (ResultSet rs2 = stmt.executeQuery(query2);) {
					// if (rs2.first()) { // Move to first returned row
					// do {
					// String ipName = rs2.getString("IPname");
					// int scr = rs2.getInt("ScreenNumber");
					// int vID = rs2.getInt("VirtualDisplayNumber");
					// retval.add(ipName + ":" + scr + " (" + vID + ")");
					// } while (rs2.next());
					//
					// }
					// }
					// } while (rs1.next());
					// } else {
					// System.err.println("No displays for IP='" + clientName + "' -- it cannot control any displays.\n\t\t"
					// + "Query1=[" + query1 + "]");
					// }
					// }

					// Using one query ...

					String query0 = "SELECT VirtualDisplayNumber,Display.IPName AS IPName,ScreenNumber,Display.DisplayID FROM "
							+ "DisplaySort,Display,SelectorLocation WHERE DisplaySort.DisplayID=Display.DisplayID AND "
							+ "DisplaySort.LocationCode=SelectorLocation.LocationCode AND SelectorLocation.IPName LIKE '"
							+ ipNameOfClient + "%' AND Instance='" + instanceOfClient + "'";

					try (ResultSet rs0 = stmt.executeQuery(query0);) {
						if (rs0.first()) { // Move to first returned row
							do {
								String ipName = rs0.getString("IPname");
								int scr = rs0.getInt("ScreenNumber");
								int vID = rs0.getInt("VirtualDisplayNumber");
								retval.add(ipName + ":" + scr + " (" + vID + ")");
							} while (rs0.next());
						} else {
							// Special case: Is the LocationCode equal to -1, indicating ALL displays are available?

							String query1 = "SELECT LocationCode from SelectorLocation WHERE IPName like '" + ipNameOfClient
									+ "%' AND Instance='" + instanceOfClient + "'";
							try (ResultSet rs1 = stmt.executeQuery(query1);) {
								if (rs1.first()) { // Move to first returned row
									do {
										lc = rs1.getInt("LocationCode");
										if (lc < 0) {
											retval = new ArrayList<String>();
											retval.add("-1");
											println(ObjectSigning.class, ": This client can control all the displays!");
											return retval;
										}
										System.err.println("\n\n**********\n\n" + ObjectSigning.class.getSimpleName()
												+ ": Unanticipated situation!  Got a location code of " + lc + " for client "
												+ ipNameOfClient
												+ " but really expecting either a -1 or no location code at all."
												+ "\n\n**********  Contact code author!");
										break; // Not sure if we can continue or not. We'll try.
									} while (rs1.next());
								}
							}
						}
					} catch (Exception e3) {
						e3.printStackTrace();
						System.err.println("Query0 = " + query0);
					}

				} catch (Exception e1) {
					e1.printStackTrace();
					System.err.println("Query was (likely): USE " + DATABASE_NAME);
				}
			}
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		println(ObjectSigning.class, ": This client has " + retval.size() + " displays it can change.");
		return retval;

	}

	@SuppressWarnings("unused")
	private static String getIPName(final String ipName) {
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

	/**
	 * Read the public key from the file system
	 * 
	 * @param filename
	 *            -- The file name containing the public keystore
	 * @throws IOException
	 *             -- A problem reading the keystore
	 */
	public void loadPublicKey(final String filename) throws IOException {
		File filePublicKey = new File(filename);
		try (FileInputStream fis = new FileInputStream(filename)) {
			byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
			fis.read(encodedPublicKey);
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
			try {
				publicKey = keyFactory.generatePublic(publicKeySpec);
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param encodedPublicKey
	 *            -- The encoded public key
	 */
	public void setPublicKey(final byte[] encodedPublicKey) {
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
		try {
			publicKey = keyFactory.generatePublic(publicKeySpec);
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param filename
	 * @throws IOException
	 *             -- A problem reading the keystore
	 * @throws InvalidKeySpecException
	 *             --
	 * @throws NoSuchAlgorithmException
	 *             -- A problem with the encryption service * @throws InvalidKeySpecException
	 */
	public void loadPrivateKey(final String filename) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
		if (privateKey != null)
			return;

		File filePrivateKey = new File(filename);
		try (FileInputStream fis = new FileInputStream(filename)) {
			byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
			fis.read(encodedPrivateKey);
			fis.close();

			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);

			privateKey = keyFactory.generatePrivate(privateKeySpec);
			signature = Signature.getInstance(privateKey.getAlgorithm());
		}
	}

	/**
	 * @param toSign
	 *            -- The object to sign. Must be Serialzable.
	 * @return -- The signed object
	 * @throws SignatureException
	 *             -- An invalid signature
	 * @throws IOException
	 *             -- A problem reading the keystore
	 * @throws NoSuchAlgorithmException
	 *             -- A problem with the encryption service * @throws InvalidKeySpecException
	 * @throws InvalidKeyException
	 *             -- The private key is not valid
	 */
	public SignedObject getSignedObject(final Serializable toSign) throws SignatureException, NoSuchAlgorithmException,
			InvalidKeyException, IOException {
		assert (privateKey != null);

		if (signature == null)
			signature = Signature.getInstance(privateKey.getAlgorithm());

		return new SignedObject(toSign, privateKey, signature);
	}

	/**
	 * @param clientName
	 *            the name of this client, according to the database
	 * @return Is this client allowed to be the source of an "emergency message"?
	 */
	public boolean isEmergMessAllowed(String clientName) {
		if (!emergMessAllowed.containsKey(clientName)) {
			Connection connection;
			boolean ema = false;
			try {
				connection = ConnectionToDatabase.getDbConnection();

				synchronized (connection) {
					try (Statement stmt = connection.createStatement();
							ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {
						String query = "SELECT EmergMessAllowed from PublicKeys WHERE ClientName= '" + clientName + "'";

						try (ResultSet rs = stmt.executeQuery(query);) {
							if (rs.first()) { // Move to first returned row (there should only be one)

								ema = rs.getInt("EmergMessAllowed") == 1;

								println(getClass(), ": Cient '" + clientName + "' " + (ema ? "IS" : "is NOT")
										+ " allowed to make emergency messages");
								emergMessAllowed.put(clientName, ema);
							} else {
								// Likely culprit here: The source of this message does not have a public key in the DB
								publicKey = null;
								System.err.println("No entry for client='" + clientName + "'");
							}
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return emergMessAllowed.get(clientName);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		ObjectSigning OS = new ObjectSigning();

		if (args.length == 0) {
			System.out
					.println("********************\n\n  Generating new public and private keys and signing an object, just as a test\n\n********************\n");
			try {
				OS.generateNewKeys();
				MessageCarrier mess1 = MessageCarrier.getMessage("Left", "Me", new Date().toString());
				MessageCarrier mess2 = MessageCarrier.getIAmAlive("Them", "Us", "Howdy!");
				SignedObject so1 = OS.example(mess1);
				SignedObject so2 = OS.example(mess2);

				System.out.println();
				System.out.println("Public key for this was\n" + dump(OS.publicKey.getEncoded()));
				System.out.println();
				System.out.println("Private key for this was\n" + dump(OS.privateKey.getEncoded()));
				System.out.println();
				System.out.println("Signature on the 1st signed object was:\n" + dump(so1.getSignature()));
				System.out.println();
				System.out.println("Signature on the 2nd signed object was:\n" + dump(so2.getSignature()));

				SignedObject so4 = OS.example(mess1);

				mess1.setMessageOriginator("Mf");
				SignedObject so3 = OS.example(mess1);
				System.out.println();
				System.out.println("Signature on the 3rd signed object was:\n" + dump(so3.getSignature()));
				System.out.println();
				System.out.println("Signature on the 4th signed object was:\n" + dump(so4.getSignature()));

				if (OS.verifySignature(so1) == null)
					System.out.println("Verified");
				else
					System.out.println("Not verified");

				Signature thesig = Signature.getInstance(OS.publicKey.getAlgorithm());
				try {
					boolean retval = so1.verify(OS.publicKey, thesig);
					if (retval)
						System.out.println("Secondary signature is verified");
					else
						System.out.println("Secondary signature is INCORRECT");

				} catch (InvalidKeyException e) {
					e.printStackTrace();
				} catch (SignatureException e) {
					e.printStackTrace();
				}

			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		} else if (args.length == 2) {
			String filenamePrivate = args[1];
			String filenamePublic = args[0];

			try {
				OS.generateNewKeys(filenamePublic, filenamePrivate);
				System.out.println("Successfully generated new keys.  Public key is in file '" + filenamePublic
						+ "'.  The public key probably belongs in the database.");
				System.out.println("The private key is in '" + filenamePrivate
						+ "'.  Be sure to move this private keystore to somewhere really, REALLY private!");
			} catch (NoSuchAlgorithmException | IOException e) {
				e.printStackTrace();
			}
		} else if (args.length == 3) {
			// This functionality has been moved to gov.fnal.ppd.security.GenerateNewKeyPair
		}
	}

	private static String dump(byte[] encoded) {
		String r = "";
		for (int i = 0; i < encoded.length; i++) {
			if ((0x000000ff & encoded[i]) < 16)
				r += "0";
			r += Integer.toHexString(0x000000ff & encoded[i]);
			if (encoded.length < 100)
				continue;
			if ((i % 4) == 3)
				r += ' ';
			if ((i % 48) == 47)
				r += '\n';
		}
		return r;
	}

	/**
	 * @param signedMess
	 *            -- The message to test
	 * @return -- null if the message is signed properly; a string explaining why if it is not.
	 */
	public String verifySignature(final SignedObject signedMess) {
		assert (signedMess != null);

		if (!checkSignedMessages()) {
			System.err.println(getClass().getSimpleName() + ".verifySignature(): Ignoring the signature and returning 'true'");
			return null;
		}

		if (publicKey == null)
			return "No public key!";

		try {
			// System.err.println(getClass().getSimpleName() + ".verifySignature(): really and truly checking the signature!");
			if (sig == null)
				sig = Signature.getInstance(publicKey.getAlgorithm());
			// The "FastBugs" error for this line is illogical.
			boolean retval = signedMess.verify(publicKey, sig);
			if (!retval) {
				for (String k : keys.keySet()) {
					if (keys.get(k) == this) { // Yes, I think "==" is right here: Are these the same objects?
						keys.remove(k);
						break;
					}
				}
				return "Signature is invalid";
			}
			return null; // The object is properly signed.
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Exeption caused the signature check to fail";
	}

	/**
	 * @param toSign
	 *            -- The object to sign
	 * @return -- The signed object that corresponds to the passed object
	 */
	public SignedObject example(final Serializable toSign) {
		try {
			// We can sign Serializable objects only
			signature = Signature.getInstance(privateKey.getAlgorithm());
			SignedObject signedMess = new SignedObject(toSign, privateKey, signature);

			// Verify the signed object
			Signature sig = Signature.getInstance(publicKey.getAlgorithm());
			boolean verifMes = signedMess.verify(publicKey, sig);

			// System.out.println("Is signed Object verified ? " + verified );
			System.out.println("Is signed Object verified ? " + verifMes);

			// Retrieve the object
			MessageCarrier unsignedMess = (MessageCarrier) signedMess.getObject();

			System.out.println("Original Message : " + unsignedMess);

			return signedMess;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected final synchronized void writePublicKeyToDatabase(String clientName) {
		String blob = "";
		byte[] encoded = publicKey.getEncoded();
		for (int i = 0; i < encoded.length; i++) {
			if ((0x000000ff & encoded[i]) < 16)
				blob += "0";
			blob += Integer.toHexString(0x000000ff & encoded[i]);

		}
		Connection connection;
		try {
			connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {
					String statementString = "INSERT INTO PublicKeys VALUES (NULL, '" + clientName + "', x'" + blob + "', '"
							+ InetAddress.getLocalHost().getHostAddress() + "');";

					int numRows = stmt.executeUpdate(statementString);
					if (numRows == 0 || numRows > 1) {
						System.err
								.println("Problem while updating status of Display: Expected to modify exactly one row, but  modified "
										+ numRows + " rows instead. SQL='" + statementString + "'");
					}
					stmt.close();
				} catch (SQLException ex) {
					System.err.println("cannot execute a query. Is the DB server down?  Try again later.");
					ex.printStackTrace();
					System.exit(-1);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		} catch (DatabaseNotVisibleException e) {
			// not good!
			System.err.println("No connection.  It is likely that the DB server is down.  Try again later.");

			e.printStackTrace();
			System.exit(-1);
		}
	}
}
