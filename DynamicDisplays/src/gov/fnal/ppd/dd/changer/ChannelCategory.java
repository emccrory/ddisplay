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

	/**
	 * The first of two assumed categories: Everything else
	 */
	public static final ChannelCategory	MISCELLANEOUS		= new ChannelCategory("Miscellaneous");

	/**
	 * The second of two assumed categories: Channels that are JPEG images
	 */
	public static final ChannelCategory	IMAGE				= new ChannelCategory("Image");


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
		this.value = s;
		this.abbreviation = s.toUpperCase();
		if (s != null && s.length() > 5)
			this.abbreviation = s.substring(0, 5);
	}

	/**
	 * @param s
	 * @param a
	 */
	public ChannelCategory(final String s, final String a) {
		this.value = s;
		this.abbreviation = a.toUpperCase();
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
		this.abbreviation = abbreviation.toUpperCase();
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
		return value + " [" + abbreviation + "]";
	}
}
