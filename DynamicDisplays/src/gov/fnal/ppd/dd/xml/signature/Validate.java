package gov.fnal.ppd.dd.xml.signature;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;

/**
 * Taken from the example code on the Oracle/Java site.
 * 
 * This is a simple example of validating an XML Signature using the XML Signature API. It assumes the key needed to validate the
 * signature is contained in a KeyValue KeyInfo.
 */
public class Validate {

	private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

	static {
		dbf.setNamespaceAware(true);
	}

	//
	// Synopsis: java Validate [document]
	//
	// where "document" is the name of a file containing the XML document to be validated.
	//
	public static void main(String[] args) throws Exception {

		// SignXMLUsingDSAKeys.loadPublicKey(args[0]);

		// Instantiate the document to be validated
		Document doc = null;
		try (FileInputStream fis = new FileInputStream(args[0])) {
			doc = dbf.newDocumentBuilder().parse(fis);
		}
		boolean coreValidity = isSignatureValid(doc);

		// Check core validation status
		if (coreValidity == false) {
			System.err.println("Signature failed core validation");
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
	private static boolean isSignatureValid(Document doc)
			throws SignatureNotFoundException, MarshalException, XMLSignatureException {
		// Find Signature element
		NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
		if (nl.getLength() == 0) {
			throw new SignatureNotFoundException("Cannot find Signature element");
		}

		// Create a DOM XMLSignatureFactory that will be used to unmarshal the document containing the XMLSignature
		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

		// Create a DOMValidateContext and specify a KeyValue KeySelector and document context
		// DOMValidateContext valContext = new DOMValidateContext(new KeyValueKeySelector(), nl.item(0));
		DOMValidateContext valContext = new DOMValidateContext(GenEnveloped.publicKey, nl.item(0));

		// unmarshal the XMLSignature
		XMLSignature signature = fac.unmarshalXMLSignature(valContext);

		// Validate the XMLSignature (generated above)
		return signature.validate(valContext);
	}

	/**
	 * 
	 * @param read
	 * @return
	 * @throws JAXBException
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws XMLSignatureException 
	 * @throws MarshalException 
	 * @throws SignatureNotFoundException 
	 */
	public static boolean isSignatureValid(Object read) throws JAXBException, SAXException, IOException, ParserConfigurationException, SignatureNotFoundException, MarshalException, XMLSignatureException {
		String xmlString = MyXMLMarshaller.getXML(read);
		Document doc = dbf.newDocumentBuilder().parse(xmlString);
		return isSignatureValid(doc);
	}

	/**
	 * Validate the signature of an XML document. You must provide the Public Key that matches the encrypting Private Key for this
	 * document.
	 * 
	 * @param pubKey
	 *            - The Public Key that matches the encrypting Private Key
	 * @param doc
	 *            - The document that is signed.
	 * @return - Is this document properly signed?
	 * @throws SignatureNotFoundException
	 *             - if there is no signature on this document
	 * @throws MarshalException
	 * @throws XMLSignatureException
	 */
	public static boolean isSignatureValid(PublicKey pubKey, Document doc)
			throws SignatureNotFoundException, MarshalException, XMLSignatureException {
		// Find Signature element
		NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
		if (nl.getLength() == 0) {
			throw new SignatureNotFoundException("Cannot find Signature element");
		}

		// Create a DOM XMLSignatureFactory that will be used to unmarshal the document containing the XMLSignature
		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

		// Create a DOMValidateContext and specify a KeyValue KeySelector and document context
		DOMValidateContext valContext = new DOMValidateContext(pubKey, nl.item(0));

		// unmarshal the XMLSignature
		XMLSignature signature = fac.unmarshalXMLSignature(valContext);

		// Validate the XMLSignature (generated above)
		return signature.validate(valContext);
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

			for (int i = 0; i < list.size(); i++) {
				XMLStructure xmlStructure = list.get(i);
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
				}
			}
			throw new KeySelectorException("No KeyValue element found!");
		}

		static boolean algEquals(String algURI, String algName) {
			if (algName.equalsIgnoreCase("DSA") && algURI.equalsIgnoreCase("http://www.w3.org/2009/xmldsig11#dsa-sha256")) {
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