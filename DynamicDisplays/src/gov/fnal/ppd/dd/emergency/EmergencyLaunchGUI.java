package gov.fnal.ppd.dd.emergency;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
	private EmergencyMessage			message				= new EmergencyMessage();
	private JTextField					headlineText		= new JTextField(45);
	private JTextArea					messageArea			= new JTextArea(5, 65);
	private JTextArea					footerArea			= new JTextArea(2, 65);

	private JRadioButton				checkEmergency		= new JRadioButton("" + Severity.EMERGENCY);
	private JRadioButton				checkAlert			= new JRadioButton("" + Severity.ALERT);
	private JRadioButton				checkInformation	= new JRadioButton("" + Severity.INFORMATION);
	private JRadioButton				checkTesting		= new JRadioButton("" + Severity.TESTING);
	private ButtonGroup					checkButtonGroup	= new ButtonGroup();

	private EmergencyMessageDistributor	emergencyMessageDistributor;

	public static void main(String[] args) {

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
	}

	public void initialize() {
		setOpaque(true);
		setBackground(new Color(255, 160, 160));

		GridBagConstraints constraints = new GridBagConstraints();

		headlineText.setFont(new Font("Arial", Font.BOLD, 16));

		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.insets = new Insets(5, 5, 5, 5);
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
		JRadioButton[] bbs = { checkEmergency, checkAlert, checkInformation, checkTesting };
		for (JRadioButton JRB : bbs) {
			JRB.setActionCommand(JRB.getText());
			box.add(JRB);
			checkButtonGroup.add(JRB);
		}
		checkTesting.setSelected(true);

		constraints.gridx = 1;
		constraints.gridy++;
		constraints.gridwidth = 2;
		add(box, constraints);

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
