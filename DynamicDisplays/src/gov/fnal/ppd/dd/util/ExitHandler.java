package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.util.Util.println;
import java.util.ArrayList;
import java.util.List;


/**
 * There are several places in the code where it is necessary to restart the who shebang. This class handles a graceful exit by
 * running the commands that others have set up to assure that the restart is clean. (The initial implementation of this is in the
 * Display client so that the current Channel is redisplayed when we are restarted.)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ExitHandler {

	private static List<Command>	finalCommand	= new ArrayList<Command>();

	private ExitHandler() {
	}

	/**
	 * Run whatever commands have been queued up, and then exit. The exit code of -1 should be a signal to the controlling script
	 * that we want to be restarted.
	 */
	public static void saveAndExit(String why) {
		// Execute the listed commands
		for (Command C : finalCommand)
			if (C != null)
				C.execute(why);

		println(ExitHandler.class, "\n\n********** EXITING **********\n\n [" + why + "]\n");
		// Exit the Java VM entirely
		System.exit(-1);
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
