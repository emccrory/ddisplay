package gov.fnal.ppd.dd.testing;

/**
 * This test program was used to learn how exit codes are generated in the JVM across different architectures (e.g., Windows versus
 * Linux versus Mac)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2020-21
 *
 */
public class TestExitCode {
	public static void main(String[] args) {
		int exitCode = 0;
		if (args.length > 0)
			exitCode = Integer.parseInt(args[0]);

		System.out.println("Begin.  Exit code will be " + exitCode);
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Exiting now.");
		System.exit(exitCode);
	}
}
