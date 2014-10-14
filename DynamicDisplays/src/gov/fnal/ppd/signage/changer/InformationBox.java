package gov.fnal.ppd.signage.changer;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.Border;

/**
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class InformationBox extends JFrame {

	private static final long	serialVersionUID	= 4866012215771895245L;

	/**
	 * A simple extension of JLabel to assist in the proper setup of the fonts and stuff
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * @copyright 2014
	 * 
	 */
	private static class MyLabel extends JLabel {

		private static final long	serialVersionUID	= 5368553680158650696L;

		MyLabel(String text, float size) {
			super(text, JLabel.CENTER);
			setFont(getFont().deriveFont(size));
			setAlignmentX(CENTER_ALIGNMENT);
		}
	}

	/**
	 * 
	 * @param comp
	 *            The parent component for this dialog box
	 * @param message1
	 *            The headline message
	 * @param message2
	 *            The sub message
	 * @param di
	 *            The Display for which we are showing the dialog box
	 */
	public InformationBox(final JComponent comp, String message1, String message2) {
		super("URL Refresh Action");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Box h = Box.createVerticalBox();
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		h.add(new MyLabel("  ***** " + message1.toUpperCase() + " *****  ", 30.0f));
		h.add(Box.createRigidArea(new Dimension(10, 10)));
		h.add(new MyLabel(message2, 24.0f));
		h.add(Box.createRigidArea(new Dimension(10, 10)));

		final MyLabel timeLeft = new MyLabel("This will disappear in 10 seconds", 10.0f);
		h.add(timeLeft);
		h.add(Box.createRigidArea(new Dimension(10, 10)));

		Border b0 = BorderFactory.createLineBorder(Color.black);
		Border b1 = BorderFactory.createLineBorder(Color.white, 15);
		Border b2 = BorderFactory.createLineBorder(Color.black, 20);
		Border b3 = BorderFactory.createEmptyBorder(50, 50, 50, 50);
		Border b4 = BorderFactory.createCompoundBorder(b0, b1);
		Border b5 = BorderFactory.createCompoundBorder(b4, b2);
		h.setBorder(BorderFactory.createCompoundBorder(b5, b3));

		setContentPane(h);
		setUndecorated(true);
		pack();
		Dimension size = comp.getSize();
		setLocation(comp.getLocation().x + size.width / 5, comp.getLocation().y + size.height / 3);

		setVisible(true);
		setAlwaysOnTop(true);

		new Thread(this.getClass().getSimpleName() + "_Removal") {
			public void run() {
				try {
					for (int i = 10; i > 0; i--) {
						timeLeft.setText("This will disappear in " + i + " seconds");
						sleep(1000L);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				InformationBox.this.dispose();
			}
		}.start();
	}
}
