package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.util.Util.catchSleep;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

public class ChangeDefaultsActionListener implements ActionListener {
	private JDialog		saveRestoreDialogWindow	= null;
	private Container	parent;

	public ChangeDefaultsActionListener(Container parent) {
		this.parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Window parentWindow = SwingUtilities.windowForComponent(parent);
		Frame parentFrame = null;
		if (parentWindow instanceof Frame) {
			parentFrame = (Frame) parentWindow;
		}

		saveRestoreDialogWindow = new JDialog(parentFrame, "Save / Restore Channels on all Displays", true);

		Box all = Box.createVerticalBox();
		all.add(SaveRestoreDefaultChannels.getGUI());
		JButton button = new JButton("<html><i>Dismiss this window</i></html>");
		button.setFont(button.getFont().deriveFont(24.0f));
		button.setHorizontalAlignment(JButton.CENTER);
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.setMargin(new Insets(20, 50, 20, 50));
		all.add(button);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (saveRestoreDialogWindow != null) {
					saveRestoreDialogWindow.setVisible(false);
					saveRestoreDialogWindow = null;
				}
			}
		});
		// Automatically dismiss this window after it is clear that the user has lost interest in it. Let's say that is 20 minutes.
		// (What seems to happen most, when the user gets bored, is that the window gets covered by the GUI. Not sure how this can
		// happen since it is supposed to be always on top. Whatever.)
		new Thread("Dismiss S/R Popup") {
			public void run() {
				catchSleep(ONE_MINUTE);
				if (saveRestoreDialogWindow != null) {
					saveRestoreDialogWindow.setVisible(false);
					saveRestoreDialogWindow = null;
				}
			}
		}.start();
		saveRestoreDialogWindow.setContentPane(all);
		int height = 400 + 30 * (displayList.size() + 1);
		if (height > 1000)
			height = 1000;
		// println(ChannelSelector.class, " -- Height of popup is " + height);
		// TODO - It would be appropriate to launch a screen-based keyboard.  But I have not found a consistent way to do this.
		saveRestoreDialogWindow.setSize(900, height);
		saveRestoreDialogWindow.setAlwaysOnTop(true);
		saveRestoreDialogWindow.setVisible(true);
	}
	
	
	
}
