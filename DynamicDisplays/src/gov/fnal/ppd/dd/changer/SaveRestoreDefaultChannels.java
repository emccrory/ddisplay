package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.SINGLE_IMAGE_DISPLAY;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getContentOnDisplays;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import static gov.fnal.ppd.dd.db.DisplayUtilDatabase.getDisplayContent;
import static gov.fnal.ppd.dd.db.DisplayUtilDatabase.saveDefaultChannels;
import static gov.fnal.ppd.dd.db.ListUtilsDatabase.getSavedLists;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.plaf.FontUIResource;

import gov.fnal.ppd.dd.db.NoSuchDisplayException;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.guiUtils.SplashScreens;
import gov.fnal.ppd.dd.util.nonguiUtils.CredentialsNotFoundException;
import gov.fnal.ppd.dd.util.specific.CheckDisplayStatus;
import gov.fnal.ppd.dd.xml.ChannelSpec;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class SaveRestoreDefaultChannels implements ActionListener {

	private static final String					SAVE_ACTION		= "Save";
	private static final String					STATUS_ACTION	= "Status";

	private static float						fontSize;
	private static Insets						inset			= null;
	private static String						title			= "Save/Restore Display Configuration";
	private JLabel								footer			= new JLabel("Status ...");

	private List<Display>						displays;

	private static SaveRestoreDefaultChannels	me				= null;

	private static JButton						saveButton;
	private static boolean						firstTime		= true;
	// private static JButton restoreButton;
	private JComponent							theGUI			= null;

	/**
	 * 
	 * Test this class
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		SplashScreens.prepareSaverImages();

		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		selectorSetup();

		title = "Save/Restore a configuration for location=" + getLocationCode();

		JFrame f = new JFrame("SaveRestoreDefaultChannels: " + title);
		SaveRestoreDefaultChannels.setup(null, 30.0f, new Insets(20, 50, 20, 50));
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JComponent x = SaveRestoreDefaultChannels.getGUI();
		x.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED.darker(), 20),
				BorderFactory.createTitledBorder("NOT THE ACTUAL SAVE/RESTORE GUI.")));
		f.setContentPane(x);
		f.setSize(700, 900);
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
	private static void setup() {
		if (firstTime) {
			me = new SaveRestoreDefaultChannels(null);
			firstTime = false;
			if (!SHOW_IN_WINDOW) {
				UIManager.put("OptionPane.buttonFont", new FontUIResource(new Font("ARIAL", Font.PLAIN, 30)));
				UIManager.put("OptionPane.font", new FontUIResource(new Font("Arial", Font.PLAIN, 26)));
			}
		}
		me.localSetupTheGUI();
	}

	/**
	 * @return the button that launches the "identify all" procedure
	 */
	public static JComponent getGUI() {
		setup();
		return me.theGUI;
	}

	private static class AlignedButton extends JButton {
		private static final long serialVersionUID = -2706383316922397811L;

		public AlignedButton(String string) {
			super(string);
			setHorizontalAlignment(CENTER);
			setAlignmentX(CENTER_ALIGNMENT);
		}

	}

	private static class AlignedLabel extends JLabel {
		private static final long serialVersionUID = 3724397217603917724L;

		public AlignedLabel(String string) {
			super(string);
			setFont(getFont().deriveFont(Font.PLAIN));
			setHorizontalAlignment(CENTER);
			setAlignmentX(CENTER_ALIGNMENT);
		}

	}

	private void localSetupTheGUI() {
		// System.out.println(" -- localSetupTheGUI has been called.");
		if (theGUI == null) {
			theGUI = new JPanel(new GridBagLayout());

			saveButton = new AlignedButton("<html>Save this configuration ...</html>");
			saveButton.setActionCommand(SAVE_ACTION);
			saveButton.setToolTipText(
					"Take the channel that each display in this location is showing and save it in the database under a name that you will provide.");
			saveButton.addActionListener(me);

			if (inset != null) {
				saveButton.setFont(saveButton.getFont().deriveFont(fontSize));
				saveButton.setMargin(inset);
				
			}
			
			theGUI.addAncestorListener(new AncestorListener() {

				@Override
				public void ancestorRemoved(AncestorEvent event) {
					// This component (probably) cannot be seen anymore. Probably not interesting
				}

				@Override
				public void ancestorMoved(AncestorEvent event) {
					// This component has moved on the screen. Probably uninteresting.
				}

				@Override
				public void ancestorAdded(AncestorEvent event) {
					// This component has been made visible. This is interesting!
					localSetupTheGUI();
				}
			});
		} else {
			theGUI.removeAll();
		}
		saveButton.setEnabled(false);
		new Thread("waitForDisplays") {
			public void run() {
				while (footer.getText().equals("Status ..."))
					catchSleep(500);
				saveButton.setEnabled(true);
				// restoreButton.setEnabled(true);
			}
		}.start();

		JPanel saveBox = new JPanel(new BorderLayout());
		JLabel rightNow = new JLabel("The configuration of the displays right now:");
		rightNow.setFont(rightNow.getFont().deriveFont(16.0f));
		saveBox.add(rightNow, BorderLayout.NORTH);
		saveBox.add(saveButton, BorderLayout.SOUTH);
		saveBox.add(getStatusFooter(), BorderLayout.CENTER);
		saveBox.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.green.darker(), 5),
				BorderFactory.createEmptyBorder(2, 2, 2, 2)));

		JPanel restorePanel = new JPanel(new BorderLayout());
		JLabel bb = new JLabel("<html>Restore a saved configuration ...</html>");
		bb.setOpaque(true);
		bb.setBackground(Color.blue.darker());
		bb.setForeground(Color.white);
		bb.setFont(new Font("Arial", Font.BOLD, 20));
		restorePanel.add(bb, BorderLayout.NORTH);
		restorePanel.add(getRestorePanel(), BorderLayout.CENTER);
		restorePanel.setBorder(BorderFactory.createLineBorder(Color.blue.darker(), 5));

		GridBagConstraints gbc = new GridBagConstraints(1, 1, 1, 1, 1, 0.1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(1, 1, 1, 1), 5, 5);
		theGUI.add(restorePanel, gbc);
		gbc.gridy = 2;
		gbc.weighty = 1;
		theGUI.add(saveBox, gbc);
		theGUI.invalidate();
		theGUI.validate();
	}

	/**
	 * 
	 */
	private SaveRestoreDefaultChannels(List<Display> d) {
		// Set up a different connection to each display within this class, doubling the number of connections to the messaging
		// server

		if (d == null)
			displays = DisplayListFactory.getInstance(getLocationCode());
		else {
			displays = new ArrayList<Display>();// theGUI.add(saveButton);
			// theGUI.add(getStatusFooter());
			// theGUI.add(restoreButton);
			displays.addAll(d);
		}

		footer = new JLabel("Status ...");
		for (Display D : displays)
			(new CheckDisplayStatus(D, footer)).start();
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

		default:
			try {
				showSelectedRestoreChoice(e.getActionCommand());
			} catch (NoSuchDisplayException e1) {
				// TODO This needs to be seen at a higher level!
				e1.printStackTrace();
			}
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
		String s = JOptionPane.showInputDialog(theGUI, box, "Save Display Configuration", JOptionPane.QUESTION_MESSAGE);
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

		saveDefaultChannels(s);

		new Thread("RewriteSavedLists") {
			public void run() {
				catchSleep(1000);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						localSetupTheGUI();
					}
				});
			}
		}.start();

	}

	protected JComponent getRestorePanel() {
		Vector<String> all = getSavedLists();

		int numColumns = 1;
		int fontSize = 14;
		if (all.size() > 20) {
			numColumns = 5;
			fontSize = 11;
		} else if (all.size() > 15) {
			numColumns = 4;
			fontSize = 11;
		} else if (all.size() > 12) {
			numColumns = 3;
			fontSize = 12;
		} else if (all.size() > 7) {
			numColumns = 2;
		}
		JPanel buts = new JPanel(new GridLayout(0, numColumns, 5, 5));
		buts.setBackground(Color.blue.darker());

		Font big = new Font("Arial", Font.BOLD, fontSize);
		for (String S : all) {
			JButton b = new JButton("<html>" + S + "</html>");
			b.setActionCommand(S);
			b.setBackground(new Color(220, 220, 255));
			if (S.length() > 80) {
				b.setFont(big.deriveFont(2 * fontSize / 3));
			} else {
				b.setFont(big);
			}
			buts.add(b);
			b.addActionListener(this);
		}

		JPanel p = new JPanel();
		p.setOpaque(true);
		p.setBackground(Color.blue.darker());
		p.add(buts);
		return p;

	}

	private void showSelectedRestoreChoice(final String setName) throws NoSuchDisplayException {
		Map<Display, SignageContent> restoreMap = getDisplayContent(setName);

		if (restoreMap.size() == 0) {
			// Nothing to to!
		} else {
			Box theRestorePanel = Box.createVerticalBox();
			Display[] sorted = getSortedDisplays(restoreMap.keySet());

			for (Display D : sorted) {
				String contentString = "<html><pre>Display " + z(D.getVirtualDisplayNumber()) + " (" + z(D.getDBDisplayNumber())
						+ "):  " + signageNameHelper(restoreMap.get(D)) + "</pre></html>";
				int max = 120;
				if (contentString.length() > max + 20) {
					for (; max < contentString.length() - "</pre></html>".length() - 10; max += 120)
						contentString = contentString.substring(0, max) + "\n                  " + contentString.substring(max);
				}
				JLabel label = new JLabel(contentString);
				theRestorePanel.add(label);
			}
			if ((JOptionPane.showConfirmDialog(theGUI, theRestorePanel, "Really restore these displays to these channels?",
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

	private static String signageNameHelper(SignageContent s) {
		String retval = s.toString();
		if (s instanceof ChannelSpec) {
			if (s.toString().contains(SINGLE_IMAGE_DISPLAY)) {
				retval = s.getName();
			} else {
				retval = "\"" + s.getName() + "\" " + s.toString();
			}
		}
		int maxLen = 120;
		if (retval.length() > maxLen) {
			retval = retval.substring(0, maxLen - 2) + " \u2026";
		}
		return retval;
	}

	private static String z(int n) {
		if (n < 10)
			return "0" + n;
		return "" + n;
	}

	private static Display[] getSortedDisplays(Set<Display> keySet) {
		Display[] sorted = keySet.toArray(new Display[keySet.size()]);
		Arrays.sort(sorted, new Comparator<Display>() {

			@Override
			public int compare(Display o1, Display o2) {
				if (o1 == o2)
					return 0;
				if (o1 == null)
					return -1;
				if (o2 == null)
					return 1;
				Integer d1 = o1.getVirtualDisplayNumber();
				Integer d2 = o2.getVirtualDisplayNumber();
				return d1.compareTo(d2);
			}
		});
		return sorted;
	}

	protected Container getStatusFooter() {
		final JPanel outer = new JPanel();

		outer.add(new AlignedLabel("Initializing ..."));
		Timer timer = new Timer();

		TimerTask taskToRedrawDisplayStatusPanel = new TimerTask() {
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
					status.add(new AlignedLabel("No display statuses at " + new Date()), constraints);
				} else {
					Display[] sorted = getSortedDisplays(h.keySet());

					for (Display D : sorted) {
						if (D == null)
							continue;
						constraints.gridx = 1;
						status.add(
								new AlignedLabel("Display " + D.getVirtualDisplayNumber() + " (" + D.getDBDisplayNumber() + "):  "),
								constraints);
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
					sp.setPreferredSize(new Dimension(850, 24 * h.size() + 40));
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
		timer.schedule(taskToRedrawDisplayStatusPanel, 1000, 2000); // Wait one second and the run it every 2 seconds

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
