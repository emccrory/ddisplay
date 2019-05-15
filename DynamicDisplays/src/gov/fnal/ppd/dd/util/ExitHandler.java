package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;

import java.util.ArrayList;
import java.util.List;


/**
 * There are several places in the code where it is necessary to restart the whole shebang. This class handles a graceful exit by
 * running the commands that others have set up to assure that the restart is clean. 
 * 
 * This is implemented in the Display client so that it can save the current Channel and then show it again when it gets restarted.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ExitHandler {

	private static List<Command>	finalCommand	= new ArrayList<Command>();

	private ExitHandler() {
	}
	public static void saveAndExit(String why) {
		saveAndExit(why, -1);
	}
	/**
	 * Run whatever commands have been queued up, and then exit. The exit code of -1 should be a signal to the controlling script
	 * that we want to be restarted.
	 */
	public static void saveAndExit(String why, int exitCode) {
		println(ExitHandler.class,
				"\n\n********** Exit handler called **********\nThere are at most " + finalCommand.size() + " exit hooks to call");
		
		// Execute the requested commands
		int count = 0;
		for (Command C : finalCommand)
			if (C != null) {
				C.execute(why);
				count++;
			}

		catchSleep(1000); // Wait for these commands to complete ...

		println(ExitHandler.class, "\n********** EXITING **********\n [" + why + "] - " + count + " exit commands executed\n");
		
		// Exit the Java VM entirely - this will activate the system-registered shutdown hooks.
		System.exit(exitCode);
	}

	/**
	 * Remove this command from the list.
	 * 
	 * @param c
	 *            The command to remove
	 */
	public static void removeFinalCommand(final Command c) {
		finalCommand.remove(c);
	}

	/**
	 * @param c
	 *            the command to add to the list of commands to be executed prior to exiting
	 * @return The command is returned back to the sender
	 */
	public static Command addFinalCommand(final Command c) {
		finalCommand.add(c);
		return c;
	}

}
