/*
 * Display
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.signage;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.net.InetAddress;

/**
 * The signature of an electronic content displayer in the Fermilab Dynamic Displays system. This is implemented as a physical
 * monitor attached to a small Linux-based PC, or as an abstraction for that physical configuration within the GUI, a.k.a.,
 * ChannelSelector.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public interface Display extends ActionListener {

	/**
	 * What content is playing on this Display?
	 * 
	 * @return The SignageContent that is playing now on this Display
	 */
	public SignageContent getContent();

	/**
	 * Change the Channel that is showing on this Display
	 * 
	 * @param c
	 *            the SignageContent to show on this Display
	 * @return The old SignageContent that was replaced by this call
	 */
	public SignageContent setContent(SignageContent c);

	/**
	 * What is the color coding for this Display?
	 * 
	 * @return the coded Color of this display
	 */
	public Color getPreferredHighlightColor();

	// public void setPreferredHighlightColor( Color c );

	/**
	 * What is the Display number? This is either the Database Index or the virutal number the user wants to see.
	 * 
	 * @return the Display number
	 */
	// public int getNumber();

	/**
	 * @return The index into the database of this display. This used to be the actual display number
	 */
	public int getDBDisplayNumber();

	/**
	 * @return The display number that the user wants to show on this display
	 */
	public int getVirtualDisplayNumber();

	/**
	 * @param d
	 */
	public void setDBDisplayNumber(int d);

	/**
	 * @param v
	 */
	public void setVirtualDisplayNumber(int v);

	/**
	 * What is the Screen number within this Display?
	 * 
	 * @return the Screen number
	 */
	public int getScreenNumber();

	/**
	 * What is the IP Address of this Display?
	 * 
	 * @return the IP number
	 */
	public InetAddress getIPAddress();

	/**
	 * Add a listener to see when things change on a Display
	 * 
	 * @param L
	 */
	public void addListener(ActionListener L);

	/**
	 * A long description, presumably including the location, of this Display
	 * 
	 * @return the Description
	 */
	public String getDescription();

	/**
	 * @return A description of the precise location of this Display
	 */
	public String getLocation();

	/**
	 * Return the status of this Display, which is expected to include if it is live and, if so, what it is showing.
	 * 
	 * @return String status message
	 */
	public String getStatus();

	/**
	 * @return The name that this Display uses on the Messaging system
	 */
	public String getMessagingName();

	/**
	 * Disconnect a display from the rest of the world (if appropriate)
	 */
	public void disconnect();

	/**
	 * A way for the Display to handle errors from the messaging system
	 * 
	 * @param message
	 */
	public void errorHandler(String message);
}
