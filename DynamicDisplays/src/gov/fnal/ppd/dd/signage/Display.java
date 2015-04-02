package gov.fnal.ppd.dd.signage;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.net.InetAddress;

/**
 * The signature of a Dynamic Display in the Fermilab Dynamic Display System
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
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
	 * What is the Display number?
	 * 
	 * @return the Display number
	 */
	public int getNumber();

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
	 * What kind of Display is this?
	 * 
	 * @return the Display category
	 */
	public SignageType getCategory();

	/*
	 * Set the DNS name of this Display. The Display class will internally set this to the InetAddress
	 * 
	 * @param ipName
	 */
	// public void setIpName( String ipName ) throws UnknownHostException;

	/*
	 * Set the screen number for this Display. The default is 0.
	 * 
	 * @param screenNumber
	 */
	// public void setScreenNumber( int screenNumber );

	/*
	 * Set the string that specifies the location of this Display
	 * 
	 * @param location
	 */
	// public void setLocation( String location );

	/**
	 * @return A description of the location of this Display
	 */
	public String getLocation();

	// public void setNumber( int number );

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
	 * A way foir the Display to handle errors from the messaging system
	 * 
	 * @param message
	 */
	public void errorHandler(String message);
}
