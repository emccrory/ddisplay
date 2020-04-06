package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.WEB_PROTOCOL;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.isThisURLNeedAnimation;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import gov.fnal.ppd.dd.display.ScreenLayoutInterpreter;
import gov.fnal.ppd.dd.emergency.EmergencyMessage;
import gov.fnal.ppd.dd.emergency.Severity;
import gov.fnal.ppd.dd.util.nonguiUtils.ColorNames;

/**
 * <p>
 * Abstract class to handle most of the functions necessary to communicate our intentions to the browser. There are some actions
 * that are directly dependent on how the communications happens, and those actions are in the concrete classes.
 * </p>
 * 
 * <p>
 * Concrete sub-classes are
 * <ul>
 * <li>SeleniumConnectionToBrowser - Assumes the Selenium framework. Any browser can be used, but Firefox is the one we have tested
 * </li>
 * <li>SimplifiedBrowserConnection - The connection to the (generic) browser is by running System.exec().</li>
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
	// private static final String FERMI_TICKERTAPE_WEB_PAGE = "http://" + WEB_SERVER_NAME + "/border5.php";
	// protected static final String WEB_PAGE_EMERGENCY_FRAME = "http://" + WEB_SERVER_NAME + "/border8.php";

	protected static final String		TICKERTAPE_WEB_PAGE				= WEB_PROTOCOL + "://" + WEB_SERVER_NAME + "/border6.php";
	protected static final String		WEB_PAGE_EMERGENCY_FRAME		= WEB_PROTOCOL + "://" + WEB_SERVER_NAME + "/borderA.php";

	protected static ColorNames			colorNames						= new ColorNames();
	protected static int				numberOfScreens					= 0;

	protected boolean					debug							= true;
	protected String					colorCode;
	protected Rectangle					bounds;
	protected int						virtualID, dbID;
	protected boolean					badNUC							= false;
	protected AtomicBoolean				showingCanonicalSite			= new AtomicBoolean(true);
	protected boolean					connected						= false;
	protected boolean					showNumber						= true;
	protected boolean					showingEmergencyCommunication	= false;
	protected int						screenNumber;
	private List<BrowserErrorListener>	listeners						= new ArrayList<BrowserErrorListener>();

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
		// TimerTask tt = new TimerTask() {
		// public void run() {
		// try {
		// // The next time there is a channel change (even for a refresh), it will do a full reset.
		// showingCanonicalSite.set(false);
		// println(ConnectionToFirefoxInstance.class, ": next refresh of URL will be a FULL reset");
		// } catch (Exception e) {
		// println(ConnectionToFirefoxInstance.class, "Exception caught in diagnostic thread, " + e.getLocalizedMessage());
		// e.printStackTrace();
		// }
		// }
		// };
		//
		// Timer timer = new Timer();
		// timer.scheduleAtFixedRate(tt, ONE_HOUR, ONE_HOUR);
		// In the Selenium framework, sending a new, full URL causes it to come out of full-screen mode.

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
	 * Ask the browser to reload the web page it is showing right now.
	 * 
	 * Note that since we are pretty much always showing a web page within a frame, we don't want to refresh the web page, but
	 * rather the frame (which is NOT what is happening here)
	 * 
	 */
	public void forceRefresh() {
		// Send the refresh
		String s = "window.location.reload(true);";
		send(s);
	}

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
	public abstract String getConnectionCode();

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
	 * @param specialCode
	 *            The code, from the database, for this URL (e.g., has sound)
	 * @return Was the change successful?
	 * @throws UnsupportedEncodingException
	 *             -- if the url we have been given is bogus
	 */
	public boolean changeURL(String urlStrg, int specialCode) throws UnsupportedEncodingException {

		String urlString = urlStrg;
		if (badNUC && isThisURLNeedAnimation(urlStrg)) {
			urlString = urlStrg + "&zoom=0";
		}
		if (debug)
			println(getClass(), getScreenText() + " New URL: " + urlString + ", specialCode=" + specialCode);

		if (showingEmergencyCommunication) {
			removeEmergencyCommunication();
		}

		String frameName = "iframe";

		String javascriptCommandToBrowser = "document.getElementById('" + frameName + "').src = '" + urlString + "';\n";

		// "FastBugs" frowns on using an internally-synchronized object for synchronization. The intent is to prevent
		// multiple instances of this method from running at the same time, so we put the "synchronized" on the method, where it
		// should be.
		// synchronized (showingCanonicalSite) {

		switch (specialCode) {
		case 2:
			// There is a class of URLs that will not let us put their content into our border frame web site. That is, the site
			// we want to show is non-secured (http), but the border page is secured (https). Firefox gives the error, "does not
			// permit cross-origin framing".

			// The first site I have encountered for this is the site that shows a Google Sheets presentation. The
			// work-around is to abandon this border web page for these sites (only).

			// This breaks the "identify" functionality, and the full-screen action.

			println(getClass(), "OVERRIDING normal container web page for this URL [" + urlString + "]");
			javascriptCommandToBrowser = "window.location=\"" + urlString + "\";\n";
			showingCanonicalSite.set(false);
			break;

		case 3:
			// Nothing is defined with this code. But I put this case here so I could write this comment.
		case 1:
			// TODO - Reserved code indicating that this page has sound. It is presumed that there are some locations for which
			// sound is a no-no. But this has not been requested. For now, fall through to the default case
		case 0:
			if (!showingCanonicalSite.get()) {
				// There is nothing to do here if the "border" page is already showing. Otherwise, show it!
				println(getClass(), getScreenText() + " Sending full, new URL to browser, " + WEB_PAGE_EMERGENCY_FRAME);
				showingCanonicalSite.set(true);
				javascriptCommandToBrowser = "window.location=\"" + WEB_PAGE_EMERGENCY_FRAME + "?url="
						+ URLEncoder.encode(urlString, "UTF-8") + "&display=" + virtualID + "&color=" + colorCode + "&width="
						+ bounds.width + "&height=" + bounds.height;

				if (!showNumber)
					javascriptCommandToBrowser += "&shownumber=0";
				javascriptCommandToBrowser += "\";\n";

				println(getClass(), "Resetting the url with this command: [" + javascriptCommandToBrowser + "]");

				// TODO - Figure this out!
				// In the Selenium framework, sending a new URL to the browser (which we do from time to time) takes it out of
				// full-screen mode. The work-around would be to call the class that assures the browser is full screen. but
				// then there would be a visible glitch in the visuals, so this is not acceptable. Maybe, then, only the
				// "refresh" from the user will send the URL all over again (which drops it out of full screen) and then quickly
				// make it full screen again.
			}
		}

		// ********* Here is where the JavaScript message is sent to FireFox, containing the new URL *************************

		send(javascriptCommandToBrowser); // **********************************************************************************************************

		// *******************************************************************************************************************

		return connected; // FIXME - Is this exactly right?
	}

	protected String getScreenText() {
		return " (Screen " + screenNumber + ")";
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

		// s += "document.getElementById('iframe').style.width=" + (bounds.width - 220) + ";\n";
		// s += "document.getElementById('iframe').style.height=" + (bounds.height - 208) + ";\n";

		s += "document.getElementById('iframe').style.width='89%';\n";
		s += "document.getElementById('iframe').style.height='81%';\n";

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
		// s += "document.getElementById('iframe').style.width=" + (bounds.width - 4) + ";\n";
		// s += "document.getElementById('iframe').style.height=" + (bounds.height - 6) + ";\n";

		s += "document.getElementById('iframe').style.width='99.8%';\n";
		s += "document.getElementById('iframe').style.height='99.5%';\n";

		s += "document.getElementById('colorName').innerHTML = '';\n";

		send(s);
	}

	/*
	 * Note: This concept is not supported. The functionality has been removed
	 * 
	 * @param frameNumber the frame number to show in the HTML file
	 */
	// public void showFrame(int frameNumber) {
	// String s = "document.getElementById('frame" + frameNumber + "').style.visibility='visible';\n";
	// send(s);
	// }

	/*
	 * Note: This concept is not supported. The functionality has been removed
	 * 
	 * @param frameNumber the frame number to hide in the HTML file
	 */
	// public void hideFrame(int frameNumber) {
	// String s = "document.getElementById('frame" + frameNumber + "').style.visibility='hidden';\n";
	// send(s);
	// }

	/**
	 * Note: This concept is not supported. The functionality has been removed
	 * 
	 * @param frameNumber
	 *            The frame number to turn off
	 */

	// public void turnOffFrame(final int frameNumber) {
	// String s = "document.getElementById('frame" + frameNumber + "').style.visibility='hidden';\n";
	// send(s);
	// }

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

	public void addErrorListener(BrowserErrorListener listener) {
		if (listener != null)
			listeners.add(listener);
	}

	protected void errorSeen(long value) {
		for (BrowserErrorListener L : listeners)
			L.errorInPageLoad((int) value);
	}

	public boolean isShowingSpecialURL() {
		return !showingCanonicalSite.get();
	}
}