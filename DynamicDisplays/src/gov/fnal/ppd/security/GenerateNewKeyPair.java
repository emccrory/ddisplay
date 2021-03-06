/*
 * GenerateNewKeyPair
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.security;

import java.io.Console;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;

/**
 * <p>
 * A class to generate a new public/private key pair
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class GenerateNewKeyPair {

	private static final String			ALG_TYPE				= "DSA";
	private volatile static Connection	connection;
	private static final String			DATABASE_NAME			= System.getProperty("ddisplay.dbname", "xoc_dev");
	private static final String			DATABASE_SERVER_NAME	= System.getProperty("ddisplay.dbserver");
	private static String				serverNode				= DATABASE_SERVER_NAME;
	private static String				thisNode;

	/**
	 * @param user
	 * @param passwd
	 * @return the DB connection object
	 * 
	 * @throws DatabaseNotVisibleException
	 *             if it cannot connect to the database server
	 */
	public static Connection getDbConnection(String user, char[] passwd) throws DatabaseNotVisibleException {
		if (connection != null) {
			return connection;
		}

		InetAddress ip = null;
		try {
			ip = InetAddress.getLocalHost();
			thisNode = ip.getCanonicalHostName();
		} catch (UnknownHostException e) {
			System.err.println("Cannot get my own IP name.  IP Address is " + ip);
			e.printStackTrace();
			System.exit(-1);
		}

		if (serverNode.equals(thisNode))
			serverNode = "localhost";

		try {
			// The newInstance() call is a work around for some broken Java implementations
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("It seems that we can't load the jdbc driver: " + ex);
			System.exit(1);
		}
		try {
			String pw = "";
			for (char C : passwd)
				pw += C;
			connection = DriverManager.getConnection("jdbc:mysql://" + serverNode + "/" + DATABASE_NAME, user, pw);
			return connection;

		} catch (SQLException ex) {
			ex.printStackTrace();
			println(GenerateNewKeyPair.class, " -- SQLException: '" + ex.getMessage() + "'");
			println(GenerateNewKeyPair.class, " -- SQLState:      " + ex.getSQLState());
			println(GenerateNewKeyPair.class, " -- VendorError:   " + ex.getErrorCode());
			if (ex.getMessage().contains("Access denied for user")) {
				System.err.println("Cannot access the Channel/Display database. DB Host=jdbc:mysql://" + serverNode + "/"
						+ DATABASE_NAME + ", user=" + user + ", password=" + Arrays.toString(passwd));
				throw new DatabaseNotVisibleException(ex.getMessage());
			}
			System.err.println("Aborting");
			System.exit(1);
		}
		return null;
	}

	/**
	 * @param clazz
	 *            The type of the caller
	 * @param message
	 *            the message to print
	 */
	private static void println(Class<?> clazz, String message) {
		System.out.println(new Date() + " -- " + clazz.getSimpleName() + ": " + message);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String hostname = "localhost";
		// Get the local hostname
		try {
			hostname = InetAddress.getLocalHost().getCanonicalHostName();
			hostname = hostname.substring(0, hostname.indexOf('.'));
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
		
		GenerateNewKeyPair OS = new GenerateNewKeyPair();

		if (args.length == 2) {
			String publicKeyFilename = "Public.key";
			String privateKeyFilename = "Private.key";
			String selectorClientName = args[0];
			String databaseUserName = args[1];

			try {
				// Prompt the user for the password and store it in a char[]
				Console console = System.console();
				console.printf("Please enter the DB password for user " + databaseUserName + ": ");
				char[] password = console.readPassword();

				// Open the DB connection first - this is what often fails, so get it out of the way before making the keys
				OS.getConnection(selectorClientName, databaseUserName, password);
				for (int i = 0; i < password.length; i++)
					password[i] = 0;

				// Now make the keys
				OS.generateNewKeys(publicKeyFilename, privateKeyFilename);
				System.out.println("Successfully generated new keys.  Public key is in file '" + publicKeyFilename + "'.");

				// Write the public key into the database
				OS.writePublicKeyToDatabase(selectorClientName);

				// All done! This is the only place where the exit code is zero.
				System.out
						.println("Public key has been inserted into the database under client name '" + selectorClientName + "'.");
				System.out.println("The private key is in '" + privateKeyFilename
						+ "'.  Be sure to move this private keystore to somewhere really, REALLY private!");
				System.exit(0);
			} catch (NoSuchAlgorithmException | IOException e) {
				System.err.println("Something went wrong.  Got an exception of type " + e.getClass().getCanonicalName()
						+ " with the message: " + e.getMessage());
			}
		} else {			
			System.err.println("You gave " + args.length + " arguments and expected 2.\nUSAGE: java "
					+ GenerateNewKeyPair.class.getCanonicalName() + " <Client name> <database user name>\nFor example:\n\tjava "
					+ GenerateNewKeyPair.class.getCanonicalName() + " \"" + hostname + " selector 00\" <redacted>");
		}
		System.exit(-1);
	}

	private KeyPairGenerator	keyPairGenerator;
	private PrivateKey			privateKey;
	private PublicKey			publicKey;

	/**
	 * Initialize the object signing mechanism
	 */
	private GenerateNewKeyPair() {

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

	protected final synchronized void getConnection(String clientName, String user, char[] password) {
		try {
			connection = getDbConnection(user, password);
		} catch (DatabaseNotVisibleException e) {
			// not good!
			System.err.println("No connection.  Either your username/password is wrong or the DB server is down.");

			e.printStackTrace();
			System.exit(-1);
		}
	}

	protected final synchronized void writePublicKeyToDatabase(String clientName) {
		String blob = "";
		byte[] encoded = publicKey.getEncoded();
		for (int i = 0; i < encoded.length; i++) {
			if ((0x000000ff & encoded[i]) < 16)
				blob += "0";
			blob += Integer.toHexString(0x000000ff & encoded[i]);

		}

		try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {
			String statementString = "INSERT INTO PublicKeys VALUES (NULL, '" + clientName + "', x'" + blob + "', 0);";

			System.out.println(statementString);
			int numRows = stmt.executeUpdate(statementString);
			if (numRows == 0 || numRows > 1) {
				System.err.println("Problem while updating status of Display: Expected to modify exactly one row, but  modified "
						+ numRows + " rows instead. SQL='" + statementString + "'");
			}
			stmt.close();
		} catch (SQLException ex) {
			System.err.println("cannot execute a query. Is the DB server down?  Try again later.");
			ex.printStackTrace();
			System.exit(-1);
		}

	}

}
