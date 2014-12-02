package gov.fnal.ppd.dd.testing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class AsymmetricEncryption {

	private static final String	PUBLIC_END	= "PUBLIC KEY";
	private static final String	PRIVATE_END	= "PRIVATE KEY";
	private PublicKey			publicKey;
	private PublicKey			privateKey;
	private Cipher				cipher;

	/**
	 * @param privacy
	 */
	public AsymmetricEncryption(final String privacy) {
		if (privacy.equals(PUBLIC_END))
			getPublicKey();
		else if (privacy.equals(PRIVATE_END))
			getPrivateKey();
	}

	private static void generateKeys(final int length) {
		KeyPairGenerator kpg;
		try {
			kpg = KeyPairGenerator.getInstance("RSA");

			kpg.initialize(length);
			KeyPair kp = kpg.genKeyPair();

			KeyFactory fact = KeyFactory.getInstance("RSA");
			RSAPublicKeySpec pub = fact.getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
			RSAPrivateKeySpec priv = fact.getKeySpec(kp.getPrivate(), RSAPrivateKeySpec.class);

			saveToFile("public.key", pub.getModulus(), pub.getPublicExponent());
			saveToFile("private.key", priv.getModulus(), priv.getPrivateExponent());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void saveToFile(final String fileName, final BigInteger mod, final BigInteger exp) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
		try {
			oout.writeObject(mod);
			oout.writeObject(exp);
		} catch (Exception e) {
			throw new IOException("Unexpected error", e);
		} finally {
			oout.close();
		}
	}

	private PublicKey readKeyFromFile(final String keyFileName) throws IOException {
		// InputStream in = ServerConnection.class.getResourceAsStream(keyFileName);
		InputStream in = getClass().getClassLoader().getResourceAsStream(keyFileName);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		try {
			BigInteger m = (BigInteger) oin.readObject();
			BigInteger e = (BigInteger) oin.readObject();
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			PublicKey pubKey = fact.generatePublic(keySpec);
			return pubKey;
		} catch (Exception e) {
			throw new RuntimeException("Spurious serialisation error", e);
		} finally {
			oin.close();
		}
	}

	private void getPublicKey() {
		try {
			publicKey = readKeyFromFile("/public.key");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			cipher = Cipher.getInstance("RSA");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}

	private void getPrivateKey() {
		try {
			privateKey = readKeyFromFile("/private.key");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			cipher = Cipher.getInstance("RSA");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param data
	 * @return The encrypted blob
	 */
	public byte[] rsaEncrypt(final byte[] data) {
		try {
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] cipherData = cipher.doFinal(data);
			return cipherData;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	/**
	 * @param data
	 * @return The original blob
	 */
	public byte[] rsaDecrypt(final byte[] data) {
		try {
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] cipherData = cipher.doFinal(data);
			return cipherData;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	public static void main(String[] args) {
		int length = 1024;
		AsymmetricEncryption.generateKeys(length);
	}
}
