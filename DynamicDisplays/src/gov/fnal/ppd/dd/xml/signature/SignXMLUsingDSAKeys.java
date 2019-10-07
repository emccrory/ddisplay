package gov.fnal.ppd.dd.xml.signature;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.PRIVATE_KEY_LOCATION;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getFullSelectorName;
import static gov.fnal.ppd.dd.GlobalVariables.prepareSaverImages;
import static gov.fnal.ppd.dd.GlobalVariables.prepareUpdateWatcher;
import static gov.fnal.ppd.dd.util.Util.println;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.xml.XMLDocumentAndString;

public class SignXMLUsingDSAKeys {

	private static final String			ALG_TYPE	= "DSA";
	private static KeyFactory			keyFactory;
	private static PrivateKey			privateKey	= null;
	private static PublicKey			publicKey	= null;
	private static XMLSignatureFactory	fac;
	private static SignedInfo			si;
	private static KeyInfo				ki;

	static {
		try {
			keyFactory = KeyFactory.getInstance(ALG_TYPE);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		//
		// try {
		// setupDSA(PRIVATE_KEY_LOCATION, getFullSelectorName());
		// } catch (Exception e1) {
		// e1.printStackTrace();
		// }
	}

	/**
	 * @param filename
	 *            - the local location of the private key for signing the document
	 * @throws IOException
	 *             -- A problem reading the private key file
	 * @throws InvalidKeySpecException
	 *             -- A problem with the key
	 * @throws NoSuchAlgorithmException
	 *             -- A problem with the encryption service
	 */
	public static boolean loadPrivateKey(final String filename)
			throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
		if (privateKey != null)
			return false;

		File filePrivateKey = new File(filename);
		try (FileInputStream fis = new FileInputStream(filename)) {
			byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
			fis.read(encodedPrivateKey);
			fis.close();

			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);

			privateKey = keyFactory.generatePrivate(privateKeySpec);
		} catch (Exception e) {
			System.out.println("Problem reading " + filename + " -- ");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Read the public key from the file system
	 * 
	 * @param filename
	 *            -- The file name containing the public keystore
	 * @throws IOException
	 *             -- A problem reading the keystore
	 */
	public static boolean loadPublicKey(final String filename) throws IOException {
		File filePublicKey = new File(filename);
		try (FileInputStream fis = new FileInputStream(filename)) {
			byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
			fis.read(encodedPublicKey);
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
			try {
				publicKey = keyFactory.generatePublic(publicKeySpec);
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/**
	 * Read the public key from the Dynamic Displays' Database
	 * 
	 * @param clientName
	 *            - the name of the client who should have sent the message
	 * @return - if this retrieval was successful
	 * @throws InvalidKeySpecException
	 *             - Invalid key was fetched
	 */
	public static boolean loadPublicKeyFromDB(final String clientName) throws InvalidKeySpecException {
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

							println(SignXMLUsingDSAKeys.class, ": Got the public key for client " + clientName);
							return true;
						}
						// If here, the likely culprit here: The source of this message does not have a public key in the DB
						publicKey = null;
						throw new InvalidKeySpecException(
								"No public key for client='" + clientName + "' -- it cannot have signed messages.");
					} catch (NoSuchAlgorithmException e) {
						System.err
								.println("Internal configuration problem; things probably aren't going to work - contact author.");
						e.printStackTrace();
					}
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		} catch (DatabaseNotVisibleException e2) {
			e2.printStackTrace();
		}
		return false;
	}

	/**
	 * Setup the signature infrastructure
	 * 
	 * @param privateKeyFileName
	 *            - the filename of the private key
	 * @param nodeName
	 *            - the name of the client that owns this private key (that is, the name of this node in the Dynamic Display system)
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeySpecException
	 * @throws IOException
	 * @throws KeyException
	 */
	public static void setupDSA(String privateKeyFileName, String nodeName)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, KeyException, NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, InvalidKeySpecException, IOException {

		if (!loadPrivateKey(privateKeyFileName) || !loadPublicKeyFromDB(nodeName)) {
			throw new KeyException("Keys not properly located/configured");
		}
		doStuff();
	}

	public static void doStuff() throws InstantiationException, IllegalAccessException, ClassNotFoundException, KeyException,
			NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");

		fac = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).newInstance());

		KeyInfoFactory kif = fac.getKeyInfoFactory();
		KeyValue kv = kif.newKeyValue(publicKey);

		Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null),
				Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);

		si = fac.newSignedInfo(
				fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null),
				fac.newSignatureMethod(SignatureMethod.DSA_SHA1, null), Collections.singletonList(ref));

		ki = kif.newKeyInfo(Collections.singletonList(kv));
	}

	/**
	 * Sign an existing XML Document that is in a file - this is likely only used for testing purposes.
	 * 
	 * @param documentFileName
	 *            - the name of the file that is the XML document
	 * @param out
	 *            - the output, signed XML document. Default: System.out
	 * @throws FileNotFoundException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws MarshalException
	 * @throws XMLSignatureException
	 * @throws TransformerException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private static void signDocument(String documentFileName, OutputStream out)
			throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, MarshalException,
			XMLSignatureException, TransformerException, InstantiationException, IllegalAccessException, ClassNotFoundException,
			KeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {

		if (out == null)
			out = System.out;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().parse(new FileInputStream(documentFileName));
		signDocument(doc, out);
	}

	/**
	 * Sign an existing XML Document.
	 * 
	 * @param doc
	 *            - the XML document
	 * @param out
	 *            - the output, signed XML document. Default: System.out
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws MarshalException
	 * @throws XMLSignatureException
	 * @throws TransformerException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static void signDocument(Document doc, OutputStream out)
			throws MarshalException, XMLSignatureException, TransformerException, InstantiationException, IllegalAccessException,
			ClassNotFoundException, KeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		if (out == null)
			out = System.out;

		if (privateKey == null) {
			try {
				setupDSA(PRIVATE_KEY_LOCATION, getFullSelectorName());
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchAlgorithmException
					| InvalidAlgorithmParameterException | InvalidKeySpecException | KeyException | IOException e) {
				e.printStackTrace();
			}
		}
		doStuff();
		DOMSignContext dsc = new DOMSignContext(privateKey, doc.getDocumentElement());

		XMLSignature signature = fac.newXMLSignature(si, ki);
		signature.sign(dsc);

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = tf.newTransformer();
		trans.transform(new DOMSource(doc), new StreamResult(out));
	}

	public static void main(String[] args) {

		if (args[0].equalsIgnoreCase("validate")) {
			try {
				XMLDocumentAndString xmlDoc = new XMLDocumentAndString(new String(Files.readAllBytes(Paths.get(args[1]))));
				if (Validate.isSignatureValid(xmlDoc))
					System.out.println(args[1] + " is a validly signed XML document!");
				else
					System.err.println("XML file '" + args[1] + "' is NOT properly signed!");

			} catch (MarshalException | XMLSignatureException | IOException | SignatureNotFoundException e) {
				e.printStackTrace();
			}
			System.exit(0);
		}

		prepareUpdateWatcher(false);
		prepareSaverImages();
		credentialsSetup();

		if (args.length != 2 || !args[0].endsWith(".key") || !args[1].endsWith(".xml")) {
			System.err.println("Usage: <Private Key> <XML File to sign>");
			System.exit(-1);
		}

		try {
			setupDSA(args[0], "ad130482 selector 00");
			signDocument(args[1], System.out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
