package gov.fnal.ppd.dd.xml;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLUtilities {

	private XMLUtilities() {
	}

	public static Document convertFromXMLToDocument(final String xmlDocument)
			throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		return dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xmlDocument)));
	}
}
