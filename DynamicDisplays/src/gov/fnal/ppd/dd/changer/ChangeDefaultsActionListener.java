package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.util.Util.println;

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

import gov.fnal.ppd.dd.ChannelSelector;

public class ChangeDefaultsActionListener implements ActionListener {
	private JDialog	f1;
	private Container	parent;

	public ChangeDefaultsActionListener(Container parent) {
		this.parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		Window parentWindow = SwingUtilities.windowForComponent(parent); 
		Frame parentFrame = null;
		if (parentWindow instanceof Frame) {
		    parentFrame = (Frame)parentWindow;
		}	
		
		f1 = new JDialog(parentFrame, "Save / Restore Channels on all Displays", true);

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
				f1.setVisible(false);				
			}
		});
		f1.setContentPane(all);
		int height = 400 + 30 * (displayList.size() + 1);
		if (height > 1000)
			height = 1000;
		println(ChannelSelector.class, " -- Height of popup is " + height);
		f1.setSize(900, height);
		f1.setAlwaysOnTop(true);
		f1.setVisible(true);
	}
}
