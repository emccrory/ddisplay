package gov.fnal.ppd;

import static gov.fnal.ppd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.GlobalVariables.lastDisplayChange;
import static gov.fnal.ppd.signage.util.Util.catchSleep;

import java.awt.Font;

import javax.swing.JLabel;

/**
 * Extend a JLabel to give the user some basic guidance on the use of the full-screen App.
 * 
 * TODO: Be sensitive to when the user successfully utilizes the App and reduce my size (or maybe make invisible). Then, after some
 * time of quiet (15 minutes?), go back to full-on, in-your-face presentation for the next user.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class SelectorInstructions extends JLabel {

	private static final long	serialVersionUID	= 4509134236620234614L;
	private static final String	INSTRUCTIONS_TEXT	= "<html><center>This App allows you to change the channel, that is, the visible content, "
															+ "on any of the HDTV Displays in the room.<br>"
															+ "Select the Display by touching one of the colored \"Display\" "
															+ "buttons on the right side of this screen.<br>"
															+ "Then you can change the channel using the buttons in the main part of the App, above</html>";
	private Font				smallFont;
	private Font				largeFont;
	private boolean				isLargeFont			= true;

	/**
	 * Construct an instance of the Special Instructions label
	 */
	public SelectorInstructions() {
		super(INSTRUCTIONS_TEXT, JLabel.CENTER);
		largeFont = getFont().deriveFont(20.0f);
		smallFont = getFont().deriveFont(8.0f);
		setFont(largeFont);
	}

	/**
	 * Call this method after the constructor has finished, please.
	 */
	public void start() {
		new Thread(this.getClass().getSimpleName() + "_heartbeat") {
			public void run() {
				long sleepTime = 30000L;
				while (true) {
					catchSleep(sleepTime);
					sleepTime = 2000L;
					checkChange();
				}
			}
		}.start();
	}

	private void checkChange() {
		if (!isLargeFont && (lastDisplayChange + FIFTEEN_MINUTES) < System.currentTimeMillis()) {
			// make font big again
			setText(INSTRUCTIONS_TEXT);
			setFont(largeFont);
			repaint();
			isLargeFont = true;
		} else if (isLargeFont && (lastDisplayChange + 10 * ONE_SECOND) > System.currentTimeMillis()) {
			// Make font small when the user does something right. (Maybe even make this go away--not sure yet)
			setText(INSTRUCTIONS_TEXT.replace("<br><br>", "<br>"));
			setFont(smallFont);
			repaint();
			isLargeFont = false;
		}
	}
}
