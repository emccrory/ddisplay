/*
 * current_observation
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.xml.news;

import gov.fnal.ppd.dd.xml.MyXMLMarshaller;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This is what the NOAA airport RSS feed looks like. 
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@XmlRootElement(name = "current_observation")
@SuppressWarnings("javadoc")
public class current_observation {

	private String	credit, credit_url, image, suggested_pickup, location, station_id, observation_time;
	private String	observation_time_rfc822, weather, temperature_string, wind_string, wind_dir, pressure_string, dewpoint_string;
	private String	windchill_string, icon_url_base, ob_url, disclaimer_url, copyright_url, privacy_policy_url;

	private int		suggested_pickup_period;
	private double	lattitude, longitude, temp_f, temp_c, relative_humidity, wind_degrees;
	private double	wind_mph, wind_kt, pressure_mb, pressure_in, dewpoint_f, dewpoint_c, windchill_f, windchill_c, visibility_mi;

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Path path = FileSystems.getDefault().getPath(args[0]);
		String xml = "";
		try {
			List<String> lines = Files.readAllLines(path, Charset.defaultCharset());
			boolean start = false;
			for (String L : lines) {
				if (L.contains("<current_observation")) {
					start = true;
				}
				if (start && L.length() > 0)
					xml += L;
				// else
				// System.out.println("-- skipping " + L);
			}
		} catch (IOException e) {
			System.err.println(path.toAbsolutePath());
			e.printStackTrace();
		}

		// System.out.println(xml);
		try {
			current_observation c = (current_observation) MyXMLMarshaller.unmarshall(current_observation.class, xml);

			System.out.println(c.getTemp_f() + "\u00B0");

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String getTemperature_string() {
		return temperature_string;
	}

	public void setTemperature_string(String temperature_string) {
		this.temperature_string = temperature_string;
	}

	@XmlElement
	public String getCredit() {
		return credit;
	}

	public void setCredit(String credit) {
		this.credit = credit;
	}

	@XmlElement
	public String getCredit_url() {
		return credit_url;
	}

	public void setCredit_url(String credit_url) {
		this.credit_url = credit_url;
	}

	@XmlElement
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	@XmlElement
	public String getSuggested_pickup() {
		return suggested_pickup;
	}

	public void setSuggested_pickup(String suggested_pickup) {
		this.suggested_pickup = suggested_pickup;
	}

	@XmlElement
	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@XmlElement
	public String getStation_id() {
		return station_id;
	}

	public void setStation_id(String station_id) {
		this.station_id = station_id;
	}

	@XmlElement
	public String getObservation_time() {
		return observation_time;
	}

	public void setObnservation_time(String obnservation_time) {
		this.observation_time = obnservation_time;
	}

	@XmlElement
	public String getObservation_time_rfc822() {
		return observation_time_rfc822;
	}

	public void setObservation_time_rfc822(String observation_time_rfc822) {
		this.observation_time_rfc822 = observation_time_rfc822;
	}

	@XmlElement
	public String getWeather() {
		return weather;
	}

	public void setWeather(String weather) {
		this.weather = weather;
	}

	

	@XmlElement
	public String getWind_string() {
		return wind_string;
	}

	public void setWind_string(String wind_string) {
		this.wind_string = wind_string;
	}

	@XmlElement
	public String getWind_dir() {
		return wind_dir;
	}

	public void setWind_dir(String wind_dir) {
		this.wind_dir = wind_dir;
	}

	@XmlElement
	public String getPressure_string() {
		return pressure_string;
	}

	public void setPressure_string(String pressure_string) {
		this.pressure_string = pressure_string;
	}

	@XmlElement
	public String getDewpoint_string() {
		return dewpoint_string;
	}

	public void setDewpoint_string(String dewpoint_string) {
		this.dewpoint_string = dewpoint_string;
	}

	@XmlElement
	public String getWindchill_string() {
		return windchill_string;
	}

	public void setWindchill_string(String windchill_string) {
		this.windchill_string = windchill_string;
	}

	@XmlElement
	public String getIcon_url_base() {
		return icon_url_base;
	}

	public void setIcon_url_base(String icon_url_base) {
		this.icon_url_base = icon_url_base;
	}

	@XmlElement
	public String getOb_url() {
		return ob_url;
	}

	public void setOb_url(String ob_url) {
		this.ob_url = ob_url;
	}

	@XmlElement
	public String getDisclaimer_url() {
		return disclaimer_url;
	}

	public void setDisclaimer_url(String disclaimer_url) {
		this.disclaimer_url = disclaimer_url;
	}

	@XmlElement
	public String getCopyright_url() {
		return copyright_url;
	}

	public void setCopyright_url(String copyright_url) {
		this.copyright_url = copyright_url;
	}

	@XmlElement
	public String getPrivacy_policy_url() {
		return privacy_policy_url;
	}

	public void setPrivacy_policy_url(String privacy_policy_url) {
		this.privacy_policy_url = privacy_policy_url;
	}

	@XmlElement
	public int getSuggested_pickup_period() {
		return suggested_pickup_period;
	}

	public void setSuggested_pickup_period(int suggested_pickup_period) {
		this.suggested_pickup_period = suggested_pickup_period;
	}

	@XmlElement
	public double getLattitude() {
		return lattitude;
	}

	public void setLattitude(double lattitude) {
		this.lattitude = lattitude;
	}

	@XmlElement
	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	@XmlElement
	public double getTemp_f() {
		return temp_f;
	}

	public void setTemp_f(double temp_f) {
		this.temp_f = temp_f;
	}

	@XmlElement
	public double getTemp_c() {
		return temp_c;
	}

	public void setTemp_c(double temp_c) {
		this.temp_c = temp_c;
	}

	@XmlElement
	public double getRelative_humidity() {
		return relative_humidity;
	}

	public void setRelative_humidity(double relative_humidity) {
		this.relative_humidity = relative_humidity;
	}

	public double getWind_degrees() {
		return wind_degrees;
	}

	public void setWind_degrees(double wind_degrees) {
		this.wind_degrees = wind_degrees;
	}

	@XmlElement
	public double getWind_mph() {
		return wind_mph;
	}

	public void setWind_mph(double wind_mph) {
		this.wind_mph = wind_mph;
	}

	public double getWind_kt() {
		return wind_kt;
	}

	public void setWind_kt(double wind_kt) {
		this.wind_kt = wind_kt;
	}

	@XmlElement
	public double getPressure_mb() {
		return pressure_mb;
	}

	public void setPressure_mb(double pressure_mb) {
		this.pressure_mb = pressure_mb;
	}

	@XmlElement
	public double getPressure_in() {
		return pressure_in;
	}

	public void setPressure_in(double pressure_in) {
		this.pressure_in = pressure_in;
	}

	@XmlElement
	public double getDewpoint_f() {
		return dewpoint_f;
	}

	public void setDewpoint_f(double dewpoint_f) {
		this.dewpoint_f = dewpoint_f;
	}

	@XmlElement
	public double getDewpoint_c() {
		return dewpoint_c;
	}

	public void setDewpoint_c(double dewpoint_c) {
		this.dewpoint_c = dewpoint_c;
	}

	@XmlElement
	public double getWindchill_f() {
		return windchill_f;
	}

	public void setWindchill_f(double windchill_f) {
		this.windchill_f = windchill_f;
	}

	@XmlElement
	public double getWindchill_c() {
		return windchill_c;
	}

	public void setWindchill_c(double windchill_c) {
		this.windchill_c = windchill_c;
	}

	@XmlElement
	public double getVisibility_mi() {
		return visibility_mi;
	}

	public void setVisibility_mi(double visibility_mi) {
		this.visibility_mi = visibility_mi;
	}
}
