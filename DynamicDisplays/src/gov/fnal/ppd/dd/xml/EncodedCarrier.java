package gov.fnal.ppd.dd.xml;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.bind.annotation.XmlElement;

/**
 * Base class for all the XML classes.
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation, 2012-14
 */
public class EncodedCarrier {

	protected String	dateString	= new Date().toString();
	protected String	ipAddress;

	/**
	 * @return the date string
	 */
	@XmlElement
	public String getDate() {
		return dateString;
	}

	/**
	 * @param d
	 */
	public void setDate(final String d) {
		dateString = d;
	}

	/**
	 * @return the IP Address string
	 */
	@XmlElement
	public String getIPAddress() {
		return ipAddress;
	}

	/**
	 * @param d
	 */
	public void setIPAddress(final String d) {
		ipAddress = d;
	}

	/**
	 * @return -- the age of this message in milliseconds
	 */
	public long howOld() {
		try { // Thu Dec 18 15:54:43 CST 2014
			DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
			Date dd = format.parse(dateString);
			long then = dd.getTime();
			// System.out.println("----------> Date from message interpreted as '" + dd + "'");
			return System.currentTimeMillis() - then;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return 3600 * 1000L; // an hour
	}
}
