package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.PROGRAM_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getContentOnDisplays;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.prepareSaverImages;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.convertObjectToHexBlob;
import static gov.fnal.ppd.dd.util.Util.getChannelFromNumber;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.CheckDisplayStatus;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

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
	private Connection							connection;

	private static SaveRestoreDefaultChannels	me;
	private static JButton						saveButton;
	private static JButton						restoreButton;
	private JComponent							theGUI;

	/**
	 * 
	 * Test this class
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		prepareSaverImages();

		credentialsSetup();

		selectorSetup();

		title = "Save/Restore a configuration for location=" + getLocationCode();

		JFrame f = new JFrame("SaveRestoreDefaultChannels: " + title);
		SaveRestoreDefaultChannels.setup(null, 30.0f, new Insets(20, 50, 20, 50));
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(SaveRestoreDefaultChannels.getGUI());
		f.setSize(700, 445);
		f.setVisible(true);

		// height = 205 + 20*(numDisplays+1)
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
		setup();
	}

	/**
	 * 
	 */
	public static void setup() {
		if (me != null)
			return;
		me = new SaveRestoreDefaultChannels(null);
		me.getTheGUI();

		if (!SHOW_IN_WINDOW) {
			UIManager.put("OptionPane.buttonFont", new FontUIResource(new Font("ARIAL", Font.PLAIN, 30)));
			UIManager.put("OptionPane.font", new FontUIResource(new Font("Arial", Font.PLAIN, 26)));
		}
	}

	/**
	 * @return the button that launches the "identify all" procedure
	 */
	public static JComponent getGUI() {
		if (me == null) {
			setup();
		}
		return me.theGUI;
	}

	private static class AlignedButton extends JButton {
		private static final long	serialVersionUID	= -2706383316922397811L;

		public AlignedButton(String string) {
			super(string);
			setHorizontalAlignment(CENTER);
			setAlignmentX(CENTER_ALIGNMENT);
		}

	}

	private static class AlignedLabel extends JLabel {
		private static final long	serialVersionUID	= 3724397217603917724L;

		public AlignedLabel(String string) {
			super(string);
			setHorizontalAlignment(CENTER);
			setAlignmentX(CENTER_ALIGNMENT);
		}

	}

	private void getTheGUI() {
		theGUI = Box.createVerticalBox();

		saveButton = new AlignedButton("<html>Save this configuration ...</html>");
		saveButton.setActionCommand(SAVE_ACTION);
		saveButton.setToolTipText("No tip");
		if (inset != null) {
			saveButton.setFont(saveButton.getFont().deriveFont(fontSize));
			saveButton.setMargin(inset);
		}
		saveButton.addActionListener(me);
		saveButton.setEnabled(false);
		theGUI.add(saveButton);

		restoreButton = new AlignedButton("<html>Restore a configuration ...</html>");
		restoreButton.setActionCommand(RESTORE_ACTION);
		restoreButton.setToolTipText("No tip");
		if (inset != null) {
			restoreButton.setFont(restoreButton.getFont().deriveFont(fontSize));
			restoreButton.setMargin(inset);
		}
		restoreButton.addActionListener(me);
		restoreButton.setEnabled(false);
		theGUI.add(restoreButton);

		theGUI.add(getStatusFooter());
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

		new Thread("waitForDisplays") {
			public void run() {
				while (footer.getText().equals("Status ..."))
					catchSleep(1000);
				saveButton.setEnabled(true);
				restoreButton.setEnabled(true);
			}
		}.start();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case SAVE_ACTION:
			new Thread("SaveDisplayConfiguration") {
				public void run() {
					getSavePanel();
				}
			}.start();

			break;

		case RESTORE_ACTION:
			new Thread("RestoreDisplayConfiguration") {
				public void run() {
					getRestorePanel();
				}
			}.start();
			break;

		case STATUS_ACTION:
			new Thread("ShowAllDisplays") {
				public void run() {
					JFrame f = new JFrame("Save configuration of the displays");
					f.setContentPane(getStatusPanel());
					f.pack();
					f.setVisible(true);
				}
			}.start();
			break;

		}
	}

	protected void getSavePanel() {
		Box box = Box.createVerticalBox();
		box.add(getStatusPanel());
		box.add(Box.createRigidArea(new Dimension(50, 50)));
		box.add(new JLabel(
				"Please enter a name for this configuration save set, or hit 'cancel' to skip it. (Limit: 255 characters)"));

		Process onscreenKeyboardProcess = null;
		if (!SHOW_IN_WINDOW)
			try {
				onscreenKeyboardProcess = Runtime.getRuntime().exec("osk");
			} catch (IOException e1) {
				if (!e1.getMessage().contains("No such file"))
					e1.printStackTrace();
				else
					println(getClass(), ": No on-screen keyboard available. (Best guess: we're running Linux)");
			}
		String s = (String) JOptionPane.showInputDialog(null, box, "Save Display Configuration", JOptionPane.QUESTION_MESSAGE);
		if (onscreenKeyboardProcess != null)
			onscreenKeyboardProcess.destroy();

		if (s == null || s.length() == 0)
			return;
		System.out.println(s);

		if (s.equalsIgnoreCase("default")) {
			if (JOptionPane.showConfirmDialog(null,
					"Do you really want to overwrite the default, boot-up channels for these displays?", "Really?  Default?",
					JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
				return;
			s = "Default";
		}
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();

			synchronized (connection) {
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("USE " + DATABASE_NAME);

				// TODO -- If the user selects the exact same name for a save set, then we have to delete all these entries first
				// before inserting them.
				// select NameOfThisDefaultSet,LocationCode from DefaultChannels; -- or something like that. Should be "distinct"

				String statementStringStart = "INSERT INTO DefaultChannels VALUES (NULL,";

				ListOfExistingContent h = getContentOnDisplays();
				for (Display D : h.keySet()) {
					String blob = convertObjectToHexBlob(D.getContent());
					String fullStatement = statementStringStart + D.getDBDisplayNumber() + ", \"" + s + "\", 0, x'" + blob + "')";

					int nrows;
					if ((nrows = stmt.executeUpdate(fullStatement)) != 1) {
						println(getClass(), "-- [" + fullStatement + "]\n\t -- expected one row modified; got " + nrows);
					}

				}
				stmt.close();
				rs.close();

			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
		}

	}

	protected void getRestorePanel() {
		Vector<String> all = new Vector<String>();

		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();

			synchronized (connection) {
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("USE " + DATABASE_NAME);

				rs = stmt.executeQuery("SELECT DISTINCT NameOfThisDefaultSet FROM DefaultChannels,DisplaySort WHERE "
						+ "DefaultChannels.DisplayID=DisplaySort.DisplayID AND LocationCode=" + getLocationCode());
				rs.first(); // Move to first returned row
				while (!rs.isAfterLast())
					try {
						String theSetName = rs.getString("NameOfThisDefaultSet");
						all.add(theSetName);
						rs.next();
					} catch (Exception e) {
						e.printStackTrace();
					}
				stmt.close();
				rs.close();
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
		}

		JComboBox<String> combo = new JComboBox<String>(all);
		combo.setFont(new Font("Arial", Font.PLAIN, 30));

		int s = JOptionPane.showConfirmDialog(null, combo, "Restore Display Configuration", JOptionPane.OK_CANCEL_OPTION);
		if (s == JOptionPane.CANCEL_OPTION)
			return;

		String setName = (String) combo.getSelectedItem();
		System.out.println("Ready to retrieve :" + setName);

		// TODO Actually go get these default channels and send it out to the displays

		HashMap<Display, SignageContent> restoreMap = new HashMap<Display, SignageContent>();

		try {
			synchronized (connection) {
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT DefaultChannels.DisplayID as DisplayID,Channel,SignageContent FROM "
						+ "DefaultChannels,DisplaySort WHERE DefaultChannels.DisplayID=DisplaySort.DisplayID AND LocationCode="
						+ getLocationCode() + " AND NameOfThisDefaultSet='" + setName + "'");
				rs.first(); // Move to first returned row
				while (!rs.isAfterLast())
					try {
						int displayID = rs.getInt("DisplayID");
						int chanNum = rs.getInt("Channel");
						SignageContent newContent = null;
						if (chanNum == 0) {
							// Set the display by the SignageContent object
							Blob sc = rs.getBlob("SignageContent");

							if (sc != null && sc.length() > 0)
								try {
									int len = (int) sc.length();
									byte[] bytes = sc.getBytes(1, len);

									ByteArrayInputStream fin = new ByteArrayInputStream(bytes);
									ObjectInputStream ois = new ObjectInputStream(fin);
									newContent = (SignageContent) ois.readObject();

								} catch (Exception e) {
									e.printStackTrace();
								}
						} else {
							newContent = getChannelFromNumber(chanNum);
						}

						Display D;
						if ((D = getContentOnDisplays().get(displayID)) != null) {
							restoreMap.put(D, newContent);
						} else
							System.err.println("Looking for displayID=" + displayID + ", but could not find it!!");

						rs.next();
					} catch (Exception e) {
						e.printStackTrace();
					}
				stmt.close();
				rs.close();
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(1);
		}

		if (restoreMap.size() == 0) {
			// Nothing to to!
		} else {
			Box theRestorePanel = Box.createVerticalBox();
			Display[] sorted = getSortedDisplays(restoreMap.keySet());

			for (Display D : sorted) {
				JLabel label = new JLabel("Display " + D.getVirtualDisplayNumber() + " | " + D.getDBDisplayNumber() + " :  "
						+ restoreMap.get(D));
				theRestorePanel.add(label);
			}
			if ((JOptionPane.showConfirmDialog(null, theRestorePanel, "Really restore these displays to these channels?",
					JOptionPane.YES_NO_OPTION)) == JOptionPane.NO_OPTION)
				return;
			for (Display D : restoreMap.keySet()) {
				SignageContent newContent = restoreMap.get(D);
				println(getClass(), " -- Setting display " + D.getDBDisplayNumber() + " to [" + newContent + "] ("
						+ newContent.getClass().getSimpleName() + ")");
				D.setContent(newContent);
			}
		}
	}

	private static Display[] getSortedDisplays(Set<Display> keySet) {
		Display[] sorted = (Display[]) keySet.toArray(new Display[keySet.size()]);
		Arrays.sort(sorted, new Comparator<Display>() {

			@Override
			public int compare(Display o1, Display o2) {
				Integer d1 = o1.getVirtualDisplayNumber();
				Integer d2 = o2.getVirtualDisplayNumber();
				return d1.compareTo(d2);
			}
		});
		return sorted;
	}

	protected Container getStatusFooter() {
		final JPanel outer = new JPanel();

		outer.setMinimumSize(new Dimension(400, 100));
		outer.setPreferredSize(new Dimension(400, 100));
		outer.add(new AlignedLabel("Initializing ..."));
		Timer timer = new Timer();

		TimerTask g = new TimerTask() {
			public void run() {
				int maxLength = 0;

				JPanel status = new JPanel(new GridBagLayout());
				GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.BASELINE,
						GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 1, 1);

				constraints.gridx = 1;
				constraints.gridy = 0;
				constraints.gridwidth = 2;
				constraints.anchor = GridBagConstraints.WEST;
				status.add(new AlignedLabel("Updated at " + new Date()), constraints);

				constraints.gridx = 1;
				constraints.gridy++;
				constraints.gridwidth = 1;
				ListOfExistingContent h = getContentOnDisplays();

				if (h.size() == 0) {
					status.add(new AlignedLabel("No display statuses"), constraints);
				} else {
					Display[] sorted = getSortedDisplays(h.keySet());

					for (Display D : sorted) {
						constraints.gridx = 1;
						status.add(new AlignedLabel("Display " + D.getVirtualDisplayNumber() + " | " + D.getDBDisplayNumber()
								+ " :  "), constraints);
						constraints.gridx = 2;
						String title = h.get(D).toString();
						if (title.length() > maxLength)
							maxLength = title.length();
						status.add(new AlignedLabel(title), constraints);
						constraints.gridy++;
					}
				}
				outer.removeAll();
				if (h.size() > 4 || maxLength > 50) {
					JScrollPane sp = new JScrollPane(status);
					sp.setPreferredSize(new Dimension(500, 20 * h.size() + 30));
					outer.add(sp);
				} else
					outer.add(status);

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						outer.validate();
						outer.repaint();
						if (me != null && me.theGUI != null) {
							Object comp = me.theGUI;
							while (comp != null && comp instanceof Component) {
								((Component) comp).validate();
								((Component) comp).repaint();
								comp = ((Component) comp).getParent();
							}
						}
					}
				});

			}
		};
		timer.schedule(g, 10000, 5000); // Run it every 5 seconds

		return outer;
	}

	private static Container getStatusPanel() {
		final JPanel outer = new JPanel();

		int maxLength = 0;

		JPanel status = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridwidth = 2;
		constraints.anchor = GridBagConstraints.WEST;
		status.add(new AlignedLabel("Retrieved at " + new Date()), constraints);

		constraints.gridx = 1;
		constraints.gridy++;
		constraints.gridwidth = 1;
		ListOfExistingContent h = getContentOnDisplays();

		Display[] sorted = getSortedDisplays(h.keySet());

		for (Display D : sorted) {
			constraints.gridx = 1;
			status.add(new AlignedLabel("Display " + D.getVirtualDisplayNumber() + " | " + D.getDBDisplayNumber() + " :  "),
					constraints);
			constraints.gridx = 2;
			String title = h.get(D).toString();
			if (title.length() > maxLength)
				maxLength = title.length();
			status.add(new AlignedLabel(title), constraints);
			constraints.gridy++;
		}

		outer.removeAll();
		if (h.size() > 4 || maxLength > 50) {
			JScrollPane sp = new JScrollPane(status);
			// sp.setPreferredSize(new Dimension(500, 20 * h.size() + 30));
			outer.add(sp);
		} else
			outer.add(status);

		return outer;
	}
}
