package gov.fnal.ppd.dd.xml.signature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SignedXMLDocument {
	private Document						unsignedDocument	= null;
	private Document						signedDocument		= null;
	private static DocumentBuilderFactory	dbf;
	private boolean							valid				= false;

	static {
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
	}

	/**
	 * Make a new, signed, XML document
	 * 
	 * @param rawUnsignedXML
	 *            - a String that is the XML document
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public SignedXMLDocument(String rawXML) throws Exception {
		this(dbf.newDocumentBuilder().parse(new InputSource(new StringReader(rawXML))));
	}

	/**
	 * Make a new XML document
	 * 
	 * @param valid
	 * 
	 * @param doc
	 *            - the XML document as an object of type org.w3c.dom.Document
	 */
	public SignedXMLDocument(Document theDoc) {
		try {
			if (SignXMLUsingDSAKeys.validate(theDoc)) {
				this.signedDocument = theDoc;
				unsignedDocument = null;
			} else {
				this.unsignedDocument = theDoc;
				signedDocument = null;
			}
			valid = true;
		} catch (Exception e) {
			e.printStackTrace();
			valid = false;
		}
	}

	public Document getUnsignedDocument() {
		if (!valid)
			return null;
		if (unsignedDocument == null)
			return signedDocument;

		return unsignedDocument;
	}

	public Document getSignedDocument() {
		if (!valid)
			return null;
		if (signedDocument == null)
			try {
				ByteArrayOutputStream outStream = new ByteArrayOutputStream();

				SignXMLUsingDSAKeys.signDocument(unsignedDocument, outStream);
				ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());

				signedDocument = dbf.newDocumentBuilder().parse((inStream));
			} catch (Exception e) {
				e.printStackTrace();
				valid = false;
				signedDocument = null;
			}
		return signedDocument;
	}

	@Override
	public int hashCode() {
		final int prime = 47;
		int result = 1;
		// FixMe - this might not work as the Document class does not define a hashCode function.
		result = prime * result + ((signedDocument == null) ? 0 : signedDocument.hashCode());
		result = prime * result + ((unsignedDocument == null) ? 0 : unsignedDocument.hashCode());
		result = prime * result + (valid ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		// FixMe - this might not work as the Document class does not define an equals function.
		SignedXMLDocument other = (SignedXMLDocument) obj;
		if (signedDocument == null) {
			if (other.signedDocument != null)
				return false;
		} else if (!signedDocument.equals(other.signedDocument))
			return false;
		if (unsignedDocument == null) {
			if (other.unsignedDocument != null)
				return false;
		} else if (!unsignedDocument.equals(other.unsignedDocument))
			return false;
		
		if (valid != other.valid)
			return false;
		return true;
	}
	
	
}
