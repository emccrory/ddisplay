package gov.fnal.ppd.dd.display.client.selenium.config;

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
	static Properties prop = new Properties();

	@SuppressWarnings("javadoc")
	public static void main(String[] args) {
		readPropertiesFile();
		writePropertiesFile("Testing");
		readPropertiesFile();
	}

	@SuppressWarnings("javadoc")
	public static void readPropertiesFile() {
		try {
			InputStream is = new FileInputStream(System.getProperty("user.dir")
					+ "/src/gov/fnal/ppd/dd/display/client/selenium/config/config.properties");
			prop.load(is);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("javadoc")
	public static void writePropertiesFile(final String comment) {
		try {
			OutputStream output = new FileOutputStream(
					System.getProperty("user.dir")
							+ "/src/gov/fnal/ppd/dd/config/config.properties");

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
