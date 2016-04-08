/*
 * CreateListOfChannels
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameSelector;
import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import gov.fnal.ppd.dd.changer.CategoryDictionary;
import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.util.ListSelectionForSaveList;
import gov.fnal.ppd.dd.util.ListSelectionForSaveList.SpecialLocalListChangeListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Let the user create a list of channels for a display to play. This is used in CreateListOfChannelsHelper as a GUI element that is
 * presented to ChannelSelector
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class CreateListOfChannels extends JPanel {

	private static final long			serialVersionUID	= 2157704848183269779L;
	private static final String			NOT_SELECTED		= "                  ";
	private static final String			CANCELLED			= "** CANCELLED **";

	private List<SignageContent>		channelList			= new ArrayList<SignageContent>();
	private List<JLabel>				labelList			= new ArrayList<JLabel>();
	private JSpinner					time;
	private SaveRestoreListOfChannels	saveRestore;

	protected String					defaultName			= System.getProperty("user.name");
	protected String					lastListName		= "A list name";
	private Map<String, JLabel>			allLabels			= new HashMap<String, JLabel>();
	private Box							timeWidgets			= Box.createVerticalBox();

	static class BigButton extends JButton {
		private static final long	serialVersionUID	= 6517317474435639087L;

		public BigButton(String title) {
			super(title);
			if (!SHOW_IN_WINDOW) {
				setFont(getFont().deriveFont(30.0f));
				setMargin(new Insets(20, 20, 20, 20));
			}
			setAlignmentX(JComponent.CENTER_ALIGNMENT);
		}
	}

	private static class BigLabel extends JLabel {
		private static final long	serialVersionUID	= 8296427549410976741L;

		public BigLabel(String title, int style) {
			super(title);
			setOpaque(true);
			setBackground(Color.white);
			if (!SHOW_IN_WINDOW)
				setFont(new Font("Arial", style, 20));
			setAlignmentX(JComponent.CENTER_ALIGNMENT);
		}
	}

	private class ChannelInfoHolder {
		public int		channelNumber, sequenceNumber;
		public long		dwell;
		public String	name;

		public ChannelInfoHolder(final int cn, final int sn, final long d, final String n) {
			channelNumber = cn;
			sequenceNumber = sn;
			dwell = d;
			name = n;
		}

		public String toString() {
			return sequenceNumber + ": Channel #" + channelNumber + " [ " + name + " ] (" + dwell + " milliseconds)";
		}
	}

	/**
	 * @return the save-restore widget
	 */
	public JPanel getSaveRestore() {
		return saveRestore;
	}

	/**
	 * 
	 * Inner class to deal with the saving and restoring of a list of channels to/from the database
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	private class SaveRestoreListOfChannels extends JPanel implements ActionListener {

		private static final long	serialVersionUID	= 6597882652927783449L;
		private JTextArea			listOfChannelsArea	= new JTextArea(50, 40);
		private JScrollPane			internalScrollPanel	= new JScrollPane(listOfChannelsArea);

		private List<Channel>		channelListHolder	= new ArrayList<Channel>();

		/**
		 * 
		 */
		public SaveRestoreListOfChannels() {
			super(new GridBagLayout());
			initComponents();
			listOfChannelsArea.setEditable(false);
			listOfChannelsArea.setBorder(BorderFactory.createTitledBorder(" The channels in this list "));
		}

		private void initComponents() {
			JButton saveIt = new JButton("Save this list to a file");
			saveIt.setActionCommand("SAVE");
			saveIt.addActionListener(this);

			JButton restoreOne = new JButton("Restore a list from a file");
			restoreOne.setActionCommand("RESTORE");
			restoreOne.addActionListener(this);

			GridBagConstraints gbag = new GridBagConstraints();

			setAlignmentX(CENTER_ALIGNMENT);
			JLabel lab = new JLabel("Archiving and restoring lists");
			gbag.gridx = gbag.gridy = 1;
			gbag.gridwidth = 2;
			gbag.fill = GridBagConstraints.HORIZONTAL;
			add(lab, gbag);

			lab = new JLabel("Just below this text are buttons to save and to restore lists to persistant storage");
			lab.setFont(lab.getFont().deriveFont(Font.PLAIN, 11));
			gbag.gridy++;
			gbag.fill = GridBagConstraints.HORIZONTAL;
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

		// protected JComponent getExistingListsComponent() throws Exception {
		// final JPanel retval = new JPanel(new BorderLayout());
		// List<String> existingLists = getExistingList();
		// listOfChannelsArea.setText("");
		//
		// final JComboBox<String> listComboBox = new JComboBox<String>(existingLists.toArray(new String[0]));
		// listComboBox.addActionListener(new ActionListener() {
		//
		// @Override
		// public void actionPerformed(ActionEvent ev) {
		// channelListHolder.clear();
		// listOfChannelsArea.setText("");
		//
		// String selected = (String) listComboBox.getSelectedItem();
		// int listNumber = Integer.parseInt(selected.substring(0, selected.indexOf(':')));
		//
		// getThisList(listNumber);
		//
		// retval.validate();
		// // retval.repaint();
		// }
		// });
		//
		// internalScrollPanel.setPreferredSize(new Dimension(500, 200));
		// internalScrollPanel.add(new JLabel("Select a list, please"));
		// retval.add(BorderLayout.NORTH, listComboBox);
		// retval.add(BorderLayout.CENTER, internalScrollPanel);
		// return retval;
		// }

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

		private List<String> getExistingList() throws Exception {
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
							throw new Exception("No elements in the save/restore database!");
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

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (arg0.getActionCommand().equals("SAVE")) {
				String errMess;
				if ((errMess = doSaveList()) != null) {
					if (!errMess.equals(CANCELLED))
						JOptionPane.showMessageDialog(SaveRestoreListOfChannels.this, errMess);
					return;
				}
			} else if (arg0.getActionCommand().equals("RESTORE")) {
				doRestoreList();
				if (channelListHolder.size() == 0) {
					JOptionPane.showMessageDialog(SaveRestoreListOfChannels.this,
							"There is no list in the database with the name you specified");
					return;
				}
			}
		}

		private void doRestoreList() {
			int userValue;
			try {
				userValue = JOptionPane.showConfirmDialog(SaveRestoreListOfChannels.this, getExistingListsComponent(),
						"Restore a list", JOptionPane.OK_CANCEL_OPTION);

				if (userValue == JOptionPane.OK_OPTION) {
					channelList.clear();
					labelList.clear();
					for (JLabel L : allLabels.values()) {
						L.setText(NOT_SELECTED);
					}

					for (int seqNum = 0; seqNum < channelListHolder.size(); seqNum++) {
						Channel C = channelListHolder.get(seqNum);
						channelList.add(C);
						JLabel lab = allLabels.get(C.getName());
						lab.setText("" + C.getTime());
						labelList.add(lab);
					}
					fixLabels();

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
			if (channelList.size() <= 1) {
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

				JLabel le = new JLabel("Existing lists in the database:");
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

			JLabel title = new JLabel("There are " + channelList.size() + " channels in the list that is about to be saved:");
			inside.add(title, gbc);

			int longest = 0;
			for (SignageContent SC : channelList) {
				Channel C = (Channel) SC;
				int s = ("(Chan #) " + C.getNumber() + C.getName()).length();
				if (s > longest)
					longest = s;
			}

			area = new JTextArea(channelList.size() - 1, longest + 15);
			area.setEditable(false);
			area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
			area.setBorder(BorderFactory.createLineBorder(Color.black));

			int seq = 1;
			String space = "";
			for (SignageContent SC : channelList) {
				Channel C = (Channel) SC;
				if (seq < 10 && channelList.size() > 9)
					space = " ";
				else
					space = "";
				area.append(" " + space + seq++ + ": " + pad("(Chan #" + C.getNumber() + ") " + C.getName(), longest) + " ["
						+ C.getTime() + " msec] \n");
			}

			gbc.gridy++;
			gbc.gridheight = channelList.size();
			inside.add(area, gbc);

			final JTextField myName = new JTextField(defaultName, 20);
			final JTextField listName = new JTextField(lastListName, 40);
			JLabel n = new JLabel("Your name:");
			JLabel m = new JLabel("The name for your list:");

			gbc.gridy += channelList.size() + 1;
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

			int userValue = JOptionPane.showConfirmDialog(SaveRestoreListOfChannels.this, inside, "Save the list",
					JOptionPane.OK_CANCEL_OPTION);
			if (userValue == JOptionPane.OK_OPTION)
				try {
					Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
					synchronized (connection) {
						int listNumber = 0;

						String list = lastListName = listName.getText();
						String author = defaultName = myName.getText();

						String insert = "INSERT INTO ChannelListName VALUES (NULL, '" + list + "', '" + author + "')";

						String retrieve = "SELECT ListNumber from ChannelListName WHERE ListName='" + list + "' AND ListAuthor='"
								+ author + "'";

						try (Statement stmt = connection.createStatement();
								ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
							if (stmt.executeUpdate(insert) == 1) {
								// OK, the insert worked.
								System.out.println("Successful: " + insert);
							} else {
								new Exception("Got the wrong number of lines inserted into the DB").printStackTrace();
								return "Insert of new list name into the database failed";
							}
							try (ResultSet rs2 = stmt.executeQuery(retrieve)) {
								System.out.println("Successful: " + retrieve);
								if (rs2.first())
									listNumber = rs2.getInt(1);

							} catch (Exception e) {
								e.printStackTrace();
								return "Reading the list number, which we just created, failed";
							}

							// Use the exiting channel list from the GUI
							int sequence = 0;
							for (SignageContent SC : channelList) {
								if (SC instanceof Channel) {
									Channel c = (Channel) SC;
									insert = "INSERT INTO ChannelList VALUES (NULL, " + listNumber + ", " + c.getNumber() + ", "
											+ c.getTime() + ", NULL, " + (sequence++) + ")";
									if (stmt.executeUpdate(insert) == 1) {
										// OK, the insert worked.
										System.out.println("Successful: " + insert);
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
					return "Some exception caused this to fail.";
				}
			else if (userValue == JOptionPane.CANCEL_OPTION) {
				return CANCELLED;
			}
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
	}

	CreateListOfChannels() {
		super();
		saveRestore = new SaveRestoreListOfChannels();
		// alt();
		alt_2();
	}

	@SuppressWarnings("unused")
	private void alt() {
		setLayout(new GridBagLayout());

		GridBagConstraints bag = new GridBagConstraints();
		bag.fill = GridBagConstraints.HORIZONTAL;
		bag.gridx = bag.gridy = 1;
		if (SHOW_IN_WINDOW)
			bag.insets = new Insets(2, 2, 2, 2);
		else
			bag.insets = new Insets(6, 6, 6, 6);
		bag.anchor = GridBagConstraints.CENTER;

		add(Box.createRigidArea(new Dimension(10, 10)), bag);
		bag.gridy++;

		Box bh = Box.createHorizontalBox();
		bh.add(Box.createHorizontalGlue());
		bh.add(new BigLabel("Approximate Dwell time (msec): ", Font.PLAIN));

		final SpinnerModel model = new SpinnerListModel(getDwellStrings());
		time = new JSpinner(model);
		time.setValue(new Long(20000l));
		time.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		if (!SHOW_IN_WINDOW)
			time.setFont(new Font("Monospace", Font.PLAIN, 40));
		bh.add(time);
		final JLabel timeInterpretLabel = new JLabel("20 secs");
		timeInterpretLabel.setFont(new Font(Font.MONOSPACED, Font.ITALIC, 12));
		bh.add(Box.createRigidArea(new Dimension(10, 10)));
		bh.add(timeInterpretLabel);
		time.addChangeListener(new ChangeListener() {
			DecimalFormat	format	= new DecimalFormat("#.0");

			@Override
			public void stateChanged(ChangeEvent e) {
				long val = (Long) model.getValue();
				String t = val + " milliseconds";
				if (val < 60000L) {
					t = (val / 1000) + " sec";
				} else if (val < 3600000) {
					double min = ((double) val) / 60000.0;
					t = format.format(min) + " min";
				} else {
					double hours = ((double) val) / (60 * 60 * 1000.0);
					t = format.format(hours) + " hr";
				}
				timeInterpretLabel.setText(t);
			}
		});
		bh.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		bh.add(Box.createHorizontalGlue());
		timeWidgets.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		timeWidgets.add(Box.createRigidArea(new Dimension(10, 10)));
		timeWidgets.add(bh);

		bag.gridy++;

		ChannelCategory categories[] = CategoryDictionary.getCategories();

		for (ChannelCategory C : categories) {
			bag.gridwidth = 2;
			add(new JSeparator(), bag);
			bag.gridy++;

			String sep = "--------------- " + C + " ----------------";

			for (int len = C.getValue().length(); len < 18; len += 2)
				sep = "-" + sep + "--";

			add(new BigLabel(sep, Font.BOLD), bag);
			bag.gridy++;

			bag.gridwidth = 1;
			Set<SignageContent> chans = ChannelCatalogFactory.getInstance().getChannelCatalog(C);
			for (final SignageContent CONTENT : chans) {
				final JButton b = new BigButton(CONTENT.getName());
				add(b, bag);
				bag.gridx++;
				final JLabel lab = new BigLabel(NOT_SELECTED, Font.ITALIC);
				allLabels.put(CONTENT.getName(), lab);

				add(lab, bag);
				bag.gridx--;
				bag.gridy++;
				b.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						boolean selected = channelList.contains(CONTENT);
						if (selected) {
							while (channelList.remove(CONTENT))
								; // Remove them all
							lab.setText(NOT_SELECTED);
							labelList.remove(lab);
						} else {
							long defaultDwell = (Long) time.getValue();
							long actualDwell = 0;
							if (CONTENT.getTime() == 0 || CONTENT.getTime() > defaultDwell) {
								CONTENT.setTime(defaultDwell);
								channelList.add(CONTENT);
								actualDwell = defaultDwell;
							} else {
								// Simple change here: Based on the internal refresh time of the channel, add 1 or more instances of
								// this channel the the list. E.g., if the user is asking for a dwell of an hour, but the channel
								// has a refresh of 20 minutes, it will put the channel in the list 3 times, at 20 minutes per.

								long tm = defaultDwell;
								for (; tm >= CONTENT.getTime(); tm -= CONTENT.getTime()) {
									channelList.add(CONTENT);
									actualDwell += CONTENT.getTime();
								}

								// TODO -- Add a copy of this channel that gets refreshed at time=(the remaining value of) tm.
							}
							lab.setText("" + actualDwell);
							labelList.add(lab);
						}
						fixLabels();
					}
				});
			}

		}

		bag.gridx++;
		add(Box.createRigidArea(new Dimension(10, 10)), bag);
	}

	private void alt_2() {
		setLayout(new BorderLayout());

		BigLabel bl = new BigLabel("Approximate Dwell time (msec): ", Font.PLAIN);

		final SpinnerModel model = new SpinnerListModel(getDwellStrings());
		time = new JSpinner(model);
		time.setValue(new Long(20000l));
		time.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		if (!SHOW_IN_WINDOW) {
			time.setFont(new Font("Monospace", Font.PLAIN, 40));
			// TODO -- increase the size of the buttons.  It is really complicated (https://community.oracle.com/thread/1357837?start=0&tstart=0) -- later!
		}
		final JLabel timeInterpretLabel = new JLabel("20 secs");
		int ft = (SHOW_IN_WINDOW ? 12 : 20);
		timeInterpretLabel.setFont(new Font(Font.MONOSPACED, Font.ITALIC, ft));

		time.addChangeListener(new ChangeListener() {
			DecimalFormat	format	= new DecimalFormat("#.0");

			@Override
			public void stateChanged(ChangeEvent e) {
				long val = (Long) model.getValue();
				String t = val + " milliseconds";
				if (val < 60000L) {
					t = (val / 1000) + " sec";
				} else if (val < 3600000) {
					double min = ((double) val) / 60000.0;
					t = format.format(min) + " min";
				} else {
					double hours = ((double) val) / (60 * 60 * 1000.0);
					t = format.format(hours) + " hr";
				}
				timeInterpretLabel.setText(t);
			}
		});

		Box bx = Box.createHorizontalBox();
		bx.add(Box.createGlue());
		bx.add(bl);
		bx.add(time);
		bx.add(Box.createRigidArea(new Dimension(10, 10)));
		bx.add(timeInterpretLabel);
		bx.add(Box.createGlue());
		add(bx, BorderLayout.NORTH);

		// ----------------------------------------

		ChannelCategory categories[] = CategoryDictionary.getCategories();
		Box mainPanel = Box.createVerticalBox();
		final Color bg1 = new Color(210, 210, 210);
		final Color bg2 = new Color(230, 230, 230);
		Color bgColor = bg2;

		for (ChannelCategory C : categories) {
			String sep = "--------------- " + C + " ----------------";

			for (int len = C.getValue().length(); len < 18; len += 2)
				sep = "-" + sep + "--";

			BigLabel catLabel = new BigLabel(sep, Font.BOLD);
			if (SHOW_IN_WINDOW)
				catLabel.setFont(new Font("Arial", Font.BOLD, 20));
			mainPanel.add(catLabel);

			int nc = (SHOW_IN_WINDOW ? 3 : 2);
			JPanel inner = new JPanel(new GridLayout(0, nc, 5, 5));

			if (bgColor == bg2)
				bgColor = bg1;
			else
				bgColor = bg2;

			inner.setOpaque(true);
			inner.setBackground(bgColor);
			catLabel.setBackground(bgColor);

			mainPanel.add(inner);
			mainPanel.add(Box.createRigidArea(new Dimension(200, 20)));
			Set<SignageContent> chans = ChannelCatalogFactory.getInstance().getChannelCatalog(C);
			for (final SignageContent CONTENT : chans) {
				final JButton bb = new BigButton(CONTENT.getName());
				if (SHOW_IN_WINDOW)
					bb.setMargin(new Insets(8, 8, 8, 8));

				final JLabel lab = new BigLabel(NOT_SELECTED, Font.ITALIC);

				final Box box = Box.createVerticalBox();
				JPanel p = new JPanel(new BorderLayout(0, 0));
				p.add(bb);
				box.add(p);
				box.add(Box.createRigidArea(new Dimension(5, 5)));
				p = new JPanel(new BorderLayout(0, 0));
				p.add(lab);
				box.add(p);
				box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2),
						BorderFactory.createLineBorder(Color.black)));

				inner.add(box);

				allLabels.put(CONTENT.getName(), lab);
				bb.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						boolean selected = channelList.contains(CONTENT);
						if (selected) {
							while (channelList.remove(CONTENT))
								; // Remove them all
							lab.setText(NOT_SELECTED);
							labelList.remove(lab);
						} else {
							long defaultDwell = (Long) time.getValue();
							long actualDwell = 0;
							if (CONTENT.getTime() == 0 || CONTENT.getTime() > defaultDwell) {
								CONTENT.setTime(defaultDwell);
								channelList.add(CONTENT);
								actualDwell = defaultDwell;
							} else {
								// Simple change here: Based on the internal refresh time of the channel, add 1 or more instances of
								// this channel the the list. E.g., if the user is asking for a dwell of an hour, but the channel
								// has a refresh of 20 minutes, it will put the channel in the list 3 times, at 20 minutes per.

								long tm = defaultDwell;
								for (; tm >= CONTENT.getTime(); tm -= CONTENT.getTime()) {
									channelList.add(CONTENT);
									actualDwell += CONTENT.getTime();
								}

								// TODO -- Add a copy of this channel that gets refreshed at time=(the remaining value of) tm.
							}
							lab.setText("" + actualDwell);
							labelList.add(lab);
						}
						fixLabels();
					}
				});
			}
		}

		add(mainPanel, BorderLayout.CENTER);
	}

	private static List<Long> getDwellStrings() {
		ArrayList<Long> retval = new ArrayList<Long>();

		retval.add(5000l);
		retval.add(7500l);
		retval.add(8000l);
		retval.add(9000l);
		retval.add(10000l); // 10 seconds
		retval.add(12000l);
		retval.add(15000l);
		retval.add(20000l);
		retval.add(30000l);
		retval.add(45000l);
		retval.add(60000l);
		retval.add(75000l);
		retval.add(90000l);
		retval.add(100000l); // 100 seconds
		retval.add(110000l);
		retval.add(120000l); // 2 minutes
		retval.add(150000l);
		retval.add(180000l);
		retval.add(210000l);
		retval.add(240000l);
		retval.add(300000l); // 5 minutes
		retval.add(360000l);
		retval.add(420000l);
		retval.add(480000l);
		retval.add(540000l);
		retval.add(600000l); // 10 minutes
		retval.add(1200000l);
		retval.add(1800000l);
		retval.add(2400000l);
		retval.add(3600000l); // One hour
		retval.add(2 * 3600000l);
		retval.add(3 * 3600000l);
		retval.add(4 * 3600000l);
		retval.add(5 * 3600000l);
		retval.add(6 * 3600000l);
		retval.add(8 * 3600000l); // 8 hours

		return retval;
	}

	protected void fixLabels() {
		int count = 1;
		for (JLabel LAB : labelList) {
			if (!LAB.getText().equals(NOT_SELECTED)) {
				String t = LAB.getText();
				long dwell = 0;
				if (t.startsWith("**")) {
					String c = t.substring(t.indexOf('(') + 1, t.indexOf(')'));
					dwell = Long.parseLong(c);
				} else if (t.length() > 0)
					dwell = Long.parseLong(t);

				if (dwell > 0)
					LAB.setText("** No. " + (count++) + " (" + dwell + ") **");
				else
					LAB.setText(NOT_SELECTED);
			}
		}
	}

	/**
	 * Get the pre-assembled GUI widgets for doing the list of channels.
	 * 
	 * @param h
	 *            the helper object
	 * 
	 * @param listener
	 *            the listener for when the "Accept this list" button get pressed. Leave this null, and it will exit when the button
	 *            is pressed.
	 * @return The JPanel that contains all the GUI widgets
	 * 
	 */
	public static Container getContainer(CreateListOfChannelsHelper h, final ActionListener listener) {
		if (h == null) {
			h = new CreateListOfChannelsHelper();
		}
		final CreateListOfChannelsHelper helper = h;

		if (listener == null)
			helper.accept.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					int count = 1;
					for (SignageContent CONTENT : helper.lister.channelList)
						if (CONTENT instanceof Channel) {
							System.out.println(count++ + " - Channel no. " + ((Channel) CONTENT).getNumber() + ": "
									+ CONTENT.getName() + ", " + CONTENT.getTime());
						} else {
							System.out.println(count++ + " - " + CONTENT.getName() + ", dwell=" + CONTENT.getTime() + " msec");
						}
					System.out.println("Default dwell time: " + helper.lister.getDwellTime() + " milliseconds");
					catchSleep(10);
					System.exit(0);
				}
			});
		else
			helper.accept.addActionListener(listener);

		JScrollPane scroller = new JScrollPane(helper.lister, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroller.getVerticalScrollBar().setUnitIncrement(16);
		if (!SHOW_IN_WINDOW)
			scroller.getVerticalScrollBar().setPreferredSize(new Dimension(40, 0));
		JPanel retval = new JPanel(new BorderLayout());

		retval.add(scroller, BorderLayout.CENTER);
		Box vb = Box.createVerticalBox();
		vb.add(helper.listerPanel);
		vb.add(helper.lister.timeWidgets);
		// vb.add(helper.lister.timeWidgets);

		retval.add(vb, BorderLayout.NORTH);

		retval.add(helper.lister.saveRestore, BorderLayout.SOUTH);
		return retval;
	}

	/**
	 * @return A play list of channels
	 */
	public SignageContent getChannelList() {
		return new ChannelPlayList(channelList, getDwellTime());
	}

	/**
	 * @param channelList
	 */
	public void setChannelList(final List<SignageContent> channelList) {
		this.channelList = channelList;
	}

	/**
	 * @return How long (in milliseconds) to stay on each channel
	 */
	public long getDwellTime() {
		return (Long) time.getValue();
	}

	/**
	 * 
	 * @return the Component that contains the time widgets
	 */
	public JComponent getTimeWidgets() {
		return timeWidgets;
	}

	/**
	 * @param args
	 *            Command line arguments (none expected)
	 */
	public static void main(final String[] args) {

		credentialsSetup();

		getMessagingServerNameSelector();

		// ChannelCatalogFactory.useRealChannels(true);
		IS_PUBLIC_CONTROLLER = false;
		SHOW_IN_WINDOW = true;

		JFrame f = new JFrame(CreateListOfChannels.class.getCanonicalName() + " Testing;");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		f.setContentPane(getContainer(null, null));
		f.setSize(900, 900);
		f.setVisible(true);
	}
}
