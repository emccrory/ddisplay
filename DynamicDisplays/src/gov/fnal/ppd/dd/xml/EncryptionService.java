/*
 * EncryptionService
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.xml;

import java.security.MessageDigest;

import javax.xml.bind.DatatypeConverter;

/**
 * A service for conducting the encryption of messages.
 * 
 * This has not been used yet. It does not seem necessary to encrypt the messages here - the signature is fine. But we leave it here
 * in case that impression changes.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public final class EncryptionService {
	private static EncryptionService instance;

	private EncryptionService() {
	}

	/**
	 * @param plaintext
	 *            The String to encrypt
	 * @return The encrypted string
	 * @throws Exception
	 */
	public synchronized String encrypt(final String plaintext) throws Exception {
		MessageDigest md = null;

		md = MessageDigest.getInstance("SHA"); // step 2
		md.update(plaintext.getBytes("UTF-8")); // step 3

		byte raw[] = md.digest(); // step 4
		// String hash = (new BASE64Encoder()).encode(raw); // step 5
		String hash = DatatypeConverter.printBase64Binary(raw);
		// String hash = BASE64.getEncoder.encode(raw); // step 5 (this works in Java 8, but not Java 7)
		return hash; // step 6
	}

	/**
	 * @return The singleton instance of this service
	 */
	public static synchronized EncryptionService getInstance() // step 1
	{
		if (instance == null) {
			instance = new EncryptionService();
		}
		return instance;
	}
}