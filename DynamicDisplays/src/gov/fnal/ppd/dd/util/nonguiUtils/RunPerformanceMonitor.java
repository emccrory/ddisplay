package gov.fnal.ppd.dd.util.nonguiUtils;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;

import java.math.BigDecimal;

/**
 * A fun, complicated class for running the PerformanceMonitor class.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2021
 *
 */
public class RunPerformanceMonitor {

	private static BigDecimal[]	pi		= new BigDecimal[3];
	private static long[]		stps	= new long[3];
	private static boolean		stop	= false;

	protected void setPi(int index, BigDecimal p) {
		pi[index] = p;
	}

	private void setSteps(int index, long steps) {
		stps[index] = steps;
	}

	private String spaces(String string, int length) {
		String spaces = string;
		while (spaces.length() < length)
			spaces = " " + spaces;
		return spaces;
	}

	private String spaces(long L, int i) {
		String spaces = "" + L;
		while (spaces.length() < i)
			spaces = " " + spaces;
		return spaces;
	}

	/**
	 * @param args
	 */
	public RunPerformanceMonitor(final String[] args) {

		// It seems that the limit of BigDecimal for calculating pi, here, is about 23 digits
		final int NUM_DECIMALS = 30;

		// A pair of useless threads that use CPU cycles

		new Thread() {
			public void run() {
				// Calculate PI using the Gregory-Leibniz Series
				boolean plus = false;
				BigDecimal pi = new BigDecimal("4.0");
				BigDecimal four = new BigDecimal("4.0");
				long i = 1;
				for (; !stop; i++) {
					BigDecimal twoIp1 = new BigDecimal(2 * i + 1);
					if (plus) {
						pi = pi.add(four.divide(twoIp1, NUM_DECIMALS, BigDecimal.ROUND_HALF_UP));
					} else {
						pi = pi.subtract(four.divide(twoIp1, NUM_DECIMALS, BigDecimal.ROUND_HALF_DOWN));
					}
					plus = !plus;
				}
				setPi(0, pi);
				setSteps(0, i);
			}

		}.start();

		new Thread() {
			public void run() {
				// Calculate PI using the Nilakantha Series
				boolean plus = true;
				BigDecimal pi = new BigDecimal("3.0");
				BigDecimal one = new BigDecimal("1.0");
				BigDecimal two = new BigDecimal("2.0");
				BigDecimal four = new BigDecimal("4.0");
				long i = 1;
				for (; !stop; i++) {
					setPi(1, pi);
					setSteps(1, i);
					BigDecimal twoI = new BigDecimal(2 * i);
					BigDecimal denominator = twoI.multiply(twoI.add(one)).multiply(twoI.add(two));
					if (plus)
						pi = pi.add(four.divide(denominator, NUM_DECIMALS, BigDecimal.ROUND_HALF_UP));
					else
						pi = pi.subtract(four.divide(denominator, NUM_DECIMALS, BigDecimal.ROUND_HALF_DOWN));
					plus = !plus;
				}
				setPi(1, pi);
				setSteps(1, i);
			}
		}.start();

		new Thread() {
			public void run() {
				// Calculate PI using the Wallis product
				BigDecimal pi = new BigDecimal("1");
				BigDecimal one = new BigDecimal("1.0");
				BigDecimal two = new BigDecimal("2.0");
				long n = 1;
				for (; !stop; n++) {
					BigDecimal twoN = new BigDecimal(2 * n);
					BigDecimal minus1 = twoN.subtract(one);
					BigDecimal plus1 = twoN.add(one);
					BigDecimal A = twoN.divide(minus1, NUM_DECIMALS, BigDecimal.ROUND_HALF_DOWN);
					BigDecimal B = twoN.divide(plus1, NUM_DECIMALS, BigDecimal.ROUND_HALF_UP);
					BigDecimal C = A.multiply(B);
					pi = pi.multiply(C);
				}
				setPi(2, pi.multiply(two));
				setSteps(2, n);
			}
		}.start();

		int steps = 5;
		if (args.length > 0)
			steps = Integer.parseInt(args[0]);

		long delay = 2000;
		System.out.println("Running 3 pi calculations, to use up CPU cycles, while printing out the CPUS Usage " + steps
				+ " times, separated by " + delay + " milliseconds");
		// Print out the CPU usage every now and then.
		for (int i = 0; i < steps; i++) {
			System.out.println("Iteration " + i + ". CPU Usage = " + PerformanceMonitor.getCpuUsage());
			catchSleep(delay);
		}
		System.out.println("Final iteration complete. CPU Usage = " + PerformanceMonitor.getCpuUsage());
		stop = true;
		catchSleep(100);
		System.out.println("Done.  Here are the calculated values of PI -->");
		String s1 = "Pi via Gregory-Leibniz Series, in " + spaces(stps[0], 12) + " steps";
		System.out.println(s1 + " = " + pi[0]);
		System.out.println("Pi via the Nilakantha Series,  in " + spaces(stps[1], 12) + " steps = " + pi[1]);
		String s2 = pi[2].toPlainString().substring(0, NUM_DECIMALS + 2);
		System.out.println("Pi via the Wallis Product,     in " + spaces(stps[2], 12) + " steps = " + s2);
		System.out.println(
				spaces(" ... to 50 digits, it is", s1.length()) + "   3.14159265358979323846264338327950288419716939937510");
		System.exit(0);
	}
}
