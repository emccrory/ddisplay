package gov.fnal.ppd.dd.xml.signature;

import static gov.fnal.ppd.dd.util.Util.streamDocument;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * Taken from examples on the Oracle/java web site.
 * 
 * This is a simple example of generating an Enveloped XML Signature using the Java XML Digital Signature API. The resulting
 * signature will look like (key and signature values will be different):
 *
 * <pre>
 * <code>
 *<Envelope xmlns="urn:envelope">
 * <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
 *   <SignedInfo>
 *     <CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/>
 *     <SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/>
 *     <Reference URI="">
 *       <Transforms>
 *         <Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
 *       </Transforms>
 *       <DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
 *       <DigestValue>/juoQ4bDxElf1M+KJauO20euW+QAvvPP0nDCruCQooM=<DigestValue>
 *     </Reference>
 *   </SignedInfo>
 *   <SignatureValue>
 *     YeS+F0uiYv0h946M69Q9pKFNnD6dxUwLA8QT3GX/0H3cSPKRnNFyZiR4RPgaA1ir/ztb4rt6Lqb8
 *     hgwPERIa5qhoGUJyHDfUTcQ0Xqn1jYCVoC3ho+oUgJPXNVgtMAtpvOgxcWXUPATYdyimO6RrHF8+
 *     JXDkeICI9BPA4NKN1i77CAy6JJbaA87aNIpMJPImwJf8CM7mYsXremZz+RsafNE2cXXRzAoNOynC
 *     pi4oPYpE7CBLzhd23gf7zYRoyT06/bVIj4j3qOlVY1TQofsQ20NtAz6PbqAs7QkNoDzkX1CYlDSJ
 *     U8cGHuwXpul/UIpOiL6MZF8I/YI4ZlJn+O8Mvg==
 *   </SignatureValue>
 *   <KeyInfo>
 *     <KeyValue>
 *       <RSAKeyValue>
 *         <Modulus>
 *           mH0S/iw2K2tFTFHI75BtB67pzjR52HvQ8K7Xi5UX3NJm0oA+KX2mm0IrVcUuv609vbAAyQoW7CWm
 *           4kswVgStCm68dlw36309cxrEmPhG+PKBmUaGuBmRzwityjXRyRZJ6yaLenE8SJO/DC5ntQvmHqQQ
 *           qeOJYvz2Cbi2bi6x9XwmpqOfZCE5iTvYwioEsrglhP1uLG9fiXyNR2PXUTyLqD91HLhZFj1CEiU7
 *           aE++WfkKaowIx5p8e3F6hQ+VFRNXjtemK5aajuL0gwU+Oujg9ijgbyMh19vBoI8LruJoMOBrYFNN
 *           2boQJ3wP0Ek7CPIqAzQB5MnmvKc9jICKiiZVZw==
 *         </Modulus>
 *         <Exponent>AQAB</Exponent>
 *       </RSAKeyValue>
 *     </KeyValue>
 *   </KeyInfo>
 * </Signature>
 *</Envelope>
 * </code>
 * </pre>
 */

public class GenEnveloped {
	// Create a DOM XMLSignatureFactory that will be used to generate the enveloped signature
	private static XMLSignatureFactory	fac	= XMLSignatureFactory.getInstance("DOM");
	private static KeyPairGenerator		kpg	= null;
	private static KeyPair				kp;

	public static DOMSignContext getSignContent(PrivateKey pk, Document doc) {
		try {
			// Create a DOMSignContext and specify the RSA PrivateKey and location of the resulting XMLSignature's parent element
			return new DOMSignContext(pk, doc.getDocumentElement());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static XMLSignature getSignature(Document doc) {
		try {
			// Create a Reference to the enveloped document (in this case we are signing the whole document, so a URI of ""
			// signifies that) and also specify the SHA256 digest algorithm and the ENVELOPED Transform.

			// The example from Oracle uses the Java 9 construction, "List.of()"
			// Reference ref = fac.newReference("",fac.newDigestMethod(DigestMethod.SHA256, null),
			// List.of(fac.newTransform(Transform.ENVELOPED,(TransformParameterSpec) null)), null, null);

			List<Object> gg = new ArrayList<Object>();
			gg.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
			Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA256, null), gg, null, null);

			// Create the SignedInfo

			// The example from Oracle uses the Java 9 construction, "List.of()"
			// SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
			// (C14NMethodParameterSpec) null),fac.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",null),
			// List.of(ref));

			List<Reference> refref = new ArrayList<Reference>();
			refref.add(ref);
			SignedInfo si = fac.newSignedInfo(
					fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null),
					fac.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null), refref);

			// Create a KeyValue containing the RSA PublicKey that was generated
			KeyInfoFactory kif = fac.getKeyInfoFactory();
			KeyValue kv = kif.newKeyValue(kp.getPublic());

			// Create a KeyInfo and add the KeyValue to it
			// KeyInfo ki = kif.newKeyInfo(List.of(kv));
			List<KeyValue> hh = new ArrayList<KeyValue>();
			hh.add(kv);
			KeyInfo ki = kif.newKeyInfo(hh);

			// Create the XMLSignature (but don't sign it yet)
			return fac.newXMLSignature(si, ki);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Cryptographically sign an XML document.
	 * 
	 * @param privateKey
	 *            - The private key for signing the document
	 * @param doc
	 *            - The XML document to sign. This document is modified - the signature is added to it.
	 */
	public static void signXMLDocument(PrivateKey privateKey, Document doc) {
		try {
			XMLSignature signature = getSignature(doc);
			XMLSignContext dsc = getSignContent(privateKey, doc);

			// Marshal, generate (and sign) the enveloped signature
			signature.sign(dsc);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//
	// Synopsis: java GenEnveloped [document] [output]
	//
	// where "document" is the name of a file containing the XML document to be signed, and "output" is the name of the file to
	// store the signed document. The 2nd argument is optional - if not specified, standard output will be used.
	//
	public static void main(String[] args) throws Exception {
		// Instantiate the document to be signed
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = null;
		try (FileInputStream fis = new FileInputStream(args[0])) {
			doc = dbf.newDocumentBuilder().parse(fis);
		}

		// For the test, make a key pair on the fly
		try {
			kpg = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		kpg.initialize(2048);
		kp = kpg.generateKeyPair();

		signXMLDocument(kp.getPrivate(), doc);

		// output the resulting document
		OutputStream os;
		if (args.length > 1) {
			os = new FileOutputStream(args[1]);
		} else {
			os = System.out;
		}

		streamDocument(doc, os);
	}
}