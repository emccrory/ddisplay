package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.isThisURLNeedAnimation;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import gov.fnal.ppd.dd.display.ScreenLayoutInterpreter;
import gov.fnal.ppd.dd.emergency.EmergencyMessage;
import gov.fnal.ppd.dd.emergency.Severity;
import gov.fnal.ppd.dd.util.ColorNames;

/**
 * <p>
 * Abstract class to handle most of the functions necessary to communicate our intentions to the browser. There are some actions
 * that are directly dependent on how the communications happens, and those actions are in the concrete classes.
 * </p>
 * 
 * </P>
 * In June, 2018, we are transitioning to using Selenium, away from using the "pmorch" plugin to Firefox.
 * </p>
 * 
 * <p>
 * Concrete sub-classes are
 * <ul>
 * <li>ConnectionToFirefoxInstance - Assumes Firefox browser and the pmorch plugin</li>
 * <li>SeleniumConnectionToBrowser - Assumes the Selenium framework. Any browser can be used, but Firefox is the one we have tested
 * </li>
 * <ul>
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public abstract class ConnectionToBrowserInstance {

	// private static final String BASE_WEB_PAGE = "http://" + WEB_SERVER_NAME + "/border.php";
	// private static final String EXTRAFRAME_WEB_PAGE = "http://" + WEB_SERVER_NAME + "/border2.php";
	// private static final String EXTRAFRAMESNOTICKER_WEB_PAGE = "http://" + WEB_SERVER_NAME + "/border3.php";
	// private static final String EXTRAFRAMENOTICKER_WEB_PAGE = "http://" + WEB_SERVER_NAME + "/border4.php";

	protected static final String	TICKERTAPE_WEB_PAGE				= "http://" + WEB_SERVER_NAME + "/border6.php";
	protected static final String	WEB_PAGE_EMERGENCY_FRAME		= "http://" + WEB_SERVER_NAME + "/border7.php";

	// private static final String FERMI_TICKERTAPE_WEB_PAGE = "http://" + WEB_SERVER_NAME + "/border5.php";

	protected static ColorNames		colorNames						= new ColorNames();
	protected static int			numberOfScreens					= 0;

	protected boolean				debug							= true;
	protected String				colorCode;
	protected Rectangle				bounds;
	protected int					virtualID, dbID;
	protected boolean				badNUC							= false;
	protected AtomicBoolean			showingCanonicalSite			= new AtomicBoolean(false);
	protected boolean				connected						= false;
	protected boolean				showNumber						= true;
	protected String				instance						= "";
	protected boolean				showingEmergencyCommunication	= false;
	protected int					screenNumber;

	/**
	 * @param screenNumber
	 * @param virtualID
	 * @param dbID
	 * @param color
	 * @param showNumber
	 */
	public ConnectionToBrowserInstance(final int screenNumber, final int virtualID, final int dbID, final Color color,
			final boolean showNumber) {
		// Create a connection to the instance of FireFox that is being targeted here

		bounds = ScreenLayoutInterpreter.getBounds(screenNumber);
		println(getClass(),
				" -- Screen size is " + bounds.width + " x " + bounds.height + ", vDisplay=" + virtualID + ", dbID=" + dbID);

		this.virtualID = virtualID;
		this.dbID = dbID;
		this.showNumber = showNumber;
		this.screenNumber = screenNumber;
		numberOfScreens++;

		colorCode = Integer.toHexString(color.getRGB() & 0x00ffffff);
		colorCode = "000000".substring(colorCode.length()) + colorCode;

		// Start a couple of daemon threads --------------------------------
		
		// Perform a full reset of the browser every now and then.
		TimerTask tt = new TimerTask() {
			public void run() {
				try {
					// The next time there is a channel change (even for a refresh), it will do a full reset.
					showingCanonicalSite.set(false);
					println(ConnectionToFirefoxInstance.class, ": next refresh of URL will be a FULL reset");
				} catch (Exception e) {
					println(ConnectionToFirefoxInstance.class, "Exception caught in diagnostic thread, " + e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		};

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(tt, ONE_HOUR, ONE_HOUR);

		instance = " (Screen " + numberOfScreens + ")";
		if (numberOfScreens == 1) {
			timer = new Timer();  // Not sure if we really need to make another one
			TimerTask g = new TimerTask() {
				public void run() {
					for (int i = 0; i < 100; i++) {
						if (numberOfScreens > 1) {
							instance = " (Screen " + numberOfScreens + ")";
							return;
						}
						// It often takes a moment for the second screen to appear. This loop waits a total of 20.5 seconds.
						catchSleep(200);
					}
				}
			};
			timer.schedule(g, 500); // Run it once, after a short delay - it will loop for a moment
		}

		// Make sure we exit the instance(s) of the browser when the VM exits ---------------------------
		Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook_Display_" + numberOfScreens) {
			public void run() {
				exit();
			}
		});

	}

	/// For testing only.
	protected ConnectionToBrowserInstance() {
		virtualID = dbID = screenNumber = 0;
		showNumber = true;
		colorCode = "ff0000";
	}

	// ------------------- The abstract methods ----------------------
	/**
	 * Establish communications with the browser
	 */
	public abstract void openConnection();

	/**
	 * Ask the browser to completely refresh itself
	 * 
	 * @param frameNumber
	 */
	public abstract void forceRefresh(int frameNumber);

	/**
	 * Put the browser window into the right place
	 */
	protected abstract void setPositionAndSize();

	/**
	 * Remove the browser window
	 */
	public abstract void exit();

	/**
	 * @return The last reply from the connection to the browser
	 */
	public abstract Object getTheReply();

	/**
	 * @return a string that tells which connection instance this is (e.g., " (port #32000)")
	 */
	public abstract String getInstance();

	/**
	 * Send JavaScript commands to the browser
	 * 
	 * @param javaScriptCommands
	 */
	public abstract void send(String javaScriptCommands);

	// ------------------- End of abstract methods ----------------------

	/**
	 * Change the URL that is showing now.
	 * 
	 * @param urlStrg
	 *            The URL that this instance should show now.
	 * @param theWrapper
	 *            What kind of wrapper page shall this be? Normal, ticker-tape or none ("none" is really not going to work)
	 * @param frameNumber
	 *            The frame number to which this content is targeted
	 * @param specialCode
	 *            The code, from the database, for this URL (e.g., has sound)
	 * @return Was the change successful?
	 * @throws UnsupportedEncodingException
	 *             -- if the url we have been given is bogus
	 */
	public boolean changeURL(String urlStrg, WrapperType theWrapper, int frameNumber, int specialCode)
			throws UnsupportedEncodingException {

		String urlString = urlStrg;
		if (badNUC && isThisURLNeedAnimation(urlStrg)) {
			urlString = urlStrg + "&zoom=0";
		}
		if (debug)
			if (frameNumber == 0)
				println(getClass(), instance + " New URL: " + urlString);
			else
				println(getClass(), instance + " New URL for frame number " + frameNumber + ": " + urlString);

		if (showingEmergencyCommunication) {
			removeEmergencyCommunication();
		}

		String frameName = "iframe";
		if (frameNumber > 0)
			frameName = "frame" + frameNumber;

		String s = "document.getElementById('" + frameName + "').src = '" + urlString + "';\n";
		if (frameNumber > 0)
			s += "document.getElementById('" + frameName + "').style.visibility='visible';\n";

		// "FastBugs" frowns on using an internally-synchronized object for synchronization. The intent is to prevent
		// multiple instances of this method from running at the same time, so we put the "synchronized" on the method, where it
		// should be.
		// synchronized (showingCanonicalSite) {
		switch (theWrapper) {

		case NORMAL:
			if ((specialCode | 1) != 0) {
				// TODO - This URL has sound. Hmmm.
			}
			if ((specialCode | 2) != 0) {
				// TODO - This URL cannot be used in our border frame URL. Hmmm.
			}
			if (!showingCanonicalSite.get()) {

				// FIXME - There is a class of URLs that will not let us put their content into our border frame
				// web site. They are giving an error, "does not permit cross-origin framing".

				// The first site I have encountered for this is the site that shows a Google Sheets presentation. The work-around,
				// I think, will be to abandon this border web page for these sites (only). This will break the fancy, beautiful
				// "identify" functionality, but I can probably figure out a work-around for this, too.

				// The solution is (probably) to flag a web page in the database as having this restriction, and then getting that
				// flag down to here, where this operation can then send just the URL, without the border stuff. There is a field
				// in the Channel table currently called "Sound". This can be changed to "Flags" and then we can put all kinds of
				// stuff like that in there ("Does the page have sound?" "Is this a non-cross-origin web page?" Etc.) There is
				// also a field in the XML transmission called <code>. That flag can go there.

				println(getClass(), instance + " Sending full, new URL to browser, " + WEB_PAGE_EMERGENCY_FRAME);
				showingCanonicalSite.set(true);
				s = "window.location=\"" + WEB_PAGE_EMERGENCY_FRAME + "?url=" + URLEncoder.encode(urlString, "UTF-8") + "&display="
						+ virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height;

				if (!showNumber)
					s += "&shownumber=0";
				s += "\";\n";
			}
			break;

		case TICKER:
			// case FERMITICKER:
			if (!showingCanonicalSite.get()) {
				println(getClass(), instance + " Sending full, new URL to browser " + TICKERTAPE_WEB_PAGE);
				showingCanonicalSite.set(true);
				s = "window.location=\"" + TICKERTAPE_WEB_PAGE + "?url=" + URLEncoder.encode(urlString, "UTF-8") + "&display="
						+ virtualID + "&color=" + colorCode + "&width=" + bounds.width + "&height=" + bounds.height + "&feed="
						+ theWrapper.getTickerName();

				if (!showNumber)
					s += "&shownumber=0";
				s += "\";\n";
			}
			break;

		}

		// ********* Here is where the JavaScript message is sent to FireFox, containing the new URL *************************

		send(s); // **********************************************************************************************************

		// *******************************************************************************************************************

		return connected; // FIXME - Is this exactly right?
	}

	/**
	 * Modify the actual display to self-identify, showing the Display number and the highlight color.
	 */
	public void showIdentity() {
		String s = "document.getElementById('numeral').style.opacity=0.8;\n";
		s += "document.getElementById('numeral').style.font='bold 750px sans-serif';\n";
		s += "document.getElementById('numeral').style.textShadow='0 0 18px black, 0 0 18px black, 0 0 18px black, 0 0 18px black, 0 0 18px black, 16px 16px 2px #"
				+ colorCode + "';\n";
		s += "document.getElementsByTagName('body')[0].setAttribute('style', 'background-color: #" + colorCode
				+ "; padding:0; margin: 100;' );\n";
		// s += "document.getElementById('iframe').style.width=1700;\n";
		// s += "document.getElementById('iframe').style.height=872;\n";
		s += "document.getElementById('iframe').style.width=" + (bounds.width - 220) + ";\n";
		s += "document.getElementById('iframe').style.height=" + (bounds.height - 208) + ";\n";

		String displayID = "(" + dbID + ") ";
		if (this.dbID == this.virtualID)
			displayID = "";
		if (colorNames.containsKey(colorCode))
			s += "document.getElementById('colorName').innerHTML = '" + displayID + colorNames.get(colorCode) + "';\n";
		else
			s += "document.getElementById('colorName').innerHTML = '" + displayID + "#" + colorCode + "';\n";

		send(s);

		// TODO -- Add the PNG image of the QR code here. Generate it with this:
		// try {
		// BufferedImage image = gov.fnal.ppd.dd.util.CrunchifyQRCode.createQRCodeImage(lastURLString);
		// } catch (WriterException e) {
		// e.printStackTrace();
		// }
	}

	/**
	 * Remove the self-identify dressings on the Display
	 */
	public void removeIdentify() {
		String s = "document.getElementById('numeral').style.opacity=" + ((!showNumber) ? "0.0" : "0.4") + ";\n";
		s += "document.getElementById('numeral').style.font='bold 120px sans-serif';\n";
		s += "document.getElementsByTagName('body')[0].setAttribute('style', 'padding:0; margin: 0;');\n";
		s += "document.getElementById('numeral').style.textShadow='0 0 4px black, 0 0 4px black, 0 0 4px black, 0 0 4px black, 0 0 4px black, 6px 6px 2px #"
				+ colorCode + "';\n";
		s += "document.getElementById('iframe').style.width=" + (bounds.width - 4) + ";\n";
		s += "document.getElementById('iframe').style.height=" + (bounds.height - 6) + ";\n";

		s += "document.getElementById('colorName').innerHTML = '';\n";

		send(s);
	}

	/**
	 * Note: Frames are not fully workable at this time. It seems like a good idea, but there is no real call for it.
	 * 
	 * @param frameNumber
	 *            the frame number to show in the HTML file
	 */
	public void showFrame(int frameNumber) {
		String s = "document.getElementById('frame" + frameNumber + "').style.visibility='visible';\n";
		send(s);
	}

	/**
	 * @param frameNumber
	 *            the frame number to hide in the HTML file
	 */
	public void hideFrame(int frameNumber) {
		String s = "document.getElementById('frame" + frameNumber + "').style.visibility='hidden';\n";
		send(s);
	}

	/**
	 * @param frameNumber
	 *            The frame number to turn off
	 */

	public void turnOffFrame(final int frameNumber) {
		String s = "document.getElementById('frame" + frameNumber + "').style.visibility='hidden';\n";
		send(s);
	}

	/**
	 * @return Best guess as to whether or not we are connected to the FireFox back end
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Force to refresh the controlling URL.
	 */
	public void resetURL() {
		showingCanonicalSite.set(false);
	}

	/**
	 * Put an emergency message onto the screen!
	 * 
	 * @param message
	 *            -- The message, which is (presumably) formatted HTML
	 * 
	 * @return Did it work?
	 */
	public boolean showEmergencyCommunication(EmergencyMessage message) {
		if (message.getSeverity() == Severity.REMOVE)
			return removeEmergencyCommunication();

		String mess = message.toHTML() + "<p class='date'>Message was last updated at this time: " + new Date() + "</p>";

		Color borderColor = null;
		switch (message.getSeverity()) {
		case ALERT:
			borderColor = Color.RED.darker();
			break;

		case INFORMATION:
			borderColor = Color.GREEN.darker();
			break;

		case TESTING:
			borderColor = Color.GRAY;
			break;

		case EMERGENCY:
			borderColor = Color.RED;
			break;

		case REMOVE:
			// Already handled above
			break;
		}

		String s = "";
		if (borderColor != null) {
			String rgb = getHex(borderColor.getRed()) + getHex(borderColor.getGreen()) + getHex(borderColor.getBlue());
			s = "document.getElementById('emergencyframe1').style.borderColor = '#" + rgb + "';\n";
		}

		s += "document.getElementById('emergencyframe1').innerHTML='" + mess.replace("'", "\\'") + "';\n";
		s += "document.getElementById('emergencyframe1').style.visibility='visible';\n";

		send(s);
		showingEmergencyCommunication = true;
		return connected;
	}

	/**
	 * @return if this was successful
	 */
	public boolean removeEmergencyCommunication() {
		String s = "document.getElementById('emergencyframe1').innerHTML='';\n";
		s += "document.getElementById('emergencyframe1').style.visibility='hidden';\n";

		send(s);
		showingEmergencyCommunication = false;

		return connected;
	}

	private String getHex(int val) {
		String retval = Integer.toHexString(0x000000ff & val);
		if (val < 16)
			return "0" + retval;
		return retval;
	}

	/**
	 * @param badNUC
	 *            Is this display controlled by a "Bad NUC" (which has trouble dealing the the "Ken Burns" slide shows)?
	 */
	public void setBadNUC(final boolean badNUC) {
		this.badNUC = badNUC;
	}

}