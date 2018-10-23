package gov.fnal.ppd.dd.util;

@SuppressWarnings("javadoc")
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
