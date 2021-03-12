package gov.fnal.ppd.dd.util.nonguiUtils;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class PropertiesFile {
	private static Properties prop = new Properties();
	private static String PROPERTY_FILE;

	static {
		PROPERTY_FILE = System.getProperty("user.dir") + File.separator + "config" + File.separator
				+ "config.properties";
		File t = new File(PROPERTY_FILE);
		if (!t.exists()) {
			PROPERTY_FILE = System.getProperty("user.dir") + File.separator + ".." + File.separator + "config"
					+ File.separator + "config.properties";
			t = new File(PROPERTY_FILE);
			if ( !t.exists() ) {
				printlnErr(PropertiesFile.class, "Connot find the properties configuration file.  Tried:\n\"" +
						System.getProperty("user.dir") + File.separator + "config" + File.separator
						+ "config.properties" + "\" and \n\t" +System.getProperty("user.dir") + File.separator + ".." + File.separator + "config"
						+ File.separator + "config.properties");
				System.exit(-1);
			}
		}
	}

	/**
	 * The ways to try to position and full-screen the browser
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 */
	public static enum PositioningMethod {
		/**
		 * Use the JavaScript "maximize()" function
		 */
		DirectPositioning, /**
							 * 
							 */
		UseHiddenButton, /**
							 * 
							 */
		ChangeIframe, /**
						 * 
						 */
		PressF11, /**
					 * 
					 */
		DoNothing
	}

	@SuppressWarnings("javadoc")
	public static void main(String[] args) {
		System.out.println("Testing the oepration of the properties file used here, " + PROPERTY_FILE);
		readPropertiesFile();
		writePropertiesFile("Testing");
		readPropertiesFile();
	}

	public static void reset() {
		prop = new Properties();
	}

	@SuppressWarnings("javadoc")
	public static void readPropertiesFile() {
		try (InputStream is = new FileInputStream(PROPERTY_FILE)) {
			prop.load(is);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("javadoc")
	public static void writePropertiesFile(final String comment) {
		try (OutputStream output = new FileOutputStream(PROPERTY_FILE)) {
			prop.store(output, comment);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("javadoc")
	public static String getProperty(String string) {
		if (prop.isEmpty())
			readPropertiesFile();
		String retval = prop.getProperty(string);
		if (retval == null)
			return null;
		while (retval.endsWith(" "))
			retval = retval.substring(0, retval.length() - 1);
		return retval;
	}

	/**
	 * @return The chosen positioning and full-screen method
	 */
	public static PositioningMethod getPositioningMethod() {
		String pm = getProperty("positioningMethod");
		return PositioningMethod.valueOf(pm);
	}

	/**
	 * @param theProperty
	 * @param theDefault
	 * @return The value in the property file for this string
	 */
	public static boolean getBooleanProperty(final String theProperty, final boolean theDefault) {
		String val = getProperty(theProperty);
		if (val == null)
			return theDefault;
		if (val.equals("0"))
			return false;
		if (val.equals("1"))
			return true;
		return val.equalsIgnoreCase("true");
	}

	/**
	 * @param theProperty
	 * @param theDefault
	 * @return The value in the property file for this string
	 */
	public static int getIntProperty(final String theProperty, final int theDefault) {
		String val = getProperty(theProperty);
		if (val == null)
			return theDefault;
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			printlnErr(PropertiesFile.class,
					"Problem interpreting the value of '" + theProperty + "' (" + val + ") - " + e.getMessage());
		}
		return theDefault;
	}

	/**
	 * @param theProperty
	 * @param theDefault
	 * @return The property corresponding to the first argument
	 */
	public static String getProperty(final String theProperty, final String theDefault) {
		String val = getProperty(theProperty);
		if (val == null)
			return theDefault;
		return val;
	}
}
