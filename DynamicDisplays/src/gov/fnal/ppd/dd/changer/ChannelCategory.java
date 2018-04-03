/*
 * ChannelCategory
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import java.io.Serializable;

/**
 * The way the channels are organized/sorted for the user.
 * 
 * This corresponds to the Channel "Type" in the database (and the accompanying "Abbreviation")
 * 
 * April 2018: Removed the pre-defined categories. This is held exclusively in the database
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelCategory implements Serializable {

	private static final long			serialVersionUID	= 7118118150912431599L;

	// This enum corresponds exactly to the names chosen for the values in the Database table, ChannelTabSort

	// /**
	// * Channels on the front page
	// */
	// public static final ChannelCategory PUBLIC = new ChannelCategory("PUBLIC", "Public");
	//
	// /**
	// * Channels on the second page
	// */
	// public static final ChannelCategory PUBLIC_DETAILS = new ChannelCategory("PUBLIC_DETAILS", "Details");
	//
	// /**
	// * Channels on the front page of the "Experiments" view
	// */
	// public static final ChannelCategory EXPERIMENT_DETAILS = new ChannelCategory("EXPERIMENT_DETAILS", "Exp");
	//
	// /**
	// * Channels that show details on the NuMI experiment
	// */
	// public static final ChannelCategory NUMI_DETAILS = new ChannelCategory("NUMI_DETAILS", "NuMI");
	//
	// /**
	// * Channels that show details on the NOvA experiment
	// */
	// public static final ChannelCategory NOVA_DETAILS = new ChannelCategory("NOVA_DETAILS", "NO\u03BDA");
	//
	// /**
	// * Channels that are (YouTube) videos
	// */
	// public static final ChannelCategory VIDEOS = new ChannelCategory("VIDEOS", "Videos");
	//
	/**
	 * Everything else
	 */
	public static final ChannelCategory	MISCELLANEOUS		= new ChannelCategory("MISCELLANEOUS", "MISC");

	/**
	 * Channels that are actually just JPEG images
	 */
	public static final ChannelCategory	IMAGE				= new ChannelCategory("IMAGE", "Image");

	// /**
	// * Channels that give details on accelerator operations
	// */
	// public static final ChannelCategory ACCELERATOR = new ChannelCategory("ACCELERATOR", "Beam");
	//
	// /**
	// * Channels that show general stuff about Fermilab itself
	// */
	// public static final ChannelCategory FERMILAB = new ChannelCategory("FERMILAB", "Fermi");

	private String						value;
	private String						abbreviation;

	/**
	 * Needed for XML encoding and decoding
	 */
	public ChannelCategory() {
	}

	/**
	 * @param s
	 */
	public ChannelCategory(final String s) {
		this.value = this.abbreviation = s;
		if (s != null && s.length() > 5)
			this.abbreviation = s.substring(0, 5);
	}

	/**
	 * @param s
	 * @param a
	 */
	public ChannelCategory(final String s, final String a) {
		this.value = s;
		this.abbreviation = a;
	}

	/**
	 * @return -- The name of the category
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value
	 *            -- The new name for this category
	 */
	public void setValue(final String value) {
		this.value = value;
	}

	/**
	 * @return -- The short name for this category
	 */
	public String getAbbreviation() {
		return abbreviation;
	}

	/**
	 * @param abbreviation
	 */
	public void setAbbreviation(final String abbreviation) {
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
		// println(getClass(), ".equals(" + toString() + ", " + obj + ")");
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
		return value + " [" + abbreviation + "]";
	}
}
