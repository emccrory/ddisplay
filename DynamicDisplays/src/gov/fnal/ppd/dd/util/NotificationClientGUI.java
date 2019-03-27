package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;

import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;

/** Isolate the GUI stuff that some clients need to have during an update */
public class NotificationClientGUI implements NotificationClient {

	private static class LocalJLabel extends JLabel {
		private static final long serialVersionUID = -6659598577980767667L;

		public LocalJLabel(String title, int style, int size) {
			super(title);
			setAlignmentX(JComponent.CENTER_ALIGNMENT);
			setFont(new Font("Arial", style, size));
		}
	}

	private JFrame frame;

	@Override
	public void success() {
		frame.invalidate();
		frame.dispose();
		JLabel big = new JLabel("Update succeeded.");
		big.setFont(new Font("Arial", Font.ITALIC, 32));
		frame.setContentPane(big);
		frame.validate();
	}

	@Override
	public void showNewUpdateInformation(FLAVOR flavor, String versionString) {
		// Unfortunately, this will fail if the version of Java has changed since the JVM started (it cannot instantiate JFrame).
		String message0 = "Dynamic Displays Software Update in progress";
		String message1 = "There is a new " + flavor + " version, " + versionString + ", of the Dynamic Displays software.";
		String message2 = "After the software has been updated, the application program will be restarted.";
		println(getClass(), message0 + "\n" + message1 + "\n" + message2);

		Dimension bigGap = new Dimension(60, 60);

		frame = new JFrame("Dynamic Displays: New Software Update");
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		Box p = Box.createVerticalBox();
		JLabel big = new LocalJLabel(message0, Font.BOLD, 46);
		p.add(big);

		big = new LocalJLabel(message1, Font.PLAIN, 32);
		p.add(Box.createRigidArea(bigGap));
		p.add(big);

		big = new LocalJLabel(message2, Font.ITALIC, 32);
		p.add(Box.createRigidArea(bigGap));
		p.add(big);

		final JLabel countdown = new LocalJLabel("This will take about 5 minutes, " + new Date(), Font.ITALIC, 18);
		p.add(Box.createRigidArea(bigGap));
		p.add(countdown);

		new Thread("Countdown") {
			// Some eye candy for the information window
			int		c			= 570;
			long	sleepTime	= 30000;

			@Override
			public void run() {
				while (true) {
					catchSleep(sleepTime);
					c--;
					if (c > 60)
						countdown.setText("This should take about " + (c + 20) / 60 + " more minutes, " + new Date());
					else if (c < -60)
						countdown.setText("Should be done already! " + new Date());
					else if (c < 60)
						countdown.setText("Should be done in a moment, " + new Date());
					sleepTime = 1000;
				}
			}

		}.start();

		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30),
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.red.darker(), 20),
						BorderFactory.createEmptyBorder(10, 10, 10, 10))));

		frame.setContentPane(p);
		frame.pack();
		frame.setAlwaysOnTop(true);
		frame.setVisible(true);

	}

	public void failure() {
		frame.invalidate();
		frame.dispose();
		JLabel big = new JLabel("Something went wrong with the update - Call for help.");
		big.setFont(new Font("Arial", Font.ITALIC, 32));
		frame.setContentPane(big);
		frame.validate();
	}

}
