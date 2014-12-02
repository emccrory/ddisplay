package gov.fnal.ppd.dd.changer;

import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.DisplayButtonGroup;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Helper class for adding a new URL, temporarily, to the system.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class AddYourOwnURL extends ChannelButtonGrid implements ActionListener {

	private static final long			serialVersionUID	= 6550883217360074694L;
	private static final String			NEW_URL_ACTION		= "NEW URL ACTION COMMAND";

	private static List<AddYourOwnURL>	myInstances			= new ArrayList<AddYourOwnURL>();

	private JButton						addNew				= new JButton("Add New, Temporary URL");
	private Box							content				= Box.createVerticalBox();
	private JPanel						outerLayer			= new JPanel();
	private List<DDButton>				buttons				= new ArrayList<DDButton>();
	private JLabel						title				= new JLabel("The URLs added are:");
	private String						name;
	private Display						display				= null;
	private static int					number				= 1000;

	/**
	 * @param display
	 * @param bg
	 */
	public AddYourOwnURL(final Display display, final DisplayButtonGroup bg) {
		super(display, bg);
		this.display = display;
		this.name = display.getLocation();

		JPanel innerLayer = new JPanel(new BorderLayout());
		innerLayer.add(addNew, BorderLayout.NORTH);
		addNew.addActionListener(this);
		content.add(title);
		innerLayer.add(content, BorderLayout.CENTER);
		outerLayer.add(innerLayer);

		add(outerLayer);
		myInstances.add(this);
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		if (ev.getSource() == addNew) {
			String s = (String) JOptionPane.showInputDialog(this, "Enter a valid URL (including the 'http://' part)",
					"Customized Dialog", JOptionPane.PLAIN_MESSAGE, null, null, null);

			if ((s != null) && (s.length() > 0)) {
				try {
					if (!s.startsWith("http://") && !s.startsWith("https://"))
						throw new Exception("Invalid URL: does not start with 'http://' or 'https://'");
					for (AddYourOwnURL AYO : myInstances) {
						Channel channel = new ChannelImpl(s, ChannelCategory.PUBLIC, "Dynamically added channel", new URI(s),
								number++);
						DDButton newButton = new DDButton(channel, AYO.display, 99);
						newButton.setEnabled(true);
						newButton.setToolTipText("Launch URL " + s + " on " + AYO.name);
						newButton.setActionCommand(NEW_URL_ACTION);
						newButton.addActionListener(AYO);
						if (AYO.display != null)
							newButton.addActionListener(AYO.display);

						AYO.buttons.add(newButton);
						AYO.content.removeAll();
						AYO.content.add(AYO.title);

						AYO.content.add(Box.createRigidArea(new Dimension(10, 10)));
						for (JButton B : AYO.buttons) {
							AYO.content.add(B);
							AYO.content.add(Box.createRigidArea(new Dimension(5, 5)));
						}
						AYO.validate();
					}
				} catch (Exception ex) {
					// This will catch a bad URL
					JOptionPane.showMessageDialog(this, ex.getLocalizedMessage(), "Input not accepted", JOptionPane.ERROR_MESSAGE);
				}
			}
		} else if (NEW_URL_ACTION.equals(ev.getActionCommand())) {
			System.out.println("Would launch [" + ((JButton) ev.getSource()).getText() + "] for '" + name + "'");
		}
	}
	// public static void main(String[] args) {
	// JFrame f = new JFrame("Test of " + AddYourOwnURL.class.getSimpleName());
	// f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	// JTabbedPane tabs = new JTabbedPane();
	// for (int i = 0; i < 4; i++)
	// tabs.add("Tab " + i, new AddYourOwnURL("Instance " + i));
	//
	// f.setContentPane(tabs);
	// f.setSize(400, 600);
	// f.setVisible(true);
	// }
}
