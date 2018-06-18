package gov.fnal.ppd.dd.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class PropertiesFile {
	private static Properties	prop			= new Properties();
	private final static String	PROPERTY_FILE	= System.getProperty("user.dir") + "/config/config.properties";

	@SuppressWarnings("javadoc")
	public static void main(String[] args) {
		System.out.println("Testing the oepration of the properties file used here, " + PROPERTY_FILE);
		readPropertiesFile();
		writePropertiesFile("Testing");
		readPropertiesFile();
	}

	@SuppressWarnings("javadoc")
	public static void readPropertiesFile() {
		try (InputStream is = new FileInputStream(PROPERTY_FILE)) {
			prop.load(is);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("javadoc")
	public static void writePropertiesFile(final String comment) {
		try (OutputStream output = new FileOutputStream(PROPERTY_FILE)) {
			prop.store(output, comment);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("javadoc")
	public static String getProperty(String string) {
		if (prop.isEmpty())
			readPropertiesFile();
		return prop.getProperty(string);
	}
}
