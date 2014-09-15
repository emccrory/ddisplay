package gov.fnal.ppd.signage.channel;

import static gov.fnal.ppd.GlobalVariables.WEB_SERVER_NAME;
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

	private static final String	DEFAULT_URLS[]		= { "http://" + WEB_SERVER_NAME + "/XOC/kenburns/portfolioDisplay.php?exp=MINOS",
			"http://" + WEB_SERVER_NAME + "/XOC/kenburns/portfolioDisplay.php?exp=MINERvA",
			"http://" + WEB_SERVER_NAME + "/XOC/kenburns/portfolioDisplay.php?exp=MiniBooNE",
			"http://" + WEB_SERVER_NAME + "/XOC/kenburns/portfolioDisplay.php?exp=MicroBooNE",
			"http://" + WEB_SERVER_NAME + "/XOC/kenburns/portfolioDisplay.php?exp=Mu2e",
			"http://" + WEB_SERVER_NAME + "/XOC/kenburns/portfolioDisplay.php?exp=gMinus2",
			"http://" + WEB_SERVER_NAME + "/XOC/kenburns/portfolioDisplay.php?exp=SeaQuest",
			"http://" + WEB_SERVER_NAME + "/XOC/kenburns/portfolioDisplay.php?exp=NOvA",
			"http://" + WEB_SERVER_NAME + "/XOC/kenburns/portfolioDisplay.php?exp=LBNE",
			"http://" + WEB_SERVER_NAME + "/XOC/kenburns/portfolioDisplay.php?exp=NuMI", //
			"http://www-bd.fnal.gov/notifyservlet/www?project=HD&refresh=on&infolinks=none", //
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
	
	/**
	 * @param url
	 * @throws URISyntaxException
	 */
	public EmptyChannel(final String url) throws URISyntaxException {
		super("EmptyChannel", ChannelCategory.PUBLIC, "This channel is undefined", new URI(url), 0);

	}

	public static void main(String[] args) {
		System.out.println(MY_URL);
	}
}
