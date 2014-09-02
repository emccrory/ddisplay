package gov.fnal.ppd.signage.channel;

import gov.fnal.ppd.signage.changer.ChannelCategory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An empty channel that can be held by a display during startup (to avoid null pointer violation elsewhere)
 * 
 * @author Elliott McCrory, Fermilab/AD/Instrumentation.
 * 
 */
public class EmptyChannel extends ChannelImpl {
	private static final long	serialVersionUID	= -419375552528263824L;

	private static final String	DEFAULT_URLS[]		= { "http://mccrory.fnal.gov/XOC/kenburns/portfolioDisplay.php?exp=MINOS",
			"http://mccrory.fnal.gov/XOC/kenburns/portfolioDisplay.php?exp=MINERvA",
			"http://mccrory.fnal.gov/XOC/kenburns/portfolioDisplay.php?exp=MiniBooNE",
			"http://mccrory.fnal.gov/XOC/kenburns/portfolioDisplay.php?exp=MicroBooNE",
			"http://mccrory.fnal.gov/XOC/kenburns/portfolioDisplay.php?exp=Mu2e",
			"http://mccrory.fnal.gov/XOC/kenburns/portfolioDisplay.php?exp=gMinus2",
			"http://mccrory.fnal.gov/XOC/kenburns/portfolioDisplay.php?exp=SeaQuest",
			"http://mccrory.fnal.gov/XOC/kenburns/portfolioDisplay.php?exp=NOvA",
			"http://mccrory.fnal.gov/XOC/kenburns/portfolioDisplay.php?exp=LBNE",
			"http://www-bd.fnal.gov/notifyservlet/www?refresh=on", //
			"http://vmsstreamer1.fnal.gov/live/novanearcams.htm", //
			"http://vmsstreamer1.fnal.gov/live/novawebcams.htm", //
			"http://elliottmccrory.com/clock/five.html", };

	private static final String	MY_URL				= DEFAULT_URLS[(int) (DEFAULT_URLS.length * Math.random())];

	/**
	 * Create an Empty Channel
	 * 
	 * @throws URISyntaxException
	 *             (In the unlikely case that the main XOC web page is no longer a valid URI)
	 */
	public EmptyChannel() throws URISyntaxException {
		super("EmptyChannel", ChannelCategory.PUBLIC, "This channel is undefined", new URI(MY_URL), 0);

	}

	public static void main(String[] args) {
		System.out.println(MY_URL);
	}
}
