/*
 * EncryptUtils
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.testing;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * At this time (June, 2014), a placeholder
 * 
 * This class was taken from an example on the Internet
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class EncryptUtils {
	Cipher	ecipher;
	Cipher	dcipher;

	/**
	 * @param key
	 */
	public EncryptUtils(final SecretKey key) {
		// Create an 8-byte initialization vector
		byte[] iv = new byte[] { (byte) 0x8E, 0x12, 0x39, (byte) 0x9C, 0x07, 0x72, 0x6F, 0x5A };
		AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
		try {
			ecipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			dcipher = Cipher.getInstance("DES/CBC/PKCS5Padding");

			// CBC requires an initialization vector
			ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
			dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
		} catch (InvalidAlgorithmParameterException e) {

		} catch (NoSuchPaddingException e) {

		} catch (NoSuchAlgorithmException e) {

		} catch (java.security.InvalidKeyException e) {

		}
	}

	// Buffer used to transport the bytes from one stream to another
	byte[]	buf	= new byte[1024];

	/**
	 * @param in
	 * @param out
	 */
	public void encrypt(final InputStream in, OutputStream out) {
		try {
			// Bytes written to out will be encrypted
			out = new CipherOutputStream(out, ecipher);

			// Read in the cleartext bytes and write to out to encrypt
			int numRead = 0;
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}
			out.close();
		} catch (java.io.IOException e) {
		}
	}

	/**
	 * @param in
	 * @param out
	 */
	public void decrypt(InputStream in, final OutputStream out) {
		try {
			// Bytes read from in will be decrypted
			in = new CipherInputStream(in, dcipher);

			// Read in the decrypted bytes and write the cleartext to out
			int numRead = 0;
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}
			out.close();
		} catch (java.io.IOException e) {
		}
	}

	/**
	 * @param args
	 */
	public final static void main(final String[] args) {
		try {
			// Generate a temporary key. In practice, you would save this key.
			// See also Encrypting with DES Using a Pass Phrase.
			SecretKey key = KeyGenerator.getInstance("DES").generateKey();

			// Create encrypter/decrypter class
			EncryptUtils encrypter = new EncryptUtils(key);

			// Encrypt
			encrypter.encrypt(new FileInputStream("runDisplay.sh"), new FileOutputStream("ciphertext"));

			// Decrypt
			encrypter.decrypt(new FileInputStream("ciphertext"), new FileOutputStream("cleartext2"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}