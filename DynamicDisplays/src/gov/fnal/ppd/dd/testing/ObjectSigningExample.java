package gov.fnal.ppd.dd.testing;

import gov.fnal.ppd.dd.chat.MessageCarrier;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;

/**
 * <p>
 * A simple example of a serializable object that gets signed.
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
class ObjectSigningExample {

	public static void main(String[] args) {

		try {
			// Generate a 1024-bit Digital Signature Algorithm (DSA) key pair.
			// This would normally be done once by the originator of the message.
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA");
			keyPairGenerator.initialize(1024);
			KeyPair keyPair = keyPairGenerator.genKeyPair();
			PrivateKey privateKey = keyPair.getPrivate();
			PublicKey publicKey = keyPair.getPublic();

			// Here, we would save the private and the public keys in appropriate places that the various objects in this exchange
			// can find (appropriately)

			// I'm thinking that all the public keys will be stored in the database and the private keys will be stored on the local
			// disk of the sender, but not in a place that can normally be read. For example, ~/.keystore

			// We can sign Serializable objects only
			// String unsignedObject = new String("A Test Object");
			MessageCarrier mess = MessageCarrier.getIAmAlive("Left", "Me", "Booyah!");
			MessageCarrier mess2 = MessageCarrier.getIAmAlive("You", "Me", "Booyah!");

			Signature signature = Signature.getInstance(privateKey.getAlgorithm());
			// SignedObject signedObject = new SignedObject(unsignedObject, privateKey, signature);
			SignedObject signedMess = new SignedObject(mess, privateKey, signature);

			//
			// Here is where the object would be streamed out to someone, somewhere.
			//

			//
			// And here is where it would be streamed in from the originator of the object.
			//

			// Verify the signed object
			Signature sig = Signature.getInstance(publicKey.getAlgorithm());
			// boolean verified = signedObject.verify(publicKey, sig);
			boolean verifMes = signedMess.verify(publicKey, sig);

			// System.out.println("Is signed Object verified ? " + verified );
			System.out.println("Is signed Object verified ? " + verifMes);

			// Retrieve the object
			// unsignedObject = (String) signedObject.getObject();
			// System.out.println("Unsigned Object : " + unsignedObject);

			MessageCarrier unsignedMess = (MessageCarrier) signedMess.getObject();

			System.out.println("Original Message : " + unsignedMess);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
