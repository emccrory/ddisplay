/*
 * ObjectSigning
 * 
 * Taken from <a
 * href="http://examples.javacodegeeks.com/core-java/security/signing-a-java-object-example/">examples.javacodegeeks.com/</a>
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.specific;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.interfaces.DatabaseNotVisibleException;

/**
 * <p>
 * A class to sign a serializable object and take care of the reading of the public and the private keys for a client. Previously
 * read public keys are cached here, also.
 * </p>
 * <p>
 * This is a Singleton
 * </p>
 * <p>
 * Note that several of the methods here have been removed as of June 2021. See GIT history if you are interested.
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ObjectSigning {

	private static final String					ALG_TYPE			= "DSA";

	private static ObjectSigning				me					= new ObjectSigning();

	// private static Map<String, ObjectSigning> keys = new HashMap<String, ObjectSigning>();

	private static Map<String, List<String>>	clientControlList	= new HashMap<String, List<String>>();

	/**
	 * @return the (Singleton) instance of this ObjectSigning object for this JVM
	 */
	public static ObjectSigning getInstance() {
		return me;
	}

	private KeyPairGenerator		keyPairGenerator;
	private PrivateKey				privateKey			= null;
	private PublicKey				publicKey			= null;
	private Map<String, Boolean>	emergMessAllowed	= new HashMap<String, Boolean>();

	private KeyFactory				keyFactory;

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
	 */
	public static void saveClient(final String client) {
		if (clientControlList.containsKey(client) && clientControlList.get(client) != null)
			return;

		clientControlList.put(client, loadDisplayListFromDB(client));

	}

	/**
	 * Remove a client from the cache of public keys so it can reconnect with different credentials
	 * 
	 * @param client
	 *            The name of the client to remove
	 * @return if this client was actually in the list.
	 */
	public static boolean dropClient(final String client) {
		if (clientControlList.containsKey(client) && clientControlList.get(client) != null) {
			clientControlList.remove(client);
			return true;
		}
		return false;
	}

	/**
	 * Figure out if this client is authorized to do a non-read-only message.
	 * 
	 * @param client
	 *            The client trying to make changes to the display
	 * @param displayName
	 *            The ID of the display (i.e., the display number)
	 * @return Is this client authorized to change the channel on this display?
	 */
	public static boolean isClientAuthorized(final String client, final String displayName) {
		println(ObjectSigning.class, ": Checking if " + client + " is authorized to send to display '" + displayName + "'");
		if (!clientControlList.containsKey(client)) {
			saveClient(client);
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
	public void generateNewKeys(final String filenamePublic, final String filenamePrivate)
			throws NoSuchAlgorithmException, IOException {

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

							String query1 = "SELECT LocationCode FROM SelectorLocation WHERE IPName like '" + ipNameOfClient
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
												+ ipNameOfClient + " but really expecting either a -1 or no location code at all."
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
	 *             -- A problem with the encryption service
	 * @throws InvalidKeySpecException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public boolean loadPrivateKey(final String filename) {
		if (privateKey != null)
			return true;

		File filePrivateKey = new File(filename);
		try (FileInputStream fis = new FileInputStream(filename)) {
			byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
			fis.read(encodedPrivateKey);
			fis.close();

			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);

			privateKey = keyFactory.generatePrivate(privateKeySpec);
			@SuppressWarnings("unused")
			Signature signature = Signature.getInstance(privateKey.getAlgorithm());
			return true;
		} catch (Exception e) {
			return false;
		}
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
						String query = "SELECT EmergMessAllowed FROM PublicKeys WHERE ClientName= '" + clientName + "'";

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
					String statementString = "INSERT INTO PublicKeys VALUES (NULL, '" + clientName + "', x'" + blob + "', 0);";

					int numRows = stmt.executeUpdate(statementString);
					if (numRows == 0 || numRows > 1) {
						System.err.println(
								"Problem while updating status of Display: Expected to modify exactly one row, but  modified "
										+ numRows + " rows instead. SQL='" + statementString + "'");
					}
					stmt.close();
				} catch (SQLException ex) {
					System.err.println("cannot execute a query. Is the DB server down?  Try again later.");
					ex.printStackTrace();
					System.exit(-1);
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
