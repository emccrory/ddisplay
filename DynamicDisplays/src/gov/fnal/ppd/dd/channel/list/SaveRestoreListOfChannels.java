package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelListHolder;
import gov.fnal.ppd.dd.db.ListUtilsDatabase;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.guiUtils.ListSelectionForSaveList;
import gov.fnal.ppd.dd.util.guiUtils.ListSelectionForSaveList.SpecialLocalListChangeListener;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Deal with the saving and restoring of a list of channels to/from the database
 * 
 * FIXME - Mixes GUI elements with business logic elements
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2017-18
 * 
 */
public class SaveRestoreListOfChannels extends JPanel implements ActionListener {
	private static final long		serialVersionUID		= 6597882652927783449L;

	private static final String		SAVE_LIST_COMMAND		= "SAVE LIST";
	private static final String		RESTORE_LIST_COMMAND	= "RESTORE LIST";
	private static final String		DELETE_LIST_COMMAND		= "DELETE LIST";

	protected String				defaultName				= System.getProperty("user.name");
	protected String				lastListName			= "A list name";

	private JTextArea				listOfChannelsArea		= new JTextArea(50, 40);
	private JScrollPane				internalScrollPanel		= new JScrollPane(listOfChannelsArea);

	private List<ChannelInList>		channelListHolder		= new ArrayList<ChannelInList>();
	private ChannelListHolder		theListOfAllChannels;
	private NewListCreationListener	newListListener			= null;
	protected int					lastListNumber;

	/**
	 * @param clh
	 *            The object that knows where we are
	 * 
	 */
	public SaveRestoreListOfChannels(ChannelListHolder clh) {
		super(new GridBagLayout());
		theListOfAllChannels = clh;
		initComponents();
		listOfChannelsArea.setEditable(false);
		listOfChannelsArea.setBorder(BorderFactory.createTitledBorder(" The channels in this list "));
	}

	// ------------------------------------------------------------------------------------------------------
	// -------------------- GUI Logic methods
	// ------------------------------------------------------------------------------------------------------

