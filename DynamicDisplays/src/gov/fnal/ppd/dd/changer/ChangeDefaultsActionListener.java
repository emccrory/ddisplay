package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.util.Util.println;

import java.awt.Container;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

		f1.setContentPane(SaveRestoreDefaultChannels.getGUI());
		int height = 400 + 30 * (displayList.size() + 1);
		if (height > 1000)
			height = 1000;
		println(ChannelSelector.class, " -- Height of popup is " + height);
		f1.setSize(900, height);
		f1.setAlwaysOnTop(true);
		f1.setVisible(true);
		// f1.addWindowListener(new WindowListener() {
		//
		// @Override
		// public void windowOpened(WindowEvent e) {
		// }
		//
		// @Override
		// public void windowIconified(WindowEvent e) {
		// visible = false;
		// }
		//
		// @Override
		// public void windowDeiconified(WindowEvent e) {
		// visible = true;
		// }
		//
		// @Override
		// public void windowDeactivated(WindowEvent e) {
		// }
		//
		// @Override
		// public void windowClosing(WindowEvent e) {
		// }
		//
		// @Override
		// public void windowClosed(WindowEvent e) {
		// visible = false;
		// f1.setVisible(false);
		// }
		//
		// @Override
		// public void windowActivated(WindowEvent e) {
		// }
		// });
		// }
		// visible = !visible;
		// f1.setVisible(visible);
		// if (visible) {
		// f1.toFront();
		// f1.repaint();
		// }
	}
}
