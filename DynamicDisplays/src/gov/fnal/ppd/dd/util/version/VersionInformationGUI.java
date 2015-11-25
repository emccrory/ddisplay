package gov.fnal.ppd.dd.util.version;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.border.Border;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class VersionInformationGUI extends JFrame {

	private static final long			serialVersionUID	= 1618474079742658363L;
	private VersionInformation			newVI				= VersionInformation.getVersionInformation();
	private final VersionInformation	oldVI				= VersionInformation.getVersionInformation();
	private JTextArea					textArea;

	/**
	 * 
	 */
	public VersionInformationGUI() {
		super("Version Information");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		VersionInformationGUI frame = new VersionInformationGUI();

		frame.setMyContent();

		frame.pack();
		frame.setVisible(true);
	}

	private void setMyContent() {
		Box content = Box.createVerticalBox();

		content.add(getDateComponents());
		content.add(Box.createRigidArea(new Dimension(8, 8)));

		content.add(getVersionNumberComponent());
		content.add(Box.createRigidArea(new Dimension(8, 8)));

		content.add(getDescriptionComponent());
		content.add(Box.createRigidArea(new Dimension(8, 8)));

		content.add(getFlavorComponent());
		content.add(Box.createRigidArea(new Dimension(8, 8)));

		Box hb = Box.createHorizontalBox();
		hb.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		content.add(hb);

		JButton accept = new JButton("Accept this version information and close");
		hb.add(accept);

		accept.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				newVI.setVersionDescription(textArea.getText());

				if (newVI.equals(oldVI)) {
					System.out.println("No change in version information");
					System.exit(-1);
				} else {
					VersionInformation.saveVersionInformation(newVI);
					System.exit(0);
				}
			}
		});

		JButton reject = new JButton("Cancel");
		reject.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("**CANCELED** (No change in version information)");
				System.exit(-1);
			}
		});
		hb.add(Box.createRigidArea(new Dimension(10, 10)));
		hb.add(reject);

		setContentPane(content);
	}

	private Component getFlavorComponent() {
		JPanel retval = new JPanel();

		retval.add(new JLabel("Disposition: "));
		retval.add(new JLabel("" + newVI.getDisposition()));

		retval.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		retval.setBorder(b());
		return retval;
	}

	private JComponent getVersionNumberComponent() {
		Box box = Box.createHorizontalBox();

		final JButton f1Inc = new JButton(newVI.getVersionVal(0) + " +");
		final JButton f2Inc = new JButton(newVI.getVersionVal(1) + " +");
		final JButton f3Inc = new JButton(newVI.getVersionVal(2) + " +");
		final JLabel fullVersion = new JLabel("( " + newVI.getVersionString() + " )");

		f1Inc.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				newVI.setVersionVal(0, newVI.getVersionVal(0) + 1);
				newVI.setVersionVal(1, 0);
				newVI.setVersionVal(2, 1);

				f1Inc.setText(newVI.getVersionVal(0) + " +");
				f2Inc.setText(newVI.getVersionVal(1) + " +");
				f3Inc.setText(newVI.getVersionVal(2) + " +");
				fullVersion.setText("( " + newVI.getVersionString() + " )");
			}
		});
		f2Inc.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				newVI.setVersionVal(1, newVI.getVersionVal(1) + 1);
				newVI.setVersionVal(2, 1);

				f2Inc.setText(newVI.getVersionVal(1) + " +");
				f3Inc.setText(newVI.getVersionVal(2) + " +");
				fullVersion.setText("( " + newVI.getVersionString() + " )");
			}
		});
		f3Inc.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				newVI.setVersionVal(2, newVI.getVersionVal(2) + 1);

				f3Inc.setText(newVI.getVersionVal(2) + " +");
				fullVersion.setText("( " + newVI.getVersionString() + " )");
			}
		});

		box.add(new JLabel("Version number: "));
		box.add(Box.createHorizontalGlue());
		box.add(Box.createRigidArea(new Dimension(8, 8)));
		box.add(f1Inc);
		box.add(Box.createRigidArea(new Dimension(8, 8)));
		box.add(new JLabel("\u2022"));
		box.add(Box.createRigidArea(new Dimension(8, 8)));
		box.add(f2Inc);
		box.add(Box.createRigidArea(new Dimension(8, 8)));
		box.add(new JLabel("\u2022"));
		box.add(Box.createRigidArea(new Dimension(8, 8)));
		box.add(f3Inc);
		box.add(Box.createRigidArea(new Dimension(20, 20)));

		box.add(fullVersion);
		box.add(Box.createHorizontalGlue());

		box.setBorder(b());
		JPanel retval = new JPanel();
		retval.setAlignmentX(LEFT_ALIGNMENT);
		retval.add(box);
		return retval;
	}

	private Container getDateComponents() {
		Box box = Box.createHorizontalBox();
		box.add(Box.createRigidArea(new Dimension(10, 10)));
		box.add(new JLabel("Time stamp: "));
		final JRadioButton rb1 = new JRadioButton("Now");
		rb1.setSelected(false);
		final JRadioButton rb2 = new JRadioButton("No change");
		rb2.setSelected(true);

		final JLabel timeStampChosen = new JLabel(" " + new Date(newVI.getTimeStamp()) + " ");

		ActionListener rbListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (rb1.isSelected()) {
					Date d = new Date(System.currentTimeMillis());
					newVI.setTimeStamp(d.getTime());
				} else if (rb2.isSelected()) {
					newVI.setTimeStamp(oldVI.getTimeStamp());
				}
				timeStampChosen.setText("" + new Date(newVI.getTimeStamp()) + " ");
			}
		};
		rb1.addActionListener(rbListener);
		rb2.addActionListener(rbListener);

		ButtonGroup timeGroup = new ButtonGroup();
		timeGroup.add(rb1);
		timeGroup.add(rb2);

		box.add(rb1);
		box.add(rb2);
		box.add(Box.createRigidArea(new Dimension(10, 10)));
		box.add(new JSeparator(JSeparator.VERTICAL));
		box.add(Box.createRigidArea(new Dimension(10, 10)));
		box.add(timeStampChosen);
		box.add(Box.createRigidArea(new Dimension(10, 10)));

		box.add(Box.createHorizontalGlue());

		box.setBorder(b());
		box.setAlignmentX(LEFT_ALIGNMENT);

		return box;
	}

	private JComponent getDescriptionComponent() {
		textArea = new JTextArea(newVI.getVersionDescription(), 10, 60);
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		
		textArea.setFont(new Font("Courier", Font.PLAIN, 12));
		textArea.setAlignmentX(LEFT_ALIGNMENT);
		textArea.setBorder(b());
		return textArea;
	}

	private static Border b() {
		return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black),
				BorderFactory.createEmptyBorder(5, 5, 5, 5));
	}
}
