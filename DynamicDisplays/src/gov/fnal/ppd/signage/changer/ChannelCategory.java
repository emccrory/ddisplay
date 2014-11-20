package gov.fnal.ppd.signage.changer;

/**
 * The way the channels are organized/sorted for the user.
 * 
 * FIXME -- I believe now (11/2014) that this is a cumbersome way to do it. These values should be stored by the database (as
 * strings). The enum here is more trouble than it is worth; I need to be able to make these categories more flexible, especially as
 * the Dynamic Display system expands outside of the ROC-W (formerly called the XOC).
 * 
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class ChannelCategory {
	// This enum corresponds exactly to the names chosen for the values in the Database table, ChannelTabSort

	/**
	 * Channels on the front page
	 */
	public static final ChannelCategory	PUBLIC				= new ChannelCategory("PUBLIC", "Public");

	/**
	 * Channels on the second page
	 */
	public static final ChannelCategory	PUBLIC_DETAILS		= new ChannelCategory("PUBLIC_DETAILS", "Details");

	/**
	 * Channels on the front page of the "Experiments" view
	 */
	public static final ChannelCategory	EXPERIMENT_DETAILS	= new ChannelCategory("EXPERIMENT_DETAILS", "Exp");

	/**
	 * Channels that show details on the NuMI experiment
	 */
	public static final ChannelCategory	NUMI_DETAILS		= new ChannelCategory("NUMI_DETAILS", "NuMI");

	/**
	 * Channels that show details on the NOvA experiment
	 */
	public static final ChannelCategory	NOVA_DETAILS		= new ChannelCategory("NOVA_DETAILS", "NOvA");

	/**
	 * Channels that are (YouTube) videos
	 */
	public static final ChannelCategory	VIDEOS				= new ChannelCategory("VIDEOS", "Videos");

	/**
	 * Everything else
	 */
	public static final ChannelCategory	MISCELLANEOUS		= new ChannelCategory("MISCELLANEOUS", "MISC");

	/**
	 * Channels that are actually just JPEG images
	 */
	public static final ChannelCategory	IMAGE				= new ChannelCategory("IMAGE", "Image");

	/**
	 * Channels that give details on accelerator operations
	 */
	public static final ChannelCategory	ACCELERATOR			= new ChannelCategory("ACCELERATOR", "Beam");

	/**
	 * Channels that show general stuff about Fermilab itself
	 */
	public static final ChannelCategory	FERMILAB			= new ChannelCategory("FERMILAB", "Fermi");

	private String						value;
	private String						abbreviation;

	public ChannelCategory(String s) {
		this.value = this.abbreviation = s;
		if (s != null && s.length() > 5)
			this.abbreviation = s.substring(0, 5);
	}

	public ChannelCategory(String s, String a) {
		this.value = s;
		this.abbreviation = a;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getAbbreviation() {
		return abbreviation;
	}

	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChannelCategory other = (ChannelCategory) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.toUpperCase().equals(other.value.toUpperCase()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return value;
	}
}
