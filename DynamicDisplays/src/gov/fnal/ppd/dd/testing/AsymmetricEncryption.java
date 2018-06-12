/*
 * EncryptionService
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
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
		try (ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))) {
			oout.writeObject(mod);
			oout.writeObject(exp);
		} catch (Exception e) {
			throw new IOException("Unexpected error", e);
		}
	}

	private PublicKey readKeyFromFile(final String keyFileName) {
		// InputStream in = ServerConnection.class.getResourceAsStream(keyFileName);
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(keyFileName)) {
			try (ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in))) {
				BigInteger m = (BigInteger) oin.readObject();
				BigInteger e = (BigInteger) oin.readObject();
				RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
				KeyFactory fact = KeyFactory.getInstance("RSA");
				PublicKey pubKey = fact.generatePublic(keySpec);
				return pubKey;
			}
		} catch (Exception e) {
			throw new RuntimeException("Spurious serialisation error", e);
		}
	}

	private void getPublicKey() {
		publicKey = readKeyFromFile("/public.key");

		try {
			cipher = Cipher.getInstance("RSA");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void getPrivateKey() {
		privateKey = readKeyFromFile("/private.key");

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

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		int length = 1024;
		AsymmetricEncryption.generateKeys(length);
	}
}
