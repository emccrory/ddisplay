package gov.fnal.ppd.dd.testing;

import gov.fnal.ppd.dd.chat.MessageCarrier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
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

/**
 * <p>
 * A class to sign any seiralizable object
 * </p>
 * <p>
 * Taken from <a
 * href="http://examples.javacodegeeks.com/core-java/security/signing-a-java-object-example/">examples.javacodegeeks.com/</a>
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class ObjectSigning {

	private static ObjectSigning	me	= new ObjectSigning();

	/**
	 * @return the instance of this ObjectSigning object for this JVM
	 */
	public ObjectSigning getInstance() {
		return me;
	}

	private KeyPairGenerator	keyPairGenerator;
	private PrivateKey			privateKey;
	private PublicKey			publicKey;

	private KeyFactory			keyFactory;
	private Signature			signature	= null;

	// I'm thinking that all the public keys will be stored in the database and the private keys will be stored on the local
	// disk of the sender, but not in a place that can normally be read. For example, ~/.keystore

	/**
	 * Initialize the object signing mechanism
	 */
	private ObjectSigning() {
		try {
			keyFactory = KeyFactory.getInstance("DSA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	private void generateNewKeys() throws NoSuchAlgorithmException {
		// Generate a 1024-bit Digital Signature Algorithm (DSA) key pair.
		keyPairGenerator = KeyPairGenerator.getInstance("DSA");

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
		keyPairGenerator = KeyPairGenerator.getInstance("DSA");

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

	/**
	 * Read the public key from the file system
	 * 
	 * @param filename
	 *            -- The file name containing the public keystore
	 * @throws IOException
	 *             -- A problem reading the keystore
	 * @throws NoSuchAlgorithmException
	 *             -- A problem with the encryption service
	 */
	public void loadPublicKey(final String filename) throws IOException {
		File filePublicKey = new File(filename);
		FileInputStream fis = new FileInputStream(filename);
		byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
		fis.read(encodedPublicKey);
		fis.close();

		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
		try {
			publicKey = keyFactory.generatePublic(publicKeySpec);
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
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
		File filePrivateKey = new File(filename);
		FileInputStream fis = new FileInputStream(filename);
		byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
		fis.read(encodedPrivateKey);
		fis.close();

		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);

		privateKey = keyFactory.generatePrivate(privateKeySpec);
		signature = Signature.getInstance(privateKey.getAlgorithm());
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
	 * @param signed
	 *            -- The signed object
	 * @return -- The plain object that was signed
	 * @throws ClassNotFoundException
	 *             -- Problem interpreting the internal signed object
	 * @throws IOException
	 *             -- problem reading stuff?
	 * @throws InvalidKeyException
	 *             -- The public key is not valid
	 * @throws SignatureException
	 *             -- The signature is not valid
	 */
	public Object getPlainObject(final SignedObject signed) throws ClassNotFoundException, IOException, InvalidKeyException,
			SignatureException {
		if (!signed.verify(publicKey, signature)) {
			System.err.println("Object " + signed + " not properly signed!");
			return null;
		}
		return signed.getObject();
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
				MessageCarrier mess1 = MessageCarrier.getIAmAlive("Left", "Me", "Booyah!");
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
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		} else {
			String filenamePrivate = args[1];
			String filenamePublic = args[0];

			try {
				OS.generateNewKeys(filenamePublic, filenamePrivate);
				System.out.println("Successfully generated new keys.  Public key is in file '" + filenamePublic
						+ "'.  The public key probably belongs in the database.");
				System.out.println("The private key is in '" + filenamePublic
						+ "'.  Be sure to move this private keystore to somewhere really, REALLY private!");
			} catch (NoSuchAlgorithmException | IOException e) {
				e.printStackTrace();
			}
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
}
