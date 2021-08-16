/**
 * <h1>Fermilab's Dynamic Displays System</h1>
 * <h2>Introduction</h2>
 * <p>
 * The Dynamic Display system at Fermilab allows for customized content to be shown on dedicated high-definition TV’s throughout the
 * lab. The content that is being shown on each display can be changed quickly by an untrained person with access to a control
 * point.
 * </p>
 * 
 * <p>
 * Content definitions are stored in a database. Each content instance is approved by a manager at Fermilab. Many of the functions
 * for administering this database are available on-site at Fermilab (only) via interactive web pages.
 * </p>
 * 
 * <p>
 * Content is web pages. Many of the web pages used by the Fermilab experiments can be used directly as content for these displays.
 * </p>
 * 
 * <p>
 * The features of this system have been developed to have a robust, dynamic display system for use in the two remote operations
 * centers in the Wilson Hall Atrium, the ROC-W (Neutrino experiments) and the ROC-E (CMS). The system has expanded to other
 * locations.
 * </p>
 * 
 * <p>
 * This system is an alternative to commercial “digital signage” systems, with advantages unique to Fermilab:
 * <ol>
 * <li>All content is hosted web pages; no special, proprietary content system is required.</li>
 * <li>The decision as to what is shown on a display is given to the user; it usually takes less than one second for a touch on a
 * control screen to be seen on a display point.</li>
 * <li>The system can run on Windows, Mac and Unix platforms; we have chosen Scientific Linux Fermilab for most of the systems and
 * have demonstrated the other systems.</li>
 * <li>There are no licensing fees.</li>
 * </ol>
 * </p>
 * 
 * <p>
 * Features that this system has in common with commercial digital signage include:
 * <ol>
 * 
 * <li>The content is administered and approved centrally</li>
 * <li>The configuration of each element in the system is administered centrally</li>
 * <li>The status of the system is available at all times, as are local log files of activities.</li>
 * <li>It uses the Internet for all communications</li>
 * <li>Security (authorization and authentication) of these communications is designed into the system</li>
 * <li>Display units use commodity computers and HDTVs</li>
 * </ol>
 * </p>
 * <h2>package gov.fnal.ppd.dd</h2>
 * <p>
 * The classes in this top-level package
 * <ul>
 * <li>Define the GUI controller, MakeChannelSelector</li>
 * <li>Define global constants and attributes for the suite, see GlobalVariables</li>
 * <li>Contain a couple of other high-level classes</li>
 * </ul>
 * </p>
 * <h2>Java Versions and this suite</h2>
 * <p>
 * This suite has been developed under Java 7 and Java 8 (a.k/a, 1.7 and 1.8). Java 16 is available and in the pipeline to be
 * adopted at Fermilab (maybe by 2029). So it needs to be tested with this advanced version. And plans should be made to move with
 * the new java releases beyond that (e.g., Java 20). This web site gives the differences between Java 8 and Java 16 (retrieved
 * August 2021): https://ondro.inginea.eu/index.php/new-features-in-java-versions-since-java-8/
 * </p>
 * <p>
 * Most of the changes are additions to the language definition. But there are some deprecations and removals.
 * </p>
 * <p>
 * More information, including links to more documentation, can be found at the web site, https://dynamicdisplays.fnal.gov. This
 * site is accessible only from inside Fermilab, with Fermilab Services credentials
 * </p>
 * 
 * @author Elliott Simkins McCrory, PhD, Fermilab AD/Instrumentation, 2010-21
 */
package gov.fnal.ppd.dd;
