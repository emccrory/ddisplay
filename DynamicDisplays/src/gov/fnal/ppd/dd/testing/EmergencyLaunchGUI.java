package gov.fnal.ppd.dd.testing;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class EmergencyLaunchGUI extends JPanel implements ActionListener {

	private static final long	serialVersionUID	= -4454406761200912692L;
	private EmergencyMessage	message				= new EmergencyMessage();
	private JTextField			headlineText		= new JTextField(60);
	private JTextArea			messageArea			= new JTextArea(5, 60);

	public static void main(String[] args) {

		EmergencyLaunchGUI elg = new EmergencyLaunchGUI();
		elg.initialize();

		JFrame f = new JFrame("Test");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(elg);
		f.pack();
		f.setVisible(true);
	}

	public EmergencyLaunchGUI() {
		super(new GridBagLayout());
	}

	public void initialize() {
		GridBagConstraints constraints = new GridBagConstraints();

		headlineText.setFont(new Font("Arial", Font.BOLD, 16));

		constraints.gridx = 1;
		constraints.gridy = 1;
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
		add(messageArea, constraints);

		JButton accept = new JButton("Accept");
		JButton cancel = new JButton("Cancel");
		accept.addActionListener(this);
		cancel.addActionListener(this);
		
		// TODO add radio buttons for the severity
		
		constraints.gridx = 1;
		constraints.gridy++;
		constraints.anchor = GridBagConstraints.EAST;
		add(accept, constraints);
		constraints.gridx = 2;
		constraints.anchor = GridBagConstraints.WEST;
		add(cancel, constraints);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case "Accept":
			message.setHeadline(headlineText.getText());
			message.setMessage(messageArea.getText());
			System.out.println(message.toHTML());
			System.exit(0);
			break;

		default:
			break;
		}

	}
}
