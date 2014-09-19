package gov.fnal.ppd;

/**
 * Encapsulate the orientation of the display (not really used yet)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @Copyright 2014
 * 
 */
public enum Orientation {
	/**
	 * The screen orientation is not known
	 */
	UNDEFINED,
	/**
	 * The screen is perfectly square
	 */
	SQUARE,
	/**
	 * the screen is in a portrait orientation (taller)
	 */
	PORTRAIT,
	/**
	 * The screen is in a landscape orientation (wider)
	 */
	LANDSCAPE
}