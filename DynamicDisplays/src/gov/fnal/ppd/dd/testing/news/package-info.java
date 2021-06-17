/**
 * <p>
 * Fermilab's Dynamic Displays System -- Implementation of XML objects for an RSS News feed.
 * </p>
 * <p>
 * This was abandoned in 2019 because it had become clear that doing this right would be a big project in and of itself. The
 * complexities include:
 * <ul>
 * <li>That the XML used in an arbitrary RSS feed is really complicated. We'd have to implement a full XML interpreter. Java has
 * this, but understanding that implementation in order to make a meaningful text-only display of that information (alone) would be
 * really hard.</li>
 * <li>RSS Feeds are not as popular now (2021) as they were in, say, 2015. Are they on their way out?</li>
 * <li>The HTML implementation of a scrolling chyron, sitting on top of an arbitrary web page, is also hard.</li>
 * </ul>
 * <p>
 * <p>
 * IMHO, this is the sort of problem that would/could be solved by putting lots of smart people on is - maybe the solution is doable
 * and simple, but I do not see it.
 * </p>
 */
package gov.fnal.ppd.dd.testing.news;
