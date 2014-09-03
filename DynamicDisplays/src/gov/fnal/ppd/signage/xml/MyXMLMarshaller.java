package gov.fnal.ppd.signage.xml;

import static gov.fnal.ppd.GlobalVariables.XML_SERVER_NAME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/**
 * Utility class for making Strings out of XML objects, and vice versa.
 * 
 * @author Elliott McCrory, Fermilab/Accelerator, 2012
 */
public class MyXMLMarshaller {

	private MyXMLMarshaller() {
	}

	private static Marshaller getMarshaller(Class<?> clazz) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		// output pretty printed
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		jaxbMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://" + XML_SERVER_NAME + " signage.xsd");
		return jaxbMarshaller;
	}

	public static String getXML(Object data) throws JAXBException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		getMarshaller(data.getClass()).marshal(data, os);
		return os.toString();
	}

	public static void saveXML(Object data, File file) throws JAXBException {
		getMarshaller(data.getClass()).marshal(data, file);
	}

	public static Object unmarshall(Class<?> clazz, String xml) throws JAXBException {
		ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
		return JAXBContext.newInstance(clazz).createUnmarshaller().unmarshal(is);
	}

	public static Object unmarshall(Class<?> clazz, File file) throws JAXBException {
		return JAXBContext.newInstance(clazz).createUnmarshaller().unmarshal(file);
	}

}