	private void doRestoreList() {
		int userValue;
		try {
			userValue = JOptionPane.showConfirmDialog(this, getExistingListsComponent(), "Restore a channel list",
					JOptionPane.OK_CANCEL_OPTION);

			if (userValue == JOptionPane.OK_OPTION) {
				theListOfAllChannels.clear();

				for (ChannelInList C : channelListHolder) {
					theListOfAllChannels.channelAdd(C);
				}
				theListOfAllChannels.fix();

			} else if (userValue == JOptionPane.CANCEL_OPTION) {
				// Nothing to do
			}
		} catch (HeadlessException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void doDeleteList(String listName, int listNum) {
		try {
			Object[] options = { "DELETE THIS LIST", "Cancel" };

			if (JOptionPane.showOptionDialog(this, getExistingListsComponent(), "DELETE a channel list",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]) == JOptionPane.OK_OPTION) {
				// Delete this list. Get confirmation!
				if (JOptionPane.showConfirmDialog(this, "Really delete list '" + listName + "' (List number " + listNum
						+ ")?", "Really delete?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					// Delete the list here.
					ListUtilsDatabase.deleteListFromDatabase(listNum);
				}

				theListOfAllChannels.fix();

			} else {
				// Nothing to do
			}
		} catch (HeadlessException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return The name of the most recent list that was saved or restored.
	 */
	public String getListName() {
		return lastListName;
	}

	// ------------------------------------------------------------------------------------------------------
	// -------------------- GUI Setup and Reaction Methods --------------------
	// ------------------------------------------------------------------------------------------------------

	private void initComponents() {
		JButton saveIt = new JButton("Save this list of channels");
		saveIt.setActionCommand(SAVE_LIST_COMMAND);
		saveIt.addActionListener(this);

		JButton restoreOne = new JButton("Retrieve a list of channels");
		restoreOne.setActionCommand(RESTORE_LIST_COMMAND);
		restoreOne.addActionListener(this);

		JButton deleteOne = new JButton("Entirely delete a list of channels");
		deleteOne.setActionCommand(DELETE_LIST_COMMAND);
		deleteOne.addActionListener(this);
		// deleteOne.setEnabled(false);

		GridBagConstraints gbag = new GridBagConstraints();

		setAlignmentX(CENTER_ALIGNMENT);
		JLabel lab = new JLabel("Archiving, restoring, and deleting saved channel lists");
		lab.setAlignmentX(CENTER_ALIGNMENT);
		gbag.gridx = gbag.gridy = 1;
		gbag.gridwidth = 3;
		gbag.anchor = GridBagConstraints.CENTER;
		// gbag.fill = GridBagConstraints.HORIZONTAL;
		gbag.insets = new Insets(10, 10, 10, 10);
		add(lab, gbag);

		gbag.gridy++;
		gbag.gridwidth = 1;
		add(saveIt, gbag);
		gbag.gridx = 2;
		add(restoreOne, gbag);
		gbag.gridx = 3;
		add(deleteOne, gbag);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5),
				BorderFactory.createLineBorder(Color.black)));

		if (!SHOW_IN_WINDOW) {
			float big = 18;
			saveIt.setFont(saveIt.getFont().deriveFont(big));
			restoreOne.setFont(restoreOne.getFont().deriveFont(big));
			deleteOne.setFont(deleteOne.getFont().deriveFont(big));
			lab.setFont(lab.getFont().deriveFont(24.0f));
		} else {
			lab.setFont(lab.getFont().deriveFont(16.0f));
		}
	}

	protected JComponent getExistingListsComponent() throws Exception {
		JList<String> list = new JList<String>(ListUtilsDatabase.getExistingList().toArray(new String[0]));
		ListSelectionForSaveList retval = new ListSelectionForSaveList(list, listOfChannelsArea);

		retval.addListener(new SpecialLocalListChangeListener() {

			@Override
			public void setListValue(int listValue) {
				channelListHolder.clear();
				listOfChannelsArea.setText("");
				List<ChannelInList> cil = ListUtilsDatabase.getThisList(listValue);
				if (cil != null) {
					long totalDwell = 0;
					for (ChannelInList cih : cil) {
						internalScrollPanel.add(new JLabel(cih.toString()));
						channelListHolder.add(cih);
						listOfChannelsArea.append(cih + "\n");
						totalDwell += cih.getTime();
					}

					DecimalFormat myFormatter = new DecimalFormat("#.0");
					String output = myFormatter.format(totalDwell / 1000.0 / 60.0);
					listOfChannelsArea.append("\nTotal duration of this list: " + totalDwell + " msec (" + output + " minutes)\n");

					lastListNumber = listValue;
					lastListName = ListUtilsDatabase.getLastListName();
				}
			}
		});

		return retval;
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		if (ev.getActionCommand().equals(SAVE_LIST_COMMAND)) {
			String errMess;
			if ((errMess = doSaveList()) != null) {
				if (!errMess.equals(CreateListOfChannels.CANCELLED))
					JOptionPane.showMessageDialog(this, errMess);
				return;
			}
		} else if (ev.getActionCommand().equals(RESTORE_LIST_COMMAND)) {
			doRestoreList();
			if (channelListHolder.size() == 0) {
				JOptionPane.showMessageDialog(this, "There is no channel list in the database with the name you specified");
				return;
			}
		} else if (ev.getActionCommand().equals(DELETE_LIST_COMMAND)) {
			doDeleteList(lastListName, lastListNumber);
		}
	}

	private String doSaveList() {
		if (theListOfAllChannels.getList().size() <= 1) {
			return "The list needs at least two elements!";
		}

		JPanel inside = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.ipadx = 5;
		gbc.ipady = 5;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;

		gbc.gridx = gbc.gridy = 1;

		JTextArea area = null;
		try {
			List<String> L = ListUtilsDatabase.getExistingList();

			area = new JTextArea(L.size() - 1, 60);
			area.setEditable(false);
			area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
			area.setBorder(BorderFactory.createLineBorder(Color.black));

			JLabel le = new JLabel("Existing channel lists in the database:");
			inside.add(le, gbc);
			gbc.gridy++;

			for (String S : L)
				area.append(" " + S + " \n");

			if (L.size() > 10)
				inside.add(new JScrollPane(area), gbc);
			else
				inside.add(area, gbc);
			gbc.gridy++; // Keep track of how many rows we are adding

		} catch (Exception e) {
			e.printStackTrace();
			return "Exception encountered while building list";
		}

		JLabel title = new JLabel("There " + (theListOfAllChannels.getList().size() == 1 ? "is " : "are ")
				+ theListOfAllChannels.getList().size() + " channel" + (theListOfAllChannels.getList().size() != 1 ? 's' : "")
				+ " in the list that is about to be saved:");
		inside.add(title, gbc);

		int longest = 0;
		for (SignageContent SC : theListOfAllChannels.getList()) {
			Channel C = (Channel) SC;
			int s = ("(Chan #) " + C.getNumber() + C.getName()).length();
			if (s > longest)
				longest = s;
		}

		area = new JTextArea(theListOfAllChannels.getList().size() - 1, longest + 20);
		area.setEditable(false);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		area.setBorder(BorderFactory.createLineBorder(Color.black));

		int seq = 1;
		String space = "";
		for (ChannelInList C : theListOfAllChannels.getList()) {
			seq = C.getSequenceNumber();
			if (seq < 10 && theListOfAllChannels.getList().size() > 9)
				space = " ";
			else
				space = "";
			area.append(" " + space + seq++ + ": " + pad("(Chan #" + C.getNumber() + ") " + C.getName(), longest) + " ["
					+ C.getTime() + " msec] \n");
		}

		gbc.gridy++;
		gbc.gridheight = Math.min(theListOfAllChannels.getList().size(), 5);
		JScrollPane sp = new JScrollPane(area);
		sp.setMaximumSize(new Dimension((longest + 22) * 7 + 10, 200));
		sp.setPreferredSize(new Dimension((longest + 22) * 7 + 10, gbc.gridheight * 20));
		inside.add(sp, gbc);

		final JTextField userName = new JTextField(defaultName, 20);
		final JTextField listName = new JTextField(lastListName, 40);
		listName.setText(lastListName);
		JLabel n = new JLabel("Your name:");
		JLabel m = new JLabel("The name for your list:");

		gbc.gridy += theListOfAllChannels.getList().size() + 1;
		gbc.gridheight = 2;
		inside.add(Box.createRigidArea(new Dimension(40, 40)), gbc);

		gbc.gridy++;
		gbc.gridheight = 1;
		// inside.add(title, gbc);
		gbc.gridwidth = 1;
		gbc.gridx = 1;
		// gbc.gridy++;
		gbc.anchor = GridBagConstraints.EAST;
		inside.add(n, gbc);
		gbc.gridx = 2;
		gbc.anchor = GridBagConstraints.WEST;
		inside.add(userName, gbc);
		gbc.gridx = 1;
		gbc.gridy++;
		gbc.anchor = GridBagConstraints.EAST;
		inside.add(m, gbc);
		gbc.gridx = 2;
		gbc.anchor = GridBagConstraints.WEST;
		inside.add(listName, gbc);

		int userValue = JOptionPane.showConfirmDialog(this, inside, "Save the list", JOptionPane.OK_CANCEL_OPTION);
		if (userValue == JOptionPane.OK_OPTION) {
			defaultName = userName.getText();
			String newListName = listName.getText();
			if (ListUtilsDatabase.doesListExist(newListName)) {
				if (JOptionPane.showConfirmDialog(this, "List called '" + newListName + "' already exists.  Replace?",
						"Replace existing list?", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
					ListUtilsDatabase.deleteChannelsFromDatabaseList(lastListNumber);
					ListUtilsDatabase.saveListToDatabase(newListName, theListOfAllChannels.getList());
				} else {
					return CreateListOfChannels.CANCELLED;
				}
			} else {
				// The list does not exist
				ListUtilsDatabase.saveListToDatabase(newListName, userName.getText(), theListOfAllChannels.getList());
			}
		} else if (userValue == JOptionPane.CANCEL_OPTION) {
			return CreateListOfChannels.CANCELLED;
		}

		// The list was saved. Call the callback to say it should reload
		if (newListListener != null)
			newListListener.newListCreationCallback();
		return null;
	}

	/**
	 * @param newListListener
	 *            the newListListener to set
	 */
	public void setNewListListener(NewListCreationListener newListListener) {
		this.newListListener = newListListener;
	}

	private String pad(String name, int longest) {
		if (name.length() == longest)
			return name;
		String retval = name;
		for (int i = name.length(); i < longest; i++)
			retval += " ";
		return retval;
	}

}