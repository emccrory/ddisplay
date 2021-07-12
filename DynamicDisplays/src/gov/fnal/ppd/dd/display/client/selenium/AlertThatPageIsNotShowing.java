package gov.fnal.ppd.dd.display.client.selenium;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import gov.fnal.ppd.dd.display.client.BrowserErrorListener;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.specific.TemporaryDialogBox;

/**
 * The implementation of the BrowserErrorListener, which pops up a temporary error dialog when there is a problem.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class AlertThatPageIsNotShowing implements BrowserErrorListener {
	private Display			display;
	private static boolean	isShowing	= false;

	public AlertThatPageIsNotShowing(final Display d) {
		display = d;
	}

	@Override
	public void errorInPageLoad(final int value) {
		println(AlertThatPageIsNotShowing.class, "errorInPageLoad called");

		if (!isShowing) {
			isShowing = true;
			new Thread("LaunchAlertThatPageIsNotShowing") {
				public void run() {

					String headline = "Dynamic Displays: Web page load error";
					String line2 = "Error code = " + value;
					String line3 = "This is an unanticipated error code. Action: Refresh page";

					SignageContent c = display.getContent();
					if (c instanceof Channel) {
						line2 += ", Offending URL=[" + c.getURI().toString() + "]";
					} else {
						line2 += ", Content is " + c;
					}
					switch (value) {
					case 403:
						line3 = "This page has been set so that the Dynamic Displays are not allowed to see it. Contact the Web Admin for that page";
						break;

					case 404:
						line3 = "This is a 'File Not Found' error.  Have a Dynamic Displays expert double check the URL for this channel.";
						break;

					case 408:
						line3 = "This is a timeouot error; auto refresh should fix it ... eventually";
						break;

					default:
						line3 = value + " is an unknown error.";
						break;
					}

					int timeout = 12000;

					new TemporaryDialogBox(null, "Web error " + value + " captured", timeout / 1000, headline, line2, line3);
					catchSleep(timeout);
					isShowing = false;
				}
			}.start();
		} else {
			println(AlertThatPageIsNotShowing.class, "Multiple requests to show launch dialog overlapped");
		}
		println(AlertThatPageIsNotShowing.class, "errorInPageLoad returned");
	}
}
