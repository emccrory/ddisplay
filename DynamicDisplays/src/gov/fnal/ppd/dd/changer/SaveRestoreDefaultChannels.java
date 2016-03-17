package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.PROGRAM_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getContentOnDisplays;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.prepareSaverImages;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.CheckDisplayStatus;

import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class SaveRestoreDefaultChannels implements ActionListener {

	private static final String					SAVE_ACTION		= "Save";
	private static final String					RESTORE_ACTION	= "Restore";
	private static final String					STATUS_ACTION	= "Status";

	private static float						fontSize;
	private static Insets						inset			= null;
	private static String						title			= "Save/Restore Display Configuration";
	private JLabel								footer			= new JLabel("Status ...");

	private List<Display>						displays;

	private static SaveRestoreDefaultChannels	me;

	/**
	 * Add all the displays to the
	 * 
	 * @param d
	 */
	public static void setListOfDisplays(List<Display> d) {
		me = new SaveRestoreDefaultChannels(d);
	}

	/**
	 * Setup the size of the button you want. Don't call this and you'll get a default button size
	 * 
	 * @param title
	 * @param fontSize
	 * @param inset
	 */
	public static void setup(final String title, final float fontSize, final Insets inset) {
		if (title != null)
			SaveRestoreDefaultChannels.title = title;
		SaveRestoreDefaultChannels.fontSize = fontSize;
		SaveRestoreDefaultChannels.inset = inset;
		CheckDisplayStatus.setDoDisplayButtons(false);
	}

	/**
	 * @return the button that launches the "identify all" procedure
	 */
	public static JComponent getGUI() {
		if (me == null)
			me = new SaveRestoreDefaultChannels(null);

		Box ret = Box.createVerticalBox();

		JButton button = new JButton("<html>Save this configuration ...</html>");
		button.setActionCommand(SAVE_ACTION);
		button.setToolTipText("No tip");
		if (inset != null) {
			button.setFont(button.getFont().deriveFont(fontSize));
			button.setMargin(inset);
		}
		button.addActionListener(me);
		ret.add(button);

		button = new JButton("<html>Restore a configuration ...</html>");
		button.setActionCommand(RESTORE_ACTION);
		button.setToolTipText("No tip");
		if (inset != null) {
			button.setFont(button.getFont().deriveFont(fontSize));
			button.setMargin(inset);
		}
		button.addActionListener(me);
		ret.add(button);

		button = new JButton("<html>What is showing now?</html>");
		button.setActionCommand(STATUS_ACTION);
		button.setToolTipText("No tip");
		if (inset != null) {
			button.setFont(button.getFont().deriveFont(fontSize));
			button.setMargin(inset);
		}
		button.addActionListener(me);
		ret.add(button);

		ret.add(me.footer);
		return ret;
	}

	/**
	 * 
	 */
	private SaveRestoreDefaultChannels(List<Display> d) {
		// Set up a different connection to each display within this class, doubling the number of connections to the messaging
		// server
		PROGRAM_NAME = "Save/Restore Display Configuration";

		final SignageType sType = (IS_PUBLIC_CONTROLLER ? SignageType.Public : SignageType.XOC);

		if (d == null)
			displays = DisplayListFactory.getInstance(sType, getLocationCode());
		else {
			displays = new ArrayList<Display>();
			displays.addAll(d);
		}

		footer = new JLabel("Status ...");
		for (Display D : displays)
			(new CheckDisplayStatus(D, footer)).start();
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		prepareSaverImages();

		credentialsSetup();

		selectorSetup();

		title = "Save/Restore a configuration for location=" + getLocationCode();

		JFrame f = new JFrame(SaveRestoreDefaultChannels.class.getSimpleName());
		SaveRestoreDefaultChannels.setup(null, 30.0f, new Insets(20, 50, 20, 50));
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(SaveRestoreDefaultChannels.getGUI());
		f.pack();
		f.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFrame f;
		switch (e.getActionCommand()) {
		case SAVE_ACTION:
			new Thread("SaveDisplayConfiguration") {
				public void run() {
					JFrame f = new JFrame("Save configuration of the displays");
					f.setContentPane(getSavePanel());
					f.pack();
					f.setVisible(true);
				}
			}.start();

			break;

		case RESTORE_ACTION:
			new Thread("RestoreDisplayConfiguration") {
				public void run() {
					JFrame f = new JFrame("Restore a configuration of the displays");
					f.setContentPane(getRestorePanel());
					f.pack();
					f.setVisible(true);
				}
			}.start();
			break;

		case STATUS_ACTION:
			new Thread("ShowAllDisplays") {
				public void run() {
					JFrame f = new JFrame("Status of displays");
					f.setContentPane(getStatusPanel());
					f.pack();
					f.setVisible(true);
				}
			}.start();
			break;

		}
	}

	protected Container getSavePanel() {
		JTextArea text = new JTextArea("Status of the displays at " + new Date() + "\n", 30, 80);
		ListOfExistingContent h = getContentOnDisplays();
		for (Display D : h.keySet()) {
			text.append(D + " : " + h.get(D) + "\n");
		}
		return new JScrollPane(text);
	}

	protected Container getRestorePanel() {
		JTextArea text = new JTextArea("Status of the displays at " + new Date() + "\n", 30, 80);
		ListOfExistingContent h = getContentOnDisplays();
		for (Display D : h.keySet()) {
			text.append(D + " : " + h.get(D) + "\n");
		}
		return new JScrollPane(text);
	}

	protected Container getStatusPanel() {
		JTextArea text = new JTextArea("Status of the displays at " + new Date() + "\n", 30, 80);
		ListOfExistingContent h = getContentOnDisplays();
		for (Display D : h.keySet()) {
			text.append(D + " : " + h.get(D) + "\n");
		}
		return new JScrollPane(text);
	}
}
