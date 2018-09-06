package gov.fnal.ppd.dd.util;

/**
 * Implementation of the well-known "Command" pattern.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public interface Command {

	/**
	 * Execute the command.
	 * 
	 */
	public void execute(String why);
}
