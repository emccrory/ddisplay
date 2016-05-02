package gov.fnal.ppd.dd.testing;

import static gov.fnal.ppd.dd.util.Util.catchSleep;

import java.lang.management.ManagementFactory;

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

	public static void main(String[] args) {
		PerformanceMonitor m = new PerformanceMonitor();

		// Two useless threads that take up CPU time
		new Thread() {
			public void run() {
				while (true) {
					double count = 1;
					for (int i = 0; i < 100; i++)
						count *= (i + 1);
				}
			}
		}.start();

		new Thread() {
			public void run() {
				while (true) {
					double count = 1;
					for (int i = 0; i < 100; i++)
						count *= (i + 1.234);
				}
			}
		}.start();

		// Print out the CPU usage every now and then.
		while (true) {
			System.out.println(m.getCpuUsage());
			catchSleep(2000L);
		}
	}
}