package gov.fnal.ppd.dd.emergency;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationDescription;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationName;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import static gov.fnal.ppd.dd.changer.DisplayButtons.normalOperations;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.CheckDisplayStatus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class EmergencyLaunchGUI extends JPanel implements ActionListener {

	private static final long			serialVersionUID	= -4454406761200912692L;

	public static boolean				SHOW_MORE_STUFF		= Boolean.getBoolean("ddisplay.extendedinformation");

	private EmergencyMessage			message				= new EmergencyMessage();
	private JTextField					headlineText		= new JTextField(45);
	private JTextArea					messageArea			= new JTextArea(5, 65);
	private JTextArea					footerArea			= new JTextArea(2, 65);

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
																	} else if (checkAlert.isSelected()) {
																		setBackground(new Color(220, 160, 160));
																	} else if (checkInformation.isSelected()) {
																		setBackground(new Color(160, 160, 255));
																	} else if (checkTesting.isSelected()) {
																		setBackground(new Color(200, 200, 200));
																	}
																}
															};

	private EmergencyMessageDistributor	emergencyMessageDistributor;
	private List<Display>				displays;
	private List<JLabel>				footers				= new ArrayList<JLabel>();

	public static void main(String[] args) {
		credentialsSetup();

		selectorSetup();

		EmergencyLaunchGUI elg = new EmergencyLaunchGUI(new EmergencyMessageDistributor() {

			@Override
			public boolean send(EmergencyMessage em) {
				System.out.println(em);
				System.out.println("The HTML looks like this:\n" + em.toHTML());
				return true;
			}
		});
		elg.initialize();

		JFrame f = new JFrame("Test");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(elg);
		f.pack();
		f.setVisible(true);
	}

	public EmergencyLaunchGUI(EmergencyMessageDistributor emd) {
		super(new GridBagLayout());
		this.emergencyMessageDistributor = emd;
		normalOperations = false;

		displays = DisplayListFactory.getInstance(SignageType.XOC, getLocationCode());

		for (Display D : displays) {
			JLabel footer = new JLabel("Waiting to hear from Display " + D.getDBDisplayNumber() + " ...");
			footer.setFont(new Font("Arial", Font.PLAIN, 10));
			footer.setOpaque(true);
			footer.setBorder(BorderFactory.createLineBorder(Color.black));
			(new CheckDisplayStatus(D, footer)).start();
			footers.add(footer);
		}

	}

	public void initialize() {
		setOpaque(true);
		setBackground(new Color(200, 200, 200));

		GridBagConstraints constraints = new GridBagConstraints();

		headlineText.setFont(new Font("Arial", Font.BOLD, 16));

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
		add(messageArea, constraints);

		constraints.gridx = 1;
		constraints.gridy++;
		constraints.anchor = GridBagConstraints.NORTH;
		add(new JLabel("Small Print: "), constraints);
		constraints.gridx++;
		footerArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		add(footerArea, constraints);

		JButton accept = new JButton("Accept");
		JButton cancel = new JButton("Cancel");
		accept.addActionListener(this);
		cancel.addActionListener(this);

		// TODO add radio buttons for the severity
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
			box.add(JRB);
			box.add(Box.createRigidArea(new Dimension(30, 10)));
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
			box.add(new JLabel(", description -- '" + getLocationDescription() + "'"));
			add(box, constraints);

			constraints.gridx = 1;
			constraints.gridy++;
			constraints.gridwidth = 2;
			int numCols = 1;
			if (footers.size() < 10)
				numCols = 1;
			else if (footers.size() < 20)
				numCols = 2;
			else if (footers.size() < 30)
				numCols = 3;
			else if (footers.size() < 40)
				numCols = 4;
			else if (footers.size() < 50)
				numCols = 5;
			else
				numCols = 6;
			JPanel footerPanel = new JPanel(new GridLayout(0, numCols, 2, 2));
			for (JLabel L : footers)
				footerPanel.add(L);

			add(footerPanel, constraints);
		}

		constraints.gridx = 1;
		constraints.gridy++;
		constraints.gridwidth = 2;
		box = Box.createHorizontalBox();
		box.add(accept);
		box.add(Box.createRigidArea(new Dimension(5, 5)));
		box.add(cancel);
		add(box, constraints);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case "Accept":
			message.setHeadline(headlineText.getText());
			message.setMessage(messageArea.getText());
			message.setFootnote(footerArea.getText());
			message.setSeverity(Severity.valueOf((checkButtonGroup.getSelection().getActionCommand())));
			if (emergencyMessageDistributor.send(message))
				System.exit(0);
			break;

		default:
			break;
		}
		System.exit(-1);
	}
}
