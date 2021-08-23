package gov.fnal.ppd.dd.util.nonguiUtils;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.sun.management.OperatingSystemMXBean;

/**
 * This code was copied from http://stackoverflow.com/questions/25552/get-os-level-system-information, 5/2/2016.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class PerformanceMonitor {
	private int							availableProcessors	= ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	private long						lastSystemTime		= 0;
	private long						lastProcessCpuTime	= 0;
	private static PerformanceMonitor	me					= null;

	private PerformanceMonitor() {
	}

	/**
	 * @return The fraction of the available CPU usage that is being used. For a 4-processor system, each thread that is busy will
	 *         represent 0.25 units on this scale.
	 */
	public static synchronized double getCpuUsage() {
		if (me == null || me.lastSystemTime == 0) {
			me = new PerformanceMonitor();
			me.baselineCounters();
			return 0;
		}

		long systemTime = System.nanoTime();
		long processCpuTime = 0;

		if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean) {
			processCpuTime = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime();
		}

		double cpuUsage = (double) (processCpuTime - me.lastProcessCpuTime) / (systemTime - me.lastSystemTime);

		me.lastSystemTime = systemTime;
		me.lastProcessCpuTime = processCpuTime;

		return cpuUsage / me.availableProcessors;
	}

	private void baselineCounters() {
		lastSystemTime = System.nanoTime();

		if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean) {
			lastProcessCpuTime = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime();
		}
	}

	private static BigDecimal	pi;
	private static long			s;

	protected static void setPi(BigDecimal p) {
		pi = p;
	}

	private static void setSteps(long i) {
		s = i;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		@SuppressWarnings("unused")
		PerformanceMonitor m = new PerformanceMonitor();

		// A useless thread that takes up CPU time

		new Thread() {
			public void run() {
				boolean plus = false;
				BigDecimal pi = new BigDecimal("4.0");
				BigDecimal four = new BigDecimal("4.0");
				for (long i = 1; true; i++) {
					if (plus) {
						pi = pi.add(four.divide(new BigDecimal(2 * i + 1), 25, BigDecimal.ROUND_HALF_UP));
					} else {
						pi = pi.subtract(four.divide(new BigDecimal(2 * i + 1), 25, BigDecimal.ROUND_HALF_UP));
					}
					setPi(pi);
					setSteps(i);
					plus = !plus;
				}
			}

		}.start();

		int steps = 5;
		if (args.length > 0)
			steps = Integer.parseInt(args[0]);

		// Print out the CPU usage every now and then.
		for (int i = 0; i < steps; i++) {
			System.out.println(PerformanceMonitor.getCpuUsage());
			catchSleep(2000L);
		}
		System.out.println(PerformanceMonitor.getCpuUsage());
		System.out.println("Done.\nPi as calculated in " + s + " steps = " + pi + ", "
				+ "\n             ...    but really it is " + Math.PI);
		System.exit(0);
	}

}