package gov.fnal.ppd.dd.xml.signature;

import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Collections;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class SignatureUsingDSAKeys {

	public static void main(String[] args) throws Exception {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = null;
		try (FileInputStream fis = new FileInputStream(args[2])) {
			doc = dbf.newDocumentBuilder().parse(fis);
		}

		GenEnveloped.loadPrivateKey(args[0]);
		GenEnveloped.loadPublicKey(args[1]);

		XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance();
		Reference ref = sigFactory.newReference("#Body", sigFactory.newDigestMethod(DigestMethod.SHA1, null));
		SignedInfo signedInfo = sigFactory.newSignedInfo(
				sigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
						(C14NMethodParameterSpec) null),
				sigFactory.newSignatureMethod(SignatureMethod.DSA_SHA1, null), Collections.singletonList(ref));
		KeyInfoFactory kif = sigFactory.getKeyInfoFactory();
		KeyValue kv = kif.newKeyValue(GenEnveloped.publicKey);
		KeyInfo keyInfo = kif.newKeyInfo(Collections.singletonList(kv));

		XMLSignature xmlSig = sigFactory.newXMLSignature(signedInfo, keyInfo);
	}
}