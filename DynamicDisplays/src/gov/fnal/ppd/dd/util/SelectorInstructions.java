/*
 * SelectorInstructions
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.FIFTEEN_MINUTES;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.dd.GlobalVariables.timeSinceLastUserActivity;
import static gov.fnal.ppd.dd.util.Util.catchSleep;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Extend a JLabel to give the user some basic guidance on the use of the full-screen App.
 * 
 * TODO: Be sensitive to when the user successfully utilizes the App and reduce my size (or maybe make invisible). Then, after some
 * time of quiet (15 minutes?), go back to full-on, in-your-face presentation for the next user.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class SelectorInstructions extends JPanel {

	private static final long	serialVersionUID	= 4509134236620234614L;
	private static final String	INSTRUCTIONS_TEXT	= "<html><center>This App allows you to change the channel, that is, the visible content, "
			+ "on any of the HDTV Displays in the room.<br>" + "Select the Display by touching one of the colored \"Display\" "
			+ "buttons on the right side of this screen.<br>"
			+ "Then you can change the channel using the buttons in the main part of the App "
			+ "on the main part of the screen (to the left).</html>";

	private Font				smallFont;
	private Font				largeFont;
	private boolean				isLargeFont			= true;
	private JLabel				lab					= new JLabel(INSTRUCTIONS_TEXT, JLabel.CENTER);

	/**
	 * Construct an instance of the Special Instructions label
	 */
	public SelectorInstructions() {
		super();
		largeFont = lab.getFont().deriveFont(20.0f);
		smallFont = lab.getFont().deriveFont(8.0f);
		lab.setFont(largeFont);
		add(lab);
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
		if (!isLargeFont && timeSinceLastUserActivity() > FIFTEEN_MINUTES) {
			// make font big again
			lab.setText(INSTRUCTIONS_TEXT);
			lab.setFont(largeFont);
			repaint();
			isLargeFont = true;
		} else if (isLargeFont && timeSinceLastUserActivity() > 10 * ONE_SECOND) {
			// Make font small when the user does something right. It will become large again in 15 minutes
			lab.setText(INSTRUCTIONS_TEXT.replace("<br><br>", "<br>"));
			lab.setFont(smallFont);
			repaint();
			isLargeFont = false;
		}
	}

	public static String getInstructions() {
		return INSTRUCTIONS_TEXT;
	}
}
