package gov.fnal.ppd.dd.util.version;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
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
import javax.swing.JTextField;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class VersionInformationGUI extends JFrame {

	private static final long	serialVersionUID	= 1618474079742658363L;
	private VersionInformation	vi					= VersionInformation.getVersionInformation();
	private JTextArea			textArea;

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

		content.add(getRadios());
		content.add(Box.createRigidArea(new Dimension(8, 8)));

		content.add(getVersionNumbers());
		content.add(Box.createRigidArea(new Dimension(8, 8)));

		JComponent cp = getDescription();
		cp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		content.add(cp);
		content.add(Box.createRigidArea(new Dimension(8, 8)));

		content.add(new JLabel(
				"<html>If you change this information, you must recommit<br>and then repush the version data file to the repository.</html>"));
		content.add(Box.createRigidArea(new Dimension(8, 8)));

		JButton accept = new JButton("Accept this version information and close");
		content.add(accept);

		accept.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				vi.setVersionDescription(textArea.getText());

				VersionInformation.saveVersionInformation(vi);

				System.exit(0);
			}
		});

		setContentPane(content);
	}

	private JComponent getVersionNumbers() {
		Box box = Box.createHorizontalBox();
		final JTextField f1 = new JTextField("" + vi.getVersionVal(0), 3);
		final JTextField f2 = new JTextField("" + vi.getVersionVal(1), 3);
		final JTextField f3 = new JTextField("" + vi.getVersionVal(2), 3);

		final JLabel fullVersion = new JLabel("( " + vi.getVersionString() + " )");
		ActionListener lis = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				try {
					vi.setVersionVal(0, Integer.parseInt(f1.getText()));
					vi.setVersionVal(1, Integer.parseInt(f2.getText()));
					vi.setVersionVal(2, Integer.parseInt(f3.getText()));

					fullVersion.setText("( " + vi.getVersionString() + " )");
				} catch (NumberFormatException e) {

				}
			}
		};
		f1.addActionListener(lis);
		f2.addActionListener(lis);
		f3.addActionListener(lis);

		box.add(Box.createHorizontalGlue());
		box.add(new JLabel("Version number: "));
		box.add(f1);
		box.add(new JLabel(" . "));
		box.add(f2);
		box.add(new JLabel(" . "));
		box.add(f3);
		box.add(Box.createRigidArea(new Dimension(8, 8)));
		box.add(fullVersion);
		box.add(Box.createHorizontalGlue());

		box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		JPanel retval = new JPanel();
		retval.setAlignmentX(LEFT_ALIGNMENT);
		retval.add(box);
		return retval;
	}

	private Container getRadios() {
		Box box = Box.createHorizontalBox();
		box.add(new JLabel("Time stamp to use: "));
		final JRadioButton rb1 = new JRadioButton("Now");
		rb1.setSelected(false);
		final JRadioButton rb2 = new JRadioButton("Custom");
		rb2.setSelected(false);

		final JLabel timeStampChosen = new JLabel(" " + new Date(vi.getTimeStamp()));

		ActionListener rbListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (rb1.isSelected()) {
					Date d = new Date(System.currentTimeMillis());
					vi.setTimeStamp(d.getTime());
					timeStampChosen.setText(" " + d);
				} else if (rb2.isSelected()) {
					// POPUP to get the time stamp they intend
					System.err.println("Not implemented");
				}
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
		box.add(Box.createHorizontalGlue());

		box.setBorder(BorderFactory.createLineBorder(Color.black));
		box.setAlignmentX(LEFT_ALIGNMENT);

		return box;
	}

	private JComponent getDescription() {
		textArea = new JTextArea(vi.getVersionDescription(), 10, 60);
		textArea.setBorder(BorderFactory.createBevelBorder(2));
		textArea.setAlignmentX(LEFT_ALIGNMENT);
		return textArea;
	}
}
