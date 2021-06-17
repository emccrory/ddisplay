package gov.fnal.ppd.dd.xml.signature;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

/**
 * Utilities for going between text and XML documents.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014-21
 *
 */
@XmlRootElement
public class XMLDocumentUtilities {
	private XMLDocumentUtilities() {
	}

	private static DocumentBuilderFactory	dbf;
	private static StringWriter				writer	= new StringWriter();
	private static TransformerFactory		tf		= TransformerFactory.newInstance();
	private static StreamResult				result	= new StreamResult(writer);

	static {
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
	}

	public static Document convertToDocument(String message) {
		try {
			return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(message.getBytes()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String convertToString(Document doc) {
		try {
			tf.newTransformer().transform(new DOMSource(doc), result);
			return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Nothing to see here";
	}
}
