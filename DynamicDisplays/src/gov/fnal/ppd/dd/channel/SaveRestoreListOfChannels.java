package gov.fnal.ppd.dd.channel;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.channel.list.NewListCreationListener;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.util.ListSelectionForSaveList;
import gov.fnal.ppd.dd.util.ListSelectionForSaveList.SpecialLocalListChangeListener;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
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
 * 
 * Deal with the saving and restoring of a list of channels to/from the database
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class SaveRestoreListOfChannels extends JPanel implements ActionListener {
	// private final CreateListOfChannels theListOfAllChannels;
	protected String				defaultName			= System.getProperty("user.name");
	protected String				lastListName		= "A list name";

	private static final long		serialVersionUID	= 6597882652927783449L;
	private JTextArea				listOfChannelsArea	= new JTextArea(50, 40);
	private JScrollPane				internalScrollPanel	= new JScrollPane(listOfChannelsArea);

	private List<Channel>			channelListHolder	= new ArrayList<Channel>();
	private ChannelListHolder		theListOfAllChannels;
	private NewListCreationListener	newListListener		= null;

	class ChannelInfoHolder {
		public int		channelNumber, sequenceNumber;
		public long		dwell;
		public String	name;

		public ChannelInfoHolder(final int chanNum, final int seqNum, final long dwell, final String name) {
			this.channelNumber = chanNum;
			this.sequenceNumber = seqNum;
			this.dwell = dwell;
			this.name = name;
		}

		public String toString() {
			return sequenceNumber + ": Channel #" + channelNumber + " [ " + name + " ] (" + dwell + " milliseconds)";
		}
	}

	/**
	 * @param clh
	 *            The object that know where we are
	 * 
	 */
	public SaveRestoreListOfChannels(ChannelListHolder clh) {
		super(new GridBagLayout());
		theListOfAllChannels = clh;
		initComponents();
		listOfChannelsArea.setEditable(false);
		listOfChannelsArea.setBorder(BorderFactory.createTitledBorder(" The channels in this list "));
	}

	private void initComponents() {
		JButton saveIt = new JButton("Save this list of channels to the database");
		saveIt.setActionCommand("SAVE");
		saveIt.addActionListener(this);

		JButton restoreOne = new JButton("Retrieve a list of channels from the database");
		restoreOne.setActionCommand("RESTORE");
		restoreOne.addActionListener(this);

		GridBagConstraints gbag = new GridBagConstraints();

		setAlignmentX(CENTER_ALIGNMENT);
		JLabel lab = new JLabel("Archiving and restoring lists");
		gbag.gridx = gbag.gridy = 1;
		gbag.gridwidth = 2;
		gbag.fill = GridBagConstraints.HORIZONTAL;
		gbag.insets = new Insets(1, 1, 1, 1);
		add(lab, gbag);

		gbag.gridy++;
		gbag.gridwidth = 1;
		add(saveIt, gbag);
		gbag.gridx = 2;
		add(restoreOne, gbag);
	}

	protected JComponent getExistingListsComponent() throws Exception {
		JList<String> list = new JList<String>(getExistingList().toArray(new String[0]));
		ListSelectionForSaveList retval = new ListSelectionForSaveList(list, listOfChannelsArea);

		retval.addListener(new SpecialLocalListChangeListener() {

			@Override
			public void setListValue(int listValue) {
				channelListHolder.clear();
				listOfChannelsArea.setText("");
				getThisList(listValue);
			}
		});

		return retval;
	}

	protected void getThisList(int listNumber) {
		Connection connection;
		try {
			connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
			return;
		}
		int totalDwell = 0;

		String query = "Select ChannelList.Number as Number,Name,SequenceNumber,Dwell, URL, Category, Description from ChannelList,Channel "
				+ "WHERE ListNumber=" + listNumber + " AND Channel.Number=ChannelList.Number ORDER BY SequenceNumber";

		synchronized (connection) {
			try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
				try (ResultSet rs2 = stmt.executeQuery(query)) {
					if (rs2.first())
						do {
							int chanNumber = rs2.getInt("Number");
							int seq = rs2.getInt("SequenceNumber");
							long dwell = rs2.getLong("Dwell");
							totalDwell += dwell;
							// TODO -- The abbreviation argument (next line) is wrong.
							ChannelCategory cat = new ChannelCategory(rs2.getString("Category"), "ABB");
							String name = rs2.getString("Name");
							String url = rs2.getString("URL");
							String desc = rs2.getString("Description");
							try {
								URI uri = new URI(url);
								ChannelInfoHolder cih = new ChannelInfoHolder(chanNumber, seq, dwell, name);
								System.out.println(cih);
								internalScrollPanel.add(new JLabel(cih.toString()));
								Channel channel = new ChannelImpl(name, cat, desc, uri, chanNumber, dwell);
								channelListHolder.add(channel);
								CreateListOfChannels.checkContent(channel);
								listOfChannelsArea.append(cih + "\n");
							} catch (URISyntaxException e) {
								e.printStackTrace();
							}

						} while (rs2.next());
					else {
						// Oops. no first element!?
						System.err.println("No elements in the save/restore database for list number " + listNumber + "!");
						return;
					}
				}
			} catch (SQLException e) {
				System.err.println(query);
				e.printStackTrace();
			}
		}
		DecimalFormat myFormatter = new DecimalFormat("#.0");
		String output = myFormatter.format(totalDwell / 1000.0 / 60.0);
		listOfChannelsArea.append("\nTotal duration of this list: " + totalDwell + " msec (" + output + " minutes)\n");
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getActionCommand().equals("SAVE")) {
			String errMess;
			if ((errMess = doSaveList()) != null) {
				if (!errMess.equals(CreateListOfChannels.CANCELLED))
					JOptionPane.showMessageDialog(this, errMess);
				return;
			}
		} else if (arg0.getActionCommand().equals("RESTORE")) {
			doRestoreList();
			if (channelListHolder.size() == 0) {
				JOptionPane.showMessageDialog(this, "There is no channel list in the database with the name you specified");
				return;
			}
		}
	}

	private void doRestoreList() {
		int userValue;
		try {
			userValue = JOptionPane.showConfirmDialog(this, getExistingListsComponent(), "Restore a channel list",
					JOptionPane.OK_CANCEL_OPTION);

			if (userValue == JOptionPane.OK_OPTION) {
				theListOfAllChannels.clear();

				for (Channel C : channelListHolder) {
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
			List<String> L = getExistingList();

			area = new JTextArea(L.size() - 1, 40);
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
		for (SignageContent SC : theListOfAllChannels.getList()) {
			Channel C = (Channel) SC;
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

		final JTextField myName = new JTextField(defaultName, 20);
		final JTextField listName = new JTextField(lastListName, 40);
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
		inside.add(myName, gbc);
		gbc.gridx = 1;
		gbc.gridy++;
		gbc.anchor = GridBagConstraints.EAST;
		inside.add(m, gbc);
		gbc.gridx = 2;
		gbc.anchor = GridBagConstraints.WEST;
		inside.add(listName, gbc);

		int userValue = JOptionPane.showConfirmDialog(this, inside, "Save the list", JOptionPane.OK_CANCEL_OPTION);
		if (userValue == JOptionPane.OK_OPTION)
			try {
				Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
				synchronized (connection) {
					int listNumber = 0;

					String list = lastListName = listName.getText();
					if (list == null || list.length() == 0)
						list = "" + new Date();
					String author = defaultName = myName.getText();

					// The queries needed here:

					String isItThereAlready = "SELECT ListNumber from ChannelListName WHERE ListName='" + list + "'";
					String retrieve = "SELECT ListNumber from ChannelListName WHERE ListName='" + list + "'";
					String deleteExisting = "DELETE FROM ChannelList WHERE ListNumber="; // To be continued
					String insert = "INSERT INTO ChannelListName VALUES (NULL, '" + list + "', '" + author + "')";

					try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {

						// Does this list already exist in the database? If so, warn the user and then overwrite it. Otherwise, just
						// save it.
						try (ResultSet rs2 = stmt.executeQuery(isItThereAlready)) {
							if (rs2.first()) {
								listNumber = rs2.getInt("ListNumber"); // Got the list number.
								if (listNumber > 0) {
									if (JOptionPane.showConfirmDialog(this, "List called '" + list + "' already exists.  Replace?",
											"Replace existing list?", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
										return "User rejected the update";
									}
									// Delete the list and move on.
									println(getClass(), ": Deleting list number " + listNumber);
									stmt.executeUpdate(deleteExisting + listNumber);
								}
							}
						} catch (Exception e) {
							// Not really a problem -- this list does not exist yet.
						}

						if (listNumber == 0) {
							try {
								println(getClass(), ": Creating a new list called '" + list + "'");
								stmt.executeUpdate(insert);

								ResultSet rs2 = stmt.executeQuery(retrieve);
								if (rs2.first())
									listNumber = rs2.getInt(1);

							} catch (Exception e) {
								e.printStackTrace();
								return "Reading the list number, which we just created, failed";
							}
						}

						// Use the exiting channel list from the GUI
						int sequence = 0;
						for (SignageContent SC : theListOfAllChannels.getList()) {
							if (SC instanceof Channel) {
								Channel c = (Channel) SC;
								insert = "INSERT INTO ChannelList VALUES (NULL, " + listNumber + ", " + c.getNumber() + ", "
										+ c.getTime() + ", NULL, " + (sequence++) + ")";
								if (stmt.executeUpdate(insert) == 1) {
									// OK, the insert worked.
									println(getClass(), ": Successful insert " + insert);
								} else {
									new Exception("Insert of list element number " + sequence + " failed").printStackTrace();
									return "Insert of list element number " + sequence + " failed";
								}

							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return "An exception caused this to fail.";
			}
		else if (userValue == JOptionPane.CANCEL_OPTION) {
			return CreateListOfChannels.CANCELLED;
		}

		// The list was saved. Call the callback to say it should reload
		if (newListListener != null)
			newListListener.newListCreationCallback();
		return null;
	}

	private String pad(String name, int longest) {
		if (name.length() == longest)
			return name;
		String retval = name;
		for (int i = name.length(); i < longest; i++)
			retval += " ";
		return retval;
	}

	/**
	 * Get the list of channels that are currently defined.
	 * 
	 * @return A list of strings of these list names
	 * @throws Exception
	 */
	public static List<String> getExistingList() throws Exception {
		List<String> retval = new ArrayList<String>();
		Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
		String query = "Select * from ChannelListName";

		synchronized (connection) {
			try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
				try (ResultSet rs2 = stmt.executeQuery(query)) {
					if (rs2.first())
						do {
							String listName = rs2.getString("ListName");
							String listAuthor = rs2.getString("ListAuthor");
							int listNumber = rs2.getInt("ListNumber");
							retval.add(listNumber + ": " + listName + " (" + listAuthor + ")");
						} while (rs2.next());
					else {
						// Oops. no first element!?
						throw new Exception("No channel lists in the save/restore database!");
					}
				} catch (SQLException e) {
					System.err.println(query);
					e.printStackTrace();
				}
			} catch (SQLException e) {
				System.err.println(query);
				e.printStackTrace();
			}
		}
		return retval;
	}

	/**
	 * @param newListListener
	 *            the newListListener to set
	 */
	public void setNewListListener(NewListCreationListener newListListener) {
		this.newListListener = newListListener;
	}

}