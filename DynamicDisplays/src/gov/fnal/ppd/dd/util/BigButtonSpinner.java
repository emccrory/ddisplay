package gov.fnal.ppd.dd.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicSpinnerUI;

public class BigButtonSpinner {
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				SpinnerNumberModel model = new SpinnerNumberModel(10, 0, 1000, 5);

				JPanel p = new JPanel();

				p.add(create(model));
				p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				JFrame frame = new JFrame();
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				frame.getContentPane().add(p);
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);

			}
		});
	}

	public static JSpinner create(SpinnerModel model) {

		JSpinner spinner = new JSpinner(model);
		spinner.setFont(new Font("Monospace", Font.PLAIN, 80));

		spinner.setUI(new BasicSpinnerUI() {
			@Override
			protected Component createPreviousButton() {
				Component b = super.createPreviousButton();
				JPanel wrap = new JPanel(new BorderLayout());
				wrap.add(b);
				wrap.setPreferredSize(new Dimension(100, 0));
				return wrap;
			}
		});

		return spinner;
	}
}