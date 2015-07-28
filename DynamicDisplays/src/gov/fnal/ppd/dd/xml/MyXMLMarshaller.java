package gov.fnal.ppd.dd.xml;

import static gov.fnal.ppd.dd.GlobalVariables.XML_SERVER_NAME;

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
@SuppressWarnings("javadoc")
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
		try {
			ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
			return JAXBContext.newInstance(clazz).createUnmarshaller().unmarshal(is);
		} catch (InternalError ie) {
			/**
			 * This exception happens in this rare case:
			 * <ol>
			 * <li>The URL has not been changed since the JVM started</li>
			 * <li>The Java version has been upgraded while the JVM was running</li>
			 * <li>A message is received.</li>
			 * </ol>
			 * 
			 * What happens is that while the underlying code is trying to load the class that has been sent to us, one of the new
			 * classes it needs to load is supposed to be in a well-known jar file, but that jar file is now missing!
			 */
			System.err.println("Horrible exception of type " + InternalError.class.getCanonicalName()
					+ "!  This probably means that a new version of Java was recently put onto this system.  "
					+ "Your best bet now is to restart the Display.");
			ie.printStackTrace();
		}
		return null;
	}

	public static Object unmarshall(Class<?> clazz, File file) throws JAXBException {
		return JAXBContext.newInstance(clazz).createUnmarshaller().unmarshal(file);
	}

}
