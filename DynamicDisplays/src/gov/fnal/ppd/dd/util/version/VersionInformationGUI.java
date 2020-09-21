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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;

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
		newVI.setDisposition(FLAVOR.DEVELOPMENT);
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

		content.add(getFlavorComponent(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				JComboBox<FLAVOR> menu = (JComboBox<FLAVOR>) e.getSource();
				newVI.setDisposition((FLAVOR) menu.getSelectedItem());
			}
		}));
		content.add(Box.createRigidArea(new Dimension(8, 8)));

		content.add(getDispositionComponent());
		content.add(Box.createRigidArea(new Dimension(8, 8)));

		Box hb = Box.createHorizontalBox();
		hb.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		content.add(hb);

		JButton accept = new JButton("Accept this versionn information and close");
		hb.add(accept);

		accept.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				newVI.setVersionDescription(textArea.getText());

				if (newVI.equals(oldVI)) {
					System.out.println("No change in version information");
					System.exit(-1);
				} else {
					System.out.println("Accepted new version, " + newVI.getVersionString() + " with the description '" + newVI.getVersionDescription() + "'");
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
				System.out.println("Would have saved this information: " + newVI);
				System.exit(-1);
			}
		});
		hb.add(Box.createRigidArea(new Dimension(10, 10)));
		hb.add(reject);

		content.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		setContentPane(content);
	}

	private JComponent getDispositionComponent() {
		Box p = Box.createVerticalBox();
		p.add(new JLabel("The dispostions are: "));
		p.add(new JLabel(" DEVELOPMENT: For changes that should not be used anywhere important"));
		p.add(new JLabel(" TEST: Designated test nodes should pick up these changes"));
		p.add(new JLabel(" PRODUCTION: Every node should pick up and install this version"));
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray),
				BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		return p;
	}

	private Component getFlavorComponent(ActionListener listener) {
		FLAVOR[] description = { FLAVOR.DEVELOPMENT, FLAVOR.TEST, FLAVOR.PRODUCTION };
		JComboBox<FLAVOR> c = new JComboBox<FLAVOR>();
		for (FLAVOR D : description)
			c.addItem(D);

		c.setSelectedItem(newVI.getDisposition());
		System.out.println("Setting disposition to " + newVI.getDisposition());
		c.addActionListener(listener);

		JPanel retval = new JPanel();

		retval.add(new JLabel("Disposition: "));
		retval.add(c);

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
		rb1.setSelected(true);
		final JRadioButton rb2 = new JRadioButton("No change");
		rb2.setSelected(false);

		Date d = new Date();
		final JLabel timeStampChosen = new JLabel(" " + d + " ");
		newVI.setTimeStamp(d.getTime());

		ActionListener rbListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (rb1.isSelected()) {
					Date d = new Date();
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
