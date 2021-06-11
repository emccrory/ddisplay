/**
 * <p>
 * Fermilab's Dynamic Displays System -- Implementation of the Emergency Messages in the system.
 * </p>
 * <p>
 * The basic idea is that a small group of carefully-selected people at the lab will have the rights/credentials to issue what we
 * are calling "Emergency Messages" to the Dynamic Display systems. When this emergency message is received by the messaging server,
 * and the identity of the sender is verified and cleared (using the signature the message), the server will forward this message to
 * all of the displays at the lab. (Note: It is possible to reduce the number of displays that get this message, e.g., for a
 * directed message or for testing.)
 * </p>
 * <p>
 * When a display receives the message, it will also verify the signature of the message. When it passes this test, the message will
 * appear on top of the channel that is playing on that display. The display decides how to display the message - this is based mostly
 * on the "severity" of the received message. That is, a message of severity "EMERGENCY" will show in large font and a bold red
 * outline.
 * </p>
 * <p>
 * The length of time the message is the lesser of (a) the length of time indicated on the message, or (2) when the user who
 * launched the message sends another message to remove it. (a) is determined in the GUI and can be adjusted by the user. But the
 * defaults are 60 seconds for the least severe level ("TEST") to one hour for the most severe ("EMERGENCY")
 * </p>
 * <p>
 * To be clear, it is thought that something like an active shooter incident would be "EMERGENCY". An "ALERT" might be a snow storm
 * that is closing the lab. A "TEST" would be just that - a test of the system.
 * </p>
 * <p>
 * Elliott McCrory, Fermilab/FRA, Instrumentation Department
 * </p>
 */
package gov.fnal.ppd.dd.emergency;
