package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;

import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 * Written/modified by alvin alexander, devdaily.com, and Elliott McCrory, Fermilab.
 * 
 */
public class FileMenu extends JMenuBar {

	private static final long	serialVersionUID	= -7840165773097885222L;

	public static final String	REFRESH_MENU			= "RefreshMenuItem";
	public static final String	INFO_BUTTON_MENU		= "InfoButtonMenuItem";
	public static final String	HELP_MENU				= "HelpMenuItem";
	public static final String	CHECK_NEW_VERSION_MENU	= "CheckMenuItem";

	private JMenu				fileMenu;
	private JMenuItem			refreshMenuItem;
	private JMenuItem			saveRestoreMenuItem;
	private JMenuItem			buttonInfoMenuItem;
	private JMenuItem			helpMenuItem;
	private JMenuItem			checkMenuItem;

	/**
	 * 
	 * @param al
	 *            The action listener for three of the menu items
	 * @param sr
	 *            The action listener for the "Save/Restore" menu item
	 */
	public FileMenu(ActionListener al, ActionListener sr) {
		int size1 = 18;
		int size2 = 18;
		if (!SHOW_IN_WINDOW) {
			size1 = 24;
			size2 = 32;
		}
		Font firstFont = new Font("Arial", Font.PLAIN, size1);
		Font secondFont = new Font("Arial", Font.BOLD, size2);
		// build the File menu
		fileMenu = new JMenu("File");
		refreshMenuItem = new JMenuItem("Refresh the channels from the database");
		saveRestoreMenuItem = new JMenuItem("Open Save/restore dialog box");
		buttonInfoMenuItem = new JMenuItem("Cycle through text on channel buttons");
		helpMenuItem = new JMenuItem("Help");
		checkMenuItem = new JMenuItem("Check for new version of software");

		refreshMenuItem.setActionCommand(REFRESH_MENU);
		buttonInfoMenuItem.setActionCommand(INFO_BUTTON_MENU);
		helpMenuItem.setActionCommand(HELP_MENU);
		checkMenuItem.setActionCommand(CHECK_NEW_VERSION_MENU);

		fileMenu.setFont(firstFont);
		refreshMenuItem.setFont(secondFont);
		saveRestoreMenuItem.setFont(secondFont);
		buttonInfoMenuItem.setFont(secondFont);
		helpMenuItem.setFont(secondFont);
		checkMenuItem.setFont(secondFont);

		saveRestoreMenuItem.addActionListener(sr);
		refreshMenuItem.addActionListener(al);
		buttonInfoMenuItem.addActionListener(al);
		helpMenuItem.addActionListener(al);
		checkMenuItem.addActionListener(al);

		if (!SHOW_IN_WINDOW) {
			int sz = 7;
			refreshMenuItem.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(sz, sz, sz, sz),
					refreshMenuItem.getBorder()));
			saveRestoreMenuItem.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(sz, sz, sz, sz),
					saveRestoreMenuItem.getBorder()));
			buttonInfoMenuItem.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(sz, sz, sz, sz),
					buttonInfoMenuItem.getBorder()));
			checkMenuItem.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(sz, sz, sz, sz),
					checkMenuItem.getBorder()));
			helpMenuItem.setBorder(
					BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(sz, sz, sz, sz), helpMenuItem.getBorder()));
		}

		fileMenu.add(refreshMenuItem);
		fileMenu.add(saveRestoreMenuItem);
		fileMenu.add(buttonInfoMenuItem);
		fileMenu.add(checkMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(helpMenuItem);

		// add menus to menubar
		add(fileMenu);
	}
}