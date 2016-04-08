package gov.fnal.ppd.dd.emergency;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getFullSelectorName;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationDescription;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationName;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import static gov.fnal.ppd.dd.changer.DisplayButtons.normalOperations;
import static gov.fnal.ppd.dd.util.Util.launchErrorMessage;
import gov.fnal.ppd.dd.changer.DisplayChangeEvent;
import gov.fnal.ppd.dd.changer.DisplayChangeEvent.Type;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.CheckDisplayStatus;
import gov.fnal.ppd.dd.util.ObjectSigning;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class EmergencyLaunchGUI extends JPanel implements ActionListener {

	private static final long			serialVersionUID	= -4454406761200912692L;

	/**
	 * Set to true to show a more complex UI
	 */
	public static final boolean			SHOW_MORE_STUFF		= Boolean.getBoolean("ddisplay.extendedinformation");

	private JTextField					headlineText		= new JTextField(45);
	private JTextArea					messageArea			= new JTextArea(5, 65);
	private JTextArea					footerArea			= new JTextArea(2, 65);

	private JLabel						emergencyStatus		= new JLabel("Have initiated no emergency messages");

	private JButton						accept				= new JButton("Send Test Message");
	private JButton						cancel				= new JButton("Exit");
	private JButton						remove				= new JButton("Remove Message");

	private JRadioButton				checkEmergency		= new JRadioButton("" + Severity.EMERGENCY);
	private JRadioButton				checkAlert			= new JRadioButton("" + Severity.ALERT);
	private JRadioButton				checkInformation	= new JRadioButton("" + Severity.INFORMATION);
	private JRadioButton				checkTesting		= new JRadioButton("" + Severity.TESTING);
	private ButtonGroup					checkButtonGroup	= new ButtonGroup();

	private ActionListener				radioActionListener	= new ActionListener() {

																@Override
																public void actionPerformed(ActionEvent e) {
																	if (checkEmergency.isSelected()) {
																		setBackground(new Color(255, 100, 100));
																		accept.setText("Send Emergency Message");
																	} else if (checkAlert.isSelected()) {
																		setBackground(new Color(220, 160, 160));
																		accept.setText("Send Alert Message");
																	} else if (checkInformation.isSelected()) {
																		setBackground(new Color(160, 160, 255));
																		accept.setText("Send Information Message");
																	} else if (checkTesting.isSelected()) {
																		setBackground(new Color(200, 200, 200));
																		accept.setText("Send Test Message");
																	}
																}
															};

	private EmergencyMessageDistributor	emergencyMessageDistributor;
	private List<Display>				displays;
	private List<JLabel>				footers				= new ArrayList<JLabel>();
	private List<JCheckBox>				checkBoxes			= new ArrayList<JCheckBox>();

	private boolean						messageSendErrors;

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		credentialsSetup();

		selectorSetup();

		if (!ObjectSigning.getInstance().isEmergMessAllowed(getFullSelectorName())) {
			JOptionPane.showMessageDialog(null, "'" + getFullSelectorName() + "' is not allowed to create emergency messages.");
			System.exit(-1);
		}

		EmergencyLaunchGUI elg = new EmergencyLaunchGUI(new EmergencyMessageDistributor() {

			@Override
			public boolean send(EmergencyMessage em) {
				System.out.println(em);
				JOptionPane.showMessageDialog(null,
						"THIS IS A TEST VERSION THAT DOES NOT SEND ANYTHING OUT.\nThe HTML would look like this:\n" + em.toHTML());
				return true;
			}
		});
		elg.initialize();

		JFrame f = new JFrame("Emergency Message Distribution");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		elg.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 20),
				BorderFactory.createTitledBorder("NOT THE ACTUAL EMERGENCY MESSAGE GUI.")));
		f.setContentPane(elg);
		f.pack();
		f.setVisible(true);
	}

	/**
	 * @param emd
	 */
	public EmergencyLaunchGUI(final EmergencyMessageDistributor emd) {
		super(new GridBagLayout());
		this.emergencyMessageDistributor = emd;
		normalOperations = false;

		displays = DisplayListFactory.getInstance(SignageType.XOC, getLocationCode());

		for (Display D : displays) {
			JLabel footer = new JLabel("Waiting to hear from Display " + D.getDBDisplayNumber() + " ... "
					+ D.getClass().getSimpleName());
			footer.setFont(new Font("Arial", Font.PLAIN, 10));
			footer.setMaximumSize(new Dimension(150, 30));
			(new CheckDisplayStatus(D, footer)).start();
			footers.add(footer);

			JCheckBox cb = new JCheckBox();
			cb.setActionCommand("" + D.getDBDisplayNumber());
			cb.setSelected(true);
			cb.setToolTipText("Display " + D.getVirtualDisplayNumber() + "|" + D.getDBDisplayNumber() + ", " + D.getLocation());
			checkBoxes.add(cb);

			// Add listener to know when a message has been rejected.
			D.addListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					DisplayChangeEvent ev = (DisplayChangeEvent) e;
					if (ev.getType() == Type.ERROR) {
						launchErrorMessage(e);
						messageSendErrors = true;
					}
					// else, we really don't care.
				}
			});
		}
	}

	private static class MyJScrollPane extends JScrollPane {
		private static final long	serialVersionUID	= 4726817736944861133L;

		public MyJScrollPane(Component c, int minWidth, int minHeight) {
			super(c);
			setMinimumSize(new Dimension(minWidth, minHeight));
		}
	}

	/**
	 * 
	 */
	public void initialize() {
		setOpaque(true);
		setBackground(new Color(200, 200, 200));

		GridBagConstraints constraints = new GridBagConstraints();

		headlineText.setFont(new Font("Arial", Font.BOLD, 16));
		headlineText.setMinimumSize(new Dimension(700, 23));
		messageArea.setMinimumSize(new Dimension(700, 80));
		footerArea.setMinimumSize(new Dimension(700, 37));
		emergencyStatus.setFont(new Font("Arial", Font.PLAIN, 18));

		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.gridwidth = 2;
		JLabel big = new JLabel("Emergency Notification System for Digital Displays");
		big.setFont(new Font("Arial", Font.BOLD, 28));
		add(big, constraints);

		constraints.gridx = 1;
		constraints.gridy++;
		constraints.gridwidth = 1;
		JLabel label = new JLabel("Headline: ");
		label.setFont(new Font("Arial", Font.BOLD, 16));

		add(label, constraints);
		constraints.gridx++;
		add(headlineText, constraints);

		constraints.gridx = 1;
		constraints.gridy++;
		constraints.anchor = GridBagConstraints.NORTH;
		add(new JLabel("Message: "), constraints);
		constraints.gridx++;
		messageArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		messageArea.setLineWrap(true);
		messageArea.setWrapStyleWord(true);
		add(new MyJScrollPane(messageArea, 710, 80), constraints);

		constraints.gridx = 1;
		constraints.gridy++;
		constraints.anchor = GridBagConstraints.NORTH;
		add(new JLabel("Small Print: "), constraints);
		constraints.gridx++;
		footerArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		footerArea.setLineWrap(true);
		footerArea.setWrapStyleWord(true);
		add(new MyJScrollPane(footerArea, 710, 37), constraints);

		accept.setActionCommand("Accept");
		cancel.setActionCommand("Cancel");
		remove.setActionCommand("Remove");

		accept.setFont(new Font("Arial", Font.BOLD, 32));
		accept.setBackground(Color.RED.darker().darker());
		accept.setForeground(Color.WHITE);
		accept.addActionListener(this);

		cancel.setFont(new Font("Arial", Font.BOLD, 32));
		cancel.addActionListener(this);

		remove.setFont(new Font("Arial", Font.BOLD, 18));
		remove.setBackground(Color.GREEN.darker().darker());
		remove.setForeground(Color.WHITE);
		remove.addActionListener(this);

		Box box = Box.createHorizontalBox();
		box.setOpaque(true);
		box.setBackground(Color.white);
		box.setBorder(BorderFactory.createLineBorder(Color.black));
		box.add(Box.createRigidArea(new Dimension(30, 10)));
		box.add(new JLabel("Message type:"));
		box.add(Box.createRigidArea(new Dimension(30, 10)));
		JRadioButton[] bbs = { checkEmergency, checkAlert, checkInformation, checkTesting };
		for (JRadioButton JRB : bbs) {
			JRB.setActionCommand(JRB.getText());
			JRB.addActionListener(radioActionListener);
			JRB.setFont(new Font("Arial", Font.PLAIN, 20));
			box.add(new JSeparator(JSeparator.VERTICAL));
			box.add(Box.createRigidArea(new Dimension(10, 10)));
			box.add(JRB);
			box.add(Box.createRigidArea(new Dimension(10, 10)));
			checkButtonGroup.add(JRB);
		}
		checkTesting.setSelected(true);

		constraints.gridx = 1;
		constraints.gridy++;
		constraints.gridwidth = 2;
		add(box, constraints);

		if (SHOW_MORE_STUFF) {
			constraints.gridx = 1;
			constraints.gridy++;
			constraints.gridwidth = 2;
			box = Box.createHorizontalBox();
			box.add(new JLabel("Location: '" + getLocationName() + "'"));
			box.add(new JLabel(" (code=" + getLocationCode() + ")"));
			box.add(new JLabel(" -- '" + getLocationDescription() + "'"));
			add(box, constraints);

			JButton selectAll = new JButton("Select ALL displays");
			selectAll.setActionCommand("ALL");
			selectAll.setFont(new Font("Arial", Font.BOLD, 10));
			JButton selectNone = new JButton("Select NO displays");
			selectNone.setActionCommand("NONE");
			selectNone.setFont(new Font("Arial", Font.BOLD, 10));

			ActionListener selectListener = new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					switch (e.getActionCommand()) {
					case "ALL":
						for (JCheckBox CB : checkBoxes)
							CB.setSelected(true);
						break;
					case "NONE":
						for (JCheckBox CB : checkBoxes)
							CB.setSelected(false);
						break;
					}
				}
			};

			selectAll.addActionListener(selectListener);
			selectNone.addActionListener(selectListener);

			constraints.gridx = 1;
			constraints.gridy++;
			constraints.gridwidth = 2;
			box = Box.createHorizontalBox();
			box.add(selectAll);
			box.add(selectNone);
			add(box, constraints);

			constraints.gridx = 1;
			constraints.gridy++;
			constraints.gridwidth = 2;
			int numCols = 1;
			int maxWidth = 230;
			if (footers.size() < 10)
				numCols = 1;
			else if (footers.size() < 20)
				numCols = 2;
			else if (footers.size() < 30)
				numCols = 3;
			else if (footers.size() < 40) {
				numCols = 4;
				maxWidth = 200;
			} else if (footers.size() < 50) {
				numCols = 5;
				maxWidth = 160;
			} else {
				numCols = 6;
				maxWidth = 130;
			}
			JPanel footerPanel = new JPanel(new GridLayout(0, numCols, 2, 2));
			footerPanel.setMaximumSize(new Dimension(maxWidth * numCols, 20 * footers.size() / numCols));
			footerPanel.setPreferredSize(new Dimension(maxWidth * numCols, 20 * footers.size() / numCols));

			for (int k = 0; k < footers.size(); k++) {
				Box b = Box.createHorizontalBox();
				b.setOpaque(true);

				b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black),
						BorderFactory.createEmptyBorder(1, 2, 1, 2)));
				b.add(checkBoxes.get(k));
				b.add(footers.get(k));
				b.setMaximumSize(new Dimension(maxWidth, 18));
				b.setPreferredSize(new Dimension(maxWidth, 18));
				footerPanel.add(b);
			}

			add(footerPanel, constraints);
		}

		constraints.gridx = 1;
		constraints.gridy++;
		constraints.gridwidth = 2;
		box = Box.createHorizontalBox();
		box.add(accept);
		box.add(Box.createRigidArea(new Dimension(5, 5)));
		// box.add(cancel);
		// box.add(Box.createRigidArea(new Dimension(5, 5)));
		box.add(remove);
		add(box, constraints);

		constraints.gridx = 1;
		constraints.gridy++;
		constraints.gridwidth = 2;
		add(emergencyStatus, constraints);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		EmergencyMessage message = new EmergencyMessage();
		switch (e.getActionCommand()) {
		case "Accept":
			message.setHeadline(headlineText.getText());
			message.setMessage(messageArea.getText());
			message.setFootnote(footerArea.getText());
			message.setSeverity(Severity.valueOf((checkButtonGroup.getSelection().getActionCommand())));
			Object body = "Send this? " + message;
			if (((String) body).length() > 80) {
				Box box = Box.createVerticalBox();

				box.add(new JLabel("Headline: '" + headlineText.getText() + "'"));

				box.add(Box.createRigidArea(new Dimension(10, 10)));

				box.add(new JLabel("Body text:"));

				JTextArea area = new JTextArea(messageArea.getText(), 5, 60);
				area.setLineWrap(true);
				area.setWrapStyleWord(true);
				box.add(new JScrollPane(area));

				box.add(Box.createRigidArea(new Dimension(10, 10)));

				box.add(new JLabel("Footnote text:"));
				area = new JTextArea(footerArea.getText(), 3, 60);
				area.setLineWrap(true);
				area.setWrapStyleWord(true);
				box.add(new JScrollPane(area));

				box.add(new JLabel("Severity = '" + message.getSeverity() + "'"));
				body = box;
			}

			String text = "Sent Emergency Message";
			messageSendErrors = false;
			if (JOptionPane.showConfirmDialog(this, body, "Are you SURE??", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				if (emergencyMessageDistributor.send(message))
					if (!messageSendErrors)
						JOptionPane.showMessageDialog(this, "Emergency message sent at " + new Date());
					else
						text = "Not showing message: Send failed";
			} else {
				JOptionPane.showMessageDialog(this, "Emergency message NOT CONFIRMED");
				text = "Not showing message: User rejected";
			}
			emergencyStatus.setText(text + " at " + new Date());
			break;

		case "Cancel":
			System.exit(0);

		case "Remove":
			message.setSeverity(Severity.REMOVE);
			text = "Emergency message was removed";
			messageSendErrors = false;
			if (emergencyMessageDistributor.send(message))
				if (!messageSendErrors)
					JOptionPane.showMessageDialog(this, "Emergency message REMOVED at " + new Date());
				else {
					JOptionPane.showMessageDialog(this, "Problem removing emergency message");
					text = "Unknown state: Message may have been removed";
				}

			emergencyStatus.setText(text + " at " + new Date());

			break;
		default:
			break;
		}
	}
}
