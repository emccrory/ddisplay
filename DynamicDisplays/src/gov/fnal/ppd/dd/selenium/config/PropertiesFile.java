package gov.fnal.ppd.dd.selenium.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class PropertiesFile {
	static Properties prop = new Properties();

	public static void main(String[] args) {
		readPropertiesFile();
		writePropertiesFile("Testing");
		readPropertiesFile();
	}

	public static void readPropertiesFile() {
		try {
			InputStream is = new FileInputStream(System.getProperty("user.dir")
					+ "/src/gov/fnal/ppd/dd/selenium/config/config.properties");
			prop.load(is);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void writePropertiesFile(String comment) {
		try {
			OutputStream output = new FileOutputStream(
					System.getProperty("user.dir")
							+ "/src/gov/fnal/ppd/dd/config/config.properties");

			prop.store(output, comment);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getProperty(String string) {
		if (prop.isEmpty())
			readPropertiesFile();
		return prop.getProperty(string);
	}
}
