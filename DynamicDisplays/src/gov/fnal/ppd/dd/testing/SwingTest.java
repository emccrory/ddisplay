package gov.fnal.ppd.dd.testing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A simple class that tests if the PC/display being used can actually handle Java/Swing graphics
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2013.
 * 
 */
public class SwingTest {
	static int	count	= 0;

	/**
	 * @param args none expected
	 */
	public static void main(final String[] args) {
		JFrame frame = new JFrame("Java/Swing Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel panel = new JPanel(new BorderLayout());
		final JLabel label = new JLabel("empty");
		JButton button = new JButton("Press me!");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				label.setText(++count + " presses");
			}
		});
		panel.add(button, BorderLayout.CENTER);

		panel.add(label, BorderLayout.SOUTH);

		frame.add(panel, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);

	}
}
