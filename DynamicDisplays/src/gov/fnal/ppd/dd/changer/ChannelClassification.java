/*
 * ChannelClassification
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import java.io.Serializable;

/**
 * The way the channels are organized/sorted for the user.
 * 
 * This corresponds to the field ChannelTabSort.Type in the database. (I really need to unify these names!)
 * 
 * TODO - Currently, there is a one-to-one relationship between Channels and ChannelClassification's. This might not be the right choice.
 * In other words, why can't a Channel be associated with two categories?
 * 
 * FIXME - There seems to be some confusion. There is the ChannelTabSort.Type field, and then this class is used to distinguish
 * between regular, web-based content and content that comes directly from an image.
 * 
 * April 2018: Removed the pre-defined categories. This is held exclusively in the database
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelClassification implements Serializable {

	private static final long serialVersionUID = 1359531755323832880L;

	/**
	 * The first of two assumed categories: Web-page content (presumably)
	 */
	public static final ChannelClassification	MISCELLANEOUS		= new ChannelClassification("Miscellaneous");

	/**
	 * The second of two assumed categories: Channels that are JPEG images
	 */
	public static final ChannelClassification	IMAGE				= new ChannelClassification("Image");

	private String						theClassification;
	private String						abbreviation;

	/**
	 * Needed for XML encoding and decoding
	 */
	public ChannelClassification() {
	}

	/**
	 * @param s
	 */
	public ChannelClassification(final String s) {
		if (s != null) {
			this.theClassification = s;
			this.abbreviation = s.toUpperCase().replace(" ", "");
			if (s.length() > 5)
				this.abbreviation = s.substring(0, 5);
		}
	}

	/**
	 * @param s
	 * @param a
	 */
	public ChannelClassification(final String s, final String a) {
		this.theClassification = s;
		this.abbreviation = a.toUpperCase();
	}

	/**
	 * @return -- The name of the category
	 */
	public String getValue() {
		return theClassification;
	}

	/**
	 * @param value
	 *            -- The new name for this category
	 */
	public void setValue(final String value) {
		this.theClassification = value;
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
		result = prime * result + ((theClassification == null) ? 0 : theClassification.hashCode());
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
		ChannelClassification other = (ChannelClassification) obj;
		if (theClassification == null) {
			if (other.theClassification != null)
				return false;
		} else if (!theClassification.toUpperCase().equals(other.theClassification.toUpperCase()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return theClassification + " [" + abbreviation + "]";
	}
}
