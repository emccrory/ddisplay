package gov.fnal.ppd.dd.display.client;

/**
 * What type of "wrapper" shall we use to show our web pages?
 * 
 * <p>
 * TODO -- this has become messy; I need to re-do this to represent what we actually have now.
 * <ol>
 * <li>Does it have a clock or not?</li>
 * <li>Does it have a ticker or not?</li>
 * <li>What ticker should it show? The URL/filename for this ticker should be completely out of this code and in the DB</li>
 * </ol>
 * </p>
 * <p>
 * All wrapper programs should have the capability of multiple frames
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public enum WrapperType {
	/**
	 * Use the normal border.php web page (with no ticker or extra frame)
	 */
	NORMAL,

	/**
	 * Use the border web page that includes the News Ticker
	 */
	TICKER,

	/**
	 * Use the border web page that includes the News Ticker AND one or more extra iFrames for announcements
	 */
	TICKERANDFRAME,

	/**
	 * There are multiple (4) frames but no ticker.
	 */
	FRAMESNOTICKER,

	/**
	 * There one extra frame but no ticker.
	 */
	FRAMENOTICKER,

	/**
	 * Show the "Fermilab News" ticker
	 */
	FERMITICKER,

	/**
	 * Show naked web pages without the border (untested)
	 */
	NONE;

	/**
	 * @param val
	 *            The value
	 * @return the enum that corresponds to this value
	 */
	public static WrapperType getWrapperType(final int val) {
		switch (val) {
		case 0:
			return NORMAL;
		case 1:
			return TICKER;
		case 2:
			return TICKERANDFRAME;
		case 3:
			return FRAMESNOTICKER;
		case 4:
			return FRAMENOTICKER;
		case 5:
			return FERMITICKER;
		default:
			return NONE;
		}
	}

	/**
	 * The URL for the local Fermilab news feed.
	 */
	public static final String	FERMI_NEWS_TICKER		= "newsfeed/breakingnews.txt";

	/**
	 * The URL for the news feed from science.org
	 */
	public static final String	SCIENCE_NEWS_TICKER		= "newsfeed/science.txt";

	/**
	 * The URL for the news feed from Symmetry Magazine
	 */
	public static final String	SYMMETRY_NEWS_TICKER	= "newsfeed/symmetry.txt";

	private String				tickerName				= SCIENCE_NEWS_TICKER;

	/**
	 * @param val
	 * @return if this enum value corresponds to the argument
	 */
	public boolean equals(final int val) {
		switch (val) {
		case 0:
			return this == NORMAL;
		case 1:
			return this == TICKER;
		case 2:
			return this == TICKERANDFRAME;
		case 3:
			return this == FRAMESNOTICKER;
		case 4:
			return this == FRAMENOTICKER;
		case 5:
			return this == FERMITICKER;
		case 6:
			return this == NONE;
		default:
			return false;
		}
	}

	/**
	 * @return the name of the file used to create the ticker/news crawl
	 */
	public String getTickerName() {
		switch (this) {
		case NORMAL:
			return "";
			
		case FERMITICKER:
			return FERMI_NEWS_TICKER;
			
		default:
			return tickerName;
		}
	}

	/**
	 * @param tickerName
	 *            The new name for the ticker/news crawl
	 */
	public void setTickerName(final String tickerName) {
		this.tickerName = tickerName;
	}
}