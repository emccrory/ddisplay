package gov.fnal.ppd.dd.emergency;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getFullSelectorName;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationDescription;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationName;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import static gov.fnal.ppd.dd.changer.DisplayButtons.normalOperations;
import static gov.fnal.ppd.dd.util.Util.launchErrorMessage;

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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import gov.fnal.ppd.dd.changer.DisplayChangeEvent;
import gov.fnal.ppd.dd.changer.DisplayChangeEvent.DisplayChangeType;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.CheckDisplayStatus;
import gov.fnal.ppd.dd.util.ObjectSigning;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class EmergencyLaunchGUI extends JPanel implements ActionListener {

	private static final long			serialVersionUID					= -4454406761200912692L;

	/**
	 * Set to true to show a more complex UI
	 */
	public static final boolean			SHOW_MORE_STUFF						= Boolean.getBoolean("ddisplay.extendedinformation");

	private static final String			DEFAULT_EMERGENCY_HEADLINE			= "An Emergency Situation has been delcared";
	private static final String			DEFAULT_ALERT_HEADLINE				= "ATTENTION! - ¡ATENCIÓN!";
	private static final String			DEFAULT_INFORMATION_HEADLINE		= "Important information";
	private static final String			DEFAULT_TESTING_HEADLINE			= "This is a test of the Emergency Notification System";

	private static final long			DEFAULT_TEST_MESSAGE_TIME			= ONE_MINUTE / 1000L;
	private static final long			DEFAULT_EMERGENCY_MESSAGE_TIME		= ONE_HOUR / 1000L;
	private static final long			DEFAULT_ALERT_MESSAGE_TIME			= 10 * ONE_MINUTE / 1000L;
	private static final long			DEFAULT_INFORMATION_MESSAGE_TIME	= 5 * ONE_MINUTE / 1000L;

	private JTextField					headlineText						= new JTextField(45);
	private JSpinner					time;

	private JTextArea					messageArea							= new JTextArea(4, 65);
	private JTextArea					footerArea							= new JTextArea(5, 65);

	private JLabel						emergencyStatus						= new JLabel("Have initiated no emergency messages");

	private JButton						accept								= new JButton("Send Test Message");
	private JButton						cancel								= new JButton("Exit");
	private JButton						remove								= new JButton("Remove Message");

	private JRadioButton				checkEmergency						= new JRadioButton("" + Severity.EMERGENCY);
	private JRadioButton				checkAlert							= new JRadioButton("" + Severity.ALERT);
	private JRadioButton				checkInformation					= new JRadioButton("" + Severity.INFORMATION);
	private JRadioButton				checkTesting						= new JRadioButton("" + Severity.TESTING);
	private ButtonGroup					checkButtonGroup					= new ButtonGroup();

	private ActionListener				radioActionListener;

	private EmergencyMessageDistributor	emergencyMessageDistributor;
	private List<Display>				displays;
	private List<JLabel>				footers								= new ArrayList<JLabel>();
	private List<JCheckBox>				checkBoxes							= new ArrayList<JCheckBox>();

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

		JFrame f = new JFrame("Fermilab Emergency Message Distribution Testing - Will not send messages");
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

		radioActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createDefaultMessages();
			}
		};

		displays = DisplayListFactory.getInstance(getLocationCode());

		for (Display D : displays) {
			JLabel footer = new JLabel(
					"Waiting to hear from Display " + D.getDBDisplayNumber() + " ... " + D.getClass().getSimpleName());
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
					if (ev.getType() == DisplayChangeType.ERROR) {
						launchErrorMessage(e);
						messageSendErrors = true;
					}
					// else, we really don't care.
				}
			});
		}
	}

	protected void createDefaultMessages() {
		if (checkEmergency.isSelected()) {
			setBackground(new Color(255, 100, 100));
			accept.setText("Send Emergency Message");
			time.setValue(new Long(DEFAULT_EMERGENCY_MESSAGE_TIME));
			
			setDefaultTestingText(DEFAULT_EMERGENCY_HEADLINE, false);

		} else if (checkAlert.isSelected()) {
			setBackground(new Color(220, 160, 160));
			accept.setText("Send Alert Message");
			time.setValue(new Long(DEFAULT_ALERT_MESSAGE_TIME));

			setDefaultTestingText(DEFAULT_ALERT_HEADLINE, false);

		} else if (checkInformation.isSelected()) {
			setBackground(new Color(160, 160, 255));
			accept.setText("Send Information Message");
			time.setValue(new Long(DEFAULT_INFORMATION_MESSAGE_TIME));

			setDefaultTestingText(DEFAULT_INFORMATION_HEADLINE, false);

		} else if (checkTesting.isSelected()) {
			setBackground(new Color(200, 200, 200));
			accept.setText("Send Test Message");
			time.setValue(new Long(DEFAULT_TEST_MESSAGE_TIME));

			setDefaultTestingText(DEFAULT_TESTING_HEADLINE, true);
		}
		// time.repaint();
	}

	private void setDefaultTestingText(String fill, boolean all) {
		headlineText.setText(fill);
		if (all) {
			messageArea.setText(fill);
			footerArea.setText(fill + " " + fill + " " + fill + " " + fill + " " + fill + " " + fill);
		} else {
			messageArea.setText("");
			footerArea.setText("");
		}
	}

	private static class MyJScrollPane extends JScrollPane {
		private static final long serialVersionUID = 4726817736944861133L;

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
		setDefaultTestingText(DEFAULT_TESTING_HEADLINE, true);

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

		constraints.gridx = 1;
		constraints.gridy++;
		constraints.gridwidth = 2;
		constraints.anchor = GridBagConstraints.NORTH;
		add(makeSpinner(), constraints);

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
			JRB.setOpaque(false);
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
		} // End of SHOW_MORE_STUFF block

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

	private JComponent makeSpinner() {
		final SpinnerModel model = new SpinnerListModel(getDwellStrings());
		time = new JSpinner(model);
		time.setValue(new Long(DEFAULT_TEST_MESSAGE_TIME));
		time.setAlignmentX(JComponent.CENTER_ALIGNMENT);

		JComponent field = time.getEditor();
		Dimension prefSize = field.getPreferredSize();
		field.setPreferredSize(new Dimension(100, prefSize.height));

		final JLabel timeInterpretLabel = new JLabel(interp((Long) time.getValue()));

		time.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				long val = (Long) model.getValue();
				timeInterpretLabel.setText(interp(val));
			}
		});

		JPanel internalBox = new JPanel(new GridLayout(1, 5));
		internalBox.add(Box.createRigidArea(new Dimension(1, 1)));
		internalBox.add(new JLabel("Remove the message after "));
		internalBox.add(time);
		internalBox.add(timeInterpretLabel);
		internalBox.add(Box.createRigidArea(new Dimension(1, 1)));
		internalBox.setOpaque(false);

		return internalBox;
	}

	protected static String interp(long val) {
		DecimalFormat format = new DecimalFormat("#.0");
		String t = "" + val;
		if (val <= 100L) {
			return " seconds ( testing only )";
		} else if (val < 3600) {
			double min = val / 60.0;
			t = format.format(min) + " minute" + (min != 1 ? "s" : "");
		} else {
			double hours = ((double) val) / (60 * 60);
			t = format.format(hours) + " hour" + (hours != 1 ? "s" : "");
		}
		return " seconds ( " + t + " )";
	}

	private static List<Long> getDwellStrings() {
		ArrayList<Long> retval = new ArrayList<Long>();

		retval.add(10L); // ten seconds (only for testing)
		retval.add(60L); // 1 minute (only for testing)
		retval.add(300L); // 5 minutes
		retval.add(600L); // 10 minutes
		retval.add(900L); // 15 minutes
		// retval.add(1200L); // 20 minutes
		retval.add(1800L); // 30 minutes
		// retval.add(2700L); // 45 minutes
		retval.add(3600L); // One hour
		retval.add(2L * 3600L); // It seems unlikely that these hours-long dwell times will be useful
		// retval.add(3 * 3600L);
		// retval.add(4 * 3600L);
		// retval.add(5 * 3600L);
		// retval.add(6 * 3600L);
		retval.add(8L * 3600L); // 8 hours
		// retval.add(10 * 3600L); // 10 hours
		retval.add(12L * 3600L); // 12 hours

		return retval;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		EmergencyMessage message = new EmergencyMessage();
		switch (e.getActionCommand()) {
		case "Accept":
			message.setHeadline(headlineText.getText());
			message.setMessage(messageArea.getText());
			message.setFootnote(footerArea.getText());
			message.setDwellTime((long) time.getValue() * 1000L);
			message.setSeverity(Severity.valueOf((checkButtonGroup.getSelection().getActionCommand())));
			message.setIpAddress("unknown");
			try {
				message.setIpAddress(InetAddress.getLocalHost().toString());
			} catch (UnknownHostException ex) {
				ex.printStackTrace();
			}

			Box box = Box.createVerticalBox();
			box.setAlignmentX(LEFT_ALIGNMENT);

			JLabel warningMessage = new JLabel("Are you SURE you want to send this " + message.getSeverity() + " message?");
			warningMessage.setFont(new Font("Arial", Font.BOLD, 24));
			warningMessage.setBackground(Color.white);
			warningMessage.setOpaque(true);
			warningMessage.setBorder(BorderFactory.createLineBorder(Color.white, 10));
			box.add(warningMessage);

			box.add(Box.createRigidArea(new Dimension(10, 10)));

			JLabel hl = new JLabel("Headline: '" + headlineText.getText() + "'");
			hl.setBackground(Color.white);
			hl.setOpaque(true);
			hl.setBorder(BorderFactory.createLineBorder(Color.white, 3));
			hl.setFont(new Font("Arial", Font.BOLD, 16));

			box.add(hl);

			box.add(Box.createRigidArea(new Dimension(10, 10)));

			box.add(new JLabel("Body text:"));

			JTextArea area = new JTextArea(messageArea.getText(), 5, 60);
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
			area.setEditable(false);
			box.add(new JScrollPane(area));

			box.add(Box.createRigidArea(new Dimension(10, 10)));

			box.add(new JLabel("Footnote text:"));
			area = new JTextArea(footerArea.getText(), 3, 60);
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
			area.setEditable(false);
			box.add(new JScrollPane(area));

			box.add(new JLabel("Severity = '" + message.getSeverity() + "'"));
			box.add(new JLabel("Dwell Time = " + time.getValue() + " seconds"));

			// box.setBackground(message.getSeverity().);

			Color bg = new Color(200, 200, 200);
			switch (message.getSeverity()) {
			case EMERGENCY:
				bg = new Color(255, 100, 100);
				break;
			case ALERT:
				bg = new Color(220, 160, 160);
				break;
			case INFORMATION:
				bg = new Color(160, 160, 255);
				break;
			default:
				break;
			}
			box.setBackground(bg);
			box.setBorder(BorderFactory.createLineBorder(bg, 20));
			box.setOpaque(true);

			String text = "Sent Emergency Message";
			messageSendErrors = false;
			if (JOptionPane.showConfirmDialog(this, box, "Are you SURE??", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				if (emergencyMessageDistributor.send(message))
					if (!messageSendErrors)
						JOptionPane.showMessageDialog(this, "Emergency message sent at " + new Date());
					else
						text = "Not showing message: Send failed";
			} else {
				JOptionPane.showMessageDialog(this, "Emergency message delivery NOT CONFIRMED");
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

	/**
	 * @return A list of displays to which we should send the emergency message
	 */
	public List<Display> getTargetedDisplays() {
		List<Display> retval = new ArrayList<Display>();
		for (int i = 0; i < displays.size(); i++)
			if (checkBoxes.get(i).isSelected())
				retval.add(displays.get(i));

		return retval;
	}
}
