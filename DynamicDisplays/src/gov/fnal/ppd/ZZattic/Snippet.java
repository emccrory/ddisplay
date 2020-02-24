package gov.fnal.ppd.ZZattic;

public class Snippet {}
//	/**
//	 * @param client
//	 *            -- The name of the client for which you'll need to check the signature
//	 * @return -- The object that knows about this client's public key
//	 * @deprecated
//	 */
//	public static ObjectSigning getPublicSigning(final String client) {
//		// if (keys.containsKey(client) && keys.get(client) != null)
//		// return keys.get(client);
//		//
//		// ObjectSigning thatObject = new ObjectSigning();
//		// if (thatObject.loadPublicKeyFromDB(client)) {
//		// keys.put(client, thatObject);
//		// clientControlList.put(client, loadDisplayListFromDB(client));
//		// return thatObject;
//		// }
//		// keys.remove(client);
//		return null;
//	}
//	
//	/**
//	 * @param toSign
//	 *            -- The object to sign. Must be Serialzable.
//	 * @return -- The signed object
//	 * @throws SignatureException
//	 *             -- An invalid signature
//	 * @throws IOException
//	 *             -- A problem reading the keystore
//	 * @throws NoSuchAlgorithmException
//	 *             -- A problem with the encryption service * @throws InvalidKeySpecException
//	 * @throws InvalidKeyException
//	 *             -- The private key is not valid
//	 * @deprecated - use XML signing techniques
//	 */
//	public SignedObject getSignedObject(final Serializable toSign)
//			throws SignatureException, NoSuchAlgorithmException, InvalidKeyException, IOException {
//		assert (privateKey != null);
//	
//		if (signature == null)
//			signature = Signature.getInstance(privateKey.getAlgorithm());
//	
//		return new SignedObject(toSign, privateKey, signature);
//	}
//	
//	/**
//	 * @param signedMess
//	 *            -- The message to test
//	 * @return -- null if the message is signed properly; a string explaining why if it is not.
//	 * @deprecated - use SignedXMLDocument now-a-days
//	 */
//	public String verifySignature(final SignedObject signedMess) {
//		assert (signedMess != null);
//	
//		// if (!checkSignedMessages()) {
//		// System.err.println(getClass().getSimpleName() + ".verifySignature(): Ignoring the signature and returning 'true'");
//		// return null;
//		// }
//		//
//		// if (publicKey == null)
//		// return "No public key!";
//		//
//		// try {
//		// // System.err.println(getClass().getSimpleName() + ".verifySignature(): really and truly checking the signature!");
//		// if (sig == null)
//		// sig = Signature.getInstance(publicKey.getAlgorithm());
//		// // The "FastBugs" error for this line is illogical.
//		// boolean retval = signedMess.verify(publicKey, sig);
//		// if (!retval) {
//		// for (String k : keys.keySet()) {
//		// if (keys.get(k) == this) { // Yes, I think "==" is right here: Are these the same objects?
//		// keys.remove(k);
//		// break;
//		// }
//		// }
//		// return "Signature is invalid";
//		// }
//		// return null; // The object is properly signed.
//		// } catch (Exception e) {
//		// e.printStackTrace();
//		// }
//		return "Exeption caused the signature check to fail";
//	}
//	
//	/**
//	 * @param toSign
//	 *            -- The object to sign
//	 * @return -- The signed object that corresponds to the passed object
//	 * @deprecated
//	 */
//	public SignedObject example(final Serializable toSign) {
//		// try {
//		// // We can sign Serializable objects only
//		// signature = Signature.getInstance(privateKey.getAlgorithm());
//		// SignedObject signedMess = new SignedObject(toSign, privateKey, signature);
//		//
//		// // Verify the signed object
//		// Signature sig = Signature.getInstance(publicKey.getAlgorithm());
//		// boolean verifMes = signedMess.verify(publicKey, sig);
//		//
//		// // System.out.println("Is signed Object verified ? " + verified );
//		// System.out.println("Is signed Object verified ? " + verifMes);
//		//
//		// // Retrieve the object
//		// MessageCarrier unsignedMess = (MessageCarrier) signedMess.getObject();
//		//
//		// System.out.println("Original Message : " + unsignedMess);
//		//
//		// return signedMess;
//		//
//		// } catch (Exception e) {
//		// e.printStackTrace();
//		// }
//		return null;
//	}
//	
}

