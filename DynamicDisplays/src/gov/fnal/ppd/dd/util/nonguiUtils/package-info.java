/**
 * Utilities that do not depend on any of the other classes in this package and are not GUI related
 * 
 * There is a warning in the class PerformanceMonitor:
 * 
 * Access restriction: The type 'OperatingSystemMXBean' is not API (restriction on required library
 * '/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.302.b08-0.el7_9.x86_64/jre/lib/rt.jar')
 * 
 * This may bite us someday. That functionality is used in the display app in order to report the CPU usage on the computer that is
 * running this display, and then place into the display's status. This IS NOT important to the operation of the display client. So
 * if the inaccessibility of com.sun.management.OperatingSystemMXBean, it can be eliminated.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
package gov.fnal.ppd.dd.util.nonguiUtils;