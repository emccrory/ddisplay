package gov.fnal.ppd.dd.xml.signature;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import gov.fnal.ppd.dd.util.nonguiUtils.PropertiesFile;
import gov.fnal.ppd.dd.xml.XMLDocumentAndString;

/**
 * Taken from the example code on the Oracle/Java site.
 * 
 * This is an example of validating an XML Signature using the XML Signature API. It assumes the key needed to validate the
 * signature is contained in a KeyValue KeyInfo.
 */
public class Validate {

	private static DocumentBuilderFactory	dbf		= DocumentBuilderFactory.newInstance();
	private static boolean					debug	= PropertiesFile.getBooleanProperty("IncomingMessVerbose", false);

	static {
		dbf.setNamespaceAware(true);
	}

	//
	// Synopsis: java Validate [document]
	//
	// where "document" is the name of a file containing the XML document to be validated.
	//
	public static void main(String[] args) throws Exception {

		if (args.length < 1) {
			System.err.println("Please specify a filename for the signed XML document.");
			System.exit(-1);
		}

		System.out.println("Checking file " + args[0] + " for a valid signature");

		// Instantiate the document to be validated
		Document doc = null;
		try (FileInputStream fis = new FileInputStream(args[0])) {
			doc = dbf.newDocumentBuilder().parse(fis);
		}
		boolean coreValidity = isSignatureValid(new XMLDocumentAndString(doc));

		// Check core validation status
		if (coreValidity == false) {
			System.err.println("Signature FAILED core validation");
		} else {
			System.out.println("Signature passed core validation");
		}
	}

	/**
	 * Validate the signature of an XML document. This document contains the public key. This method is used only for testing.
	 * 
	 * @param doc
	 *            - The document that is signed. The public key is contained in this document
	 * @return - Is this document properly signed?
	 * @throws SignatureNotFoundException
	 *             - if there is no signature on this document
	 * @throws MarshalException
	 * @throws XMLSignatureException
	 */
	public static boolean isSignatureValid(XMLDocumentAndString doc)
			throws SignatureNotFoundException, MarshalException, XMLSignatureException {

		// Find Signature element
		NodeList nl = doc.getTheDocument().getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
		if (nl.getLength() == 0) {
			throw new SignatureNotFoundException("Cannot find Signature element");
		}

		// Create a DOM XMLSignatureFactory that will be used to unmarshal the document containing the XMLSignature
		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

		DOMValidateContext valContext = new DOMValidateContext(new KeyValueKeySelector(), nl.item(0));

		// unmarshal the XMLSignature
		XMLSignature signature = fac.unmarshalXMLSignature(valContext);

		// Validate the XMLSignature (generated above)
		boolean coreValidity = signature.validate(valContext);

		if (debug) {
			System.out.println("Here is a string representation of the internal Document:\n" + doc.getTheXML());
			boolean sv = signature.getSignatureValue().validate(valContext);
			System.out.println("Is the signature itself valid? " + sv);
			// check the validation status of each Reference
			InputStream is = signature.getSignedInfo().getCanonicalizedData();
			byte[] b = new byte[doc.getTheXML().length() + 100];
			try {
				int num = is.read(b);
				System.out.println("Read " + num + " bytes: " + Arrays.toString(b));
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (Object i : signature.getSignedInfo().getReferences()) {
				if (i instanceof Reference) {
					Reference R = (Reference) i;
					boolean refValid = R.validate(valContext);
					System.out.println("Does the reference, " + R
							+ ", that the signature is supposed to be signing match properly? " + refValid);
					System.out.println("Calculated Digest Values: " + Arrays.toString(R.getCalculatedDigestValue()));
				}
			}
		} else {
			// Check core validation status
			if (coreValidity == false) {
				printlnErr(Validate.class, "Signature FAILED validation check");
			} else {
				println(Validate.class, "Signature passed validation check");
			}
		}
		return coreValidity;

	}

	/**
	 * KeySelector which retrieves the public key out of the KeyValue element and returns it.
	 * 
	 * NOTE: If the key algorithm doesn't match signature algorithm, then the public key will be ignored.
	 */
	private static class KeyValueKeySelector extends KeySelector {
		public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose purpose, AlgorithmMethod method,
				XMLCryptoContext context) throws KeySelectorException {
			if (keyInfo == null) {
				throw new KeySelectorException("Null KeyInfo object!");
			}
			SignatureMethod sm = (SignatureMethod) method;
			@SuppressWarnings("unchecked")
			List<XMLStructure> list = keyInfo.getContent();
			if (debug)
				println(Validate.class, "We see " + list.size() + " keyInfo elements");

			for (int i = 0; i < list.size(); i++) {
				XMLStructure xmlStructure = list.get(i);
				if (debug)
					println(Validate.class, "Found element of type " + xmlStructure.getClass().getCanonicalName());
				if (xmlStructure instanceof KeyValue) {
					PublicKey pk = null;
					try {
						pk = ((KeyValue) xmlStructure).getPublicKey();
					} catch (KeyException ke) {
						throw new KeySelectorException(ke);
					}
					// make sure algorithm is compatible with method
					if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
						return new SimpleKeySelectorResult(pk);
					}
				} else {
				}
			}
			throw new KeySelectorException("No KeyValue element found!");
		}

		static boolean algEquals(String algURI, String algName) {
			if (algName.equalsIgnoreCase("DSA") && (algURI.equalsIgnoreCase("http://www.w3.org/2009/xmldsig11#dsa-sha256")
					|| algURI.equalsIgnoreCase("http://www.w3.org/2000/09/xmldsig#dsa-sha1"))) {
				return true;
			} else if (algName.equalsIgnoreCase("RSA")
					&& algURI.equalsIgnoreCase("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256")) {
				return true;
			} else {
				return false;
			}
		}
	}

	private static class SimpleKeySelectorResult implements KeySelectorResult {
		private PublicKey pk;

		SimpleKeySelectorResult(PublicKey pk) {
			this.pk = pk;
		}

		public Key getKey() {
			return pk;
		}
	}

}