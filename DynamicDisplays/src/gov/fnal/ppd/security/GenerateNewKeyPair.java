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

/**
 * <p>
 * A class to generate a new public/private key pair
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class GenerateNewKeyPair {

	private static final String	ALG_TYPE				= "DSA";
	private static Connection	connection;
	private static final String	DEFAULT_SERVER			= "mccrory.fnal.gov";
	private static final String	DATABASE_NAME			= System.getProperty("ddisplay.dbname", "xoc");
	private static final String	DATABASE_SERVER_NAME	= System.getProperty("ddisplay.dbserver", DEFAULT_SERVER);
	private static String		serverNode				= DATABASE_SERVER_NAME;
	private static String		thisNode;

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
			for (char C: passwd)
				pw += C;
			connection = DriverManager.getConnection("jdbc:mysql://" + serverNode + "/xoc", user, pw);
			return connection;

		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			ex.printStackTrace();
			if (ex.getMessage().contains("Access denied for user")) {
				System.err.println("Cannont access the Channel/Display database. DB Host=jdbc:mysql://" + serverNode + "/xoc");
				throw new DatabaseNotVisibleException(ex.getMessage());
			} else {
				System.err.println("Aborting");
				System.exit(1);
			}
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		GenerateNewKeyPair OS = new GenerateNewKeyPair();

		if (args.length == 4) {
			String filenamePublic = args[0];
			String filenamePrivate = args[1];
			String clientName = args[2];
			String userName = args[3];

			try {
				OS.generateNewKeys(filenamePublic, filenamePrivate);
				System.out.println("Successfully generated new keys.  Public key is in file '" + filenamePublic + "'.");
				System.out.println("The private key is in '" + filenamePrivate
						+ "'.  Be sure to move this private keystore to somewhere really, REALLY private!");

				// Prompt the user for the password and store it in a char[]
				Console console = System.console();
				console.printf("Please enter the DB password for user " + userName + ": ");
				char[] password = console.readPassword();

				
				OS.writePublicKeyToDatabase(clientName, userName, password);
				for (int i = 0; i < password.length; i++)
					password[i] = 0;


				System.out.println("Public key has been inserted into the database under client name '" + clientName + "'.");
			} catch (NoSuchAlgorithmException | IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("USAGE: java " + GenerateNewKeyPair.class.getCanonicalName()
					+ " <PublicKey File Name> <PrivateKey file name> <Client name> <database user name> <database password>");
		}
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

		FileOutputStream fos = new FileOutputStream(filenamePublic);
		fos.write(x509EncodedKeySpec.getEncoded());
		fos.close();

		// Store Private Key.
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());

		fos = new FileOutputStream(filenamePrivate);
		fos.write(pkcs8EncodedKeySpec.getEncoded());
		fos.close();
	}

	protected final synchronized void writePublicKeyToDatabase(String clientName, String user, char[] password) {
		String blob = "";
		byte[] encoded = publicKey.getEncoded();
		for (int i = 0; i < encoded.length; i++) {
			if ((0x000000ff & encoded[i]) < 16)
				blob += "0";
			blob += Integer.toHexString(0x000000ff & encoded[i]);

		}
		Connection connection;
		try {
			connection = getDbConnection(user, password);

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
		} catch (DatabaseNotVisibleException e) {
			// not good!
			System.err.println("No connection.  It is likely that the DB server is down.  Try again later.");

			e.printStackTrace();
			System.exit(-1);
		}
	}

}
