package gov.fnal.ppd.signage.testing;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Use the Cipher stuff in javax.crypto
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class CipherTest {

	// Let's assume the bytes to encrypt are in

	// Next, you'll need the key and initialization vector bytes

	private Cipher			cipher;
	private SecretKeySpec	key;
	private IvParameterSpec	ivSpec;

	// Now you can initialize the Cipher for the algorithm that you select:

	/**
	 * @param keyBytes
	 * @param ivBytes
	 * @param encType
	 *            Use "DES"
	 * @param encTypeFull
	 *            use "DES/CBC/PKCS5Padding"
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public CipherTest(final byte[] keyBytes, final byte[] ivBytes, final String encType, final String encTypeFull)
			throws NoSuchAlgorithmException, NoSuchPaddingException {
		// wrap key data in Key/IV specs to pass to cipher
		key = new SecretKeySpec(keyBytes, encType);
		ivSpec = new IvParameterSpec(ivBytes);

		// create the cipher with the algorithm you choose
		// see javadoc for Cipher class for more info, e.g.
		cipher = Cipher.getInstance(encTypeFull);

	}

	/**
	 * @param input
	 * @return The encrypted data
	 * @throws InvalidKeyException
	 * @throws ShortBufferException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidAlgorithmParameterException
	 */
	public byte[] encrypt(final byte[] input) throws InvalidKeyException, ShortBufferException, IllegalBlockSizeException,
			BadPaddingException, InvalidAlgorithmParameterException {
		// Encryption would go like this:

		cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
		byte[] encrypted = new byte[cipher.getOutputSize(input.length)];
		int enc_len = cipher.update(input, 0, input.length, encrypted, 0);
		enc_len += cipher.doFinal(encrypted, enc_len);
		return encrypted;
	}

	/**
	 * @param encrypted
	 * @return The decrypted data
	 * @throws InvalidKeyException
	 * @throws ShortBufferException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidAlgorithmParameterException
	 */
	public byte[] decrypt(final byte[] encrypted) throws InvalidKeyException, ShortBufferException, IllegalBlockSizeException,
			BadPaddingException, InvalidAlgorithmParameterException {
		// And decryption like this:

		cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
		int enc_len = encrypted.length;
		byte[] decrypted = new byte[cipher.getOutputSize(enc_len)];
		int dec_len = cipher.update(encrypted, 0, enc_len, decrypted, 0);
		dec_len += cipher.doFinal(decrypted, dec_len);
		return decrypted;

	}

}
