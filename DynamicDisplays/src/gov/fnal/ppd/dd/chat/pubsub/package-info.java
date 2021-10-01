/**
 * <p>
 * Fermilab's Dynamic Displays System -- A clean and simple attempt, copied from the Internet, to do Publish/Subscribe
 * </p>
 * 
 * <p>
 * This was created September 2021. Note that the first attempt was implemented by hand, by me, in the MessagingServer, in around
 * 2018. It is likely that that first attempt was not a great implementation.
 * </p>
 * <p>
 * This is untested. And it is not sophisticated enough to use for our system, but it might be a start.
 * </p>
 * <p>
 * These files were taken from
 * <a href="https://riptutorial.com/design-patterns/example/6498/publish-subscribe-in-java">riptutorial.com</a> on 09/23/2021.</a>
 * </p>
 * <p>
 * This version has been refactored to have the main classes be defined by an interface. The classes in the simple sub-package are
 * very close to the version obtained from the RipTutorial site.
 * </p>
 */
package gov.fnal.ppd.dd.chat.pubsub;
