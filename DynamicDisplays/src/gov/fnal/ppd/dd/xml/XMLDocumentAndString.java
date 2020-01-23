package gov.fnal.ppd.dd.xml;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import gov.fnal.ppd.dd.CredentialsNotFoundException;

// import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import gov.fnal.ppd.dd.xml.signature.SignXMLUsingDSAKeys;
import gov.fnal.ppd.dd.xml.signature.Validate;
import gov.fnal.ppd.dd.xml.signature.XMLDocumentUtilities;

public class XMLDocumentAndString {

	private static DocumentBuilderFactory	dbf	= DocumentBuilderFactory.newInstance();

	private Document						theDoc;
	private String							theXML;

	public XMLDocumentAndString(Document doc) {
		theDoc = doc;
		theXML = XMLDocumentUtilities.convertToString(doc);
	}

	public XMLDocumentAndString(String xml) {
		theXML = xml;
		theDoc = XMLDocumentUtilities.convertToDocument(xml);
	}

	public String getTheXML() {
		return theXML;
	}

	public Document getTheDocument() {
		return theDoc;
	}

	public XMLDocumentAndString getSignedDocument()
			throws MarshalException, XMLSignatureException, TransformerException, InstantiationException, IllegalAccessException,
			ClassNotFoundException, KeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		ByteArrayOutputStream localOutputStream = new ByteArrayOutputStream();
		SignXMLUsingDSAKeys.signDocument(getTheDocument(), localOutputStream);
		String theString = localOutputStream.toString();

		// the method, signDocument, changes the document. So put it back to its unsigned state.
		theDoc = XMLDocumentUtilities.convertToDocument(theXML);

		return new XMLDocumentAndString(theString);
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Please specify a filename for the signed XML document.");
			System.exit(-1);
		}

		Document doc = null;
		try (FileInputStream fis = new FileInputStream(args[0])) {
			doc = dbf.newDocumentBuilder().parse(fis);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			e.printStackTrace();
		}
		XMLDocumentAndString d1 = new XMLDocumentAndString(doc);
		XMLDocumentAndString d2 = new XMLDocumentAndString(d1.getTheXML());

		if (args.length < 2 || args[1].equalsIgnoreCase("R")) {
			System.out.println("Doing a round-trip on the XML document " + args[0]);
			// Instantiate the document to be validated

			if (d1.getTheXML().equals(d2.getTheXML())) {
				System.out.println("The XMLs match!");
			} else {
				System.out.println("The XMLs DO NOT match!");
			}

			// There does not seem to be a Document.equals() method. So it falls bask to Object.equals(), which fails.
			// if (d1.getTheDocument().equals(d2.getTheDocument())) {
			// System.out.println("The Docs match!");
			// } else {
			// System.out.println("The round-trip constructions: the Docs do NOT match!");
			// }
		} else {
			try {
				credentialsSetup();
			} catch (CredentialsNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup();
			try {
				SignXMLUsingDSAKeys.setupDSA(args[1], "ad130482 selector 00");
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchAlgorithmException
					| InvalidAlgorithmParameterException | InvalidKeySpecException | KeyException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// Sign the document from the file and see what happens
			try {
				XMLDocumentAndString signed = d1.getSignedDocument();
				if (Validate.isSignatureValid(signed))
					System.err.println("Signed successfully");
				else
					System.err.println("The signature is NOT VALID");

			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(0);
		}
	}
}
