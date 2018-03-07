package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_HOUR;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.channel.ListUtils.getDwellStrings;
import static gov.fnal.ppd.dd.channel.ListUtils.interp;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.channel.BigLabel;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelListHolder;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.channel.SaveRestoreListOfChannels;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public class ChannelListGUI extends JPanel implements ActionListener, ChannelListHolder {
	private static final long				serialVersionUID				= 7371440415155147051L;

	private static boolean					PART_OF_CHANNEL_SELECTOR		= true;

	private static Color					defaultColor					= new Color(45, 100, 30);

	private ChannelCooserAsTable			choiceTable						= new ChannelCooserAsTable();
	private SelectedChannelsDisplayAsTable	resultsTable					= new SelectedChannelsDisplayAsTable();
	private ImageChooserAsTable				imageTable						= new ImageChooserAsTable();

	private JSpinner						time;

	private final BigLabel					selectedRowLabel				= new BigLabel(" (no rows selected) ", Font.PLAIN);
	private final JLabel					header1							= new JLabel("The content in your list");
	private final JLabel					header2							= new JLabel("Choose content to add to your list");
	private JLabel							databaseNameOfThisListHolder	= new JLabel("(none)");

	private JButton							moveRowUp						= new JButton("⇧");
	private JButton							moveRowDown						= new JButton("⇩");
	private JButton							deleteRow						= new JButton("✖");
	private JButton							clearUserList					= new JButton("Clear your list");
	private JButton							acceptThisList					= new JButton("Send this list to the Display");
	private JButton							instructionsButton				= new JButton("Instructions");
	private JButton							doubleButton					= new JButton("Double Seq Nums");

	private Box								bottomBox						= Box.createHorizontalBox();
	private Box								tableBox						= Box.createHorizontalBox();
	private Box								timeWidgets						= Box.createHorizontalBox();

	private SaveRestoreListOfChannels		saveRestore;

	// private NewListCreationListener newListCreationCallback = null;

	/**
	 * For testing the operation of this GUI (only)
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		credentialsSetup();
		// MakeChannelSelector.selectorSetup();
		SHOW_IN_WINDOW = true;
		PART_OF_CHANNEL_SELECTOR = false;

		// Schedule a job for the event-dispatching thread: creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	// TODO -- Tie this in with the save/restore capabilities

	/**
	 * 
	 */
	public ChannelListGUI() {
		super(new BorderLayout());
		saveRestore = new SaveRestoreListOfChannels(this);

		setupLocalListeners();
		setupLocalGUI();
	}

	private void setupLocalListeners() {
		choiceTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				addRowToResultsTable();
			}
		});

		imageTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				addImageRowToResultsTable();
			}
		});

		resultsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				ChannelInList chan = (ChannelInList) resultsTable.getValueAt(resultsTable.getSelectedRow(), 2);
				if (chan != null) {
					selectedRowLabel.setText("Row " + resultsTable.getSelectedRow() + " selected: Channel [\"" + chan + "\", seq="
							+ chan.getSequenceNumber() + "]");
					moveRowUp.setEnabled(true);
					moveRowDown.setEnabled(true);
					deleteRow.setEnabled(true);
				} else {
					selectedRowLabel.setText(" (no rows selected) ");
					moveRowUp.setEnabled(false);
					moveRowDown.setEnabled(false);
					deleteRow.setEnabled(false);
				}
			}
		});
	}

	private void setupLocalGUI() {
		int w = 700, h = 700;
		if (SHOW_IN_WINDOW) {
			w = 500;
			h = 500;
		}

		choiceTable.setPreferredScrollableViewportSize(new Dimension(w, h));

		BigLabel bl = new BigLabel("Approx Dwell Time (sec): ", Font.PLAIN);
		bl.setOpaque(false);

		final SpinnerModel model = new SpinnerListModel(getDwellStrings());
		time = new JSpinner(model);
		time.setValue(new Long(300l));
		time.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		if (!SHOW_IN_WINDOW) {
			time.setFont(new Font("Monospace", Font.PLAIN, 30));
			// TODO -- increase the size of the buttons. It is really complicated
			// (https://community.oracle.com/thread/1357837?start=0&tstart=0) -- later!
		}
		final JLabel timeInterpretLabel = new JLabel(interp((Long) time.getValue()));
		int ft = (SHOW_IN_WINDOW ? 12 : 24);
		timeInterpretLabel.setFont(new Font(Font.MONOSPACED, Font.ITALIC, ft));

		time.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				long val = (Long) model.getValue();
				timeInterpretLabel.setText(interp(val));
			}
		});

		timeWidgets.add(Box.createGlue());
		timeWidgets.add(bl);
		timeWidgets.add(time);
		timeWidgets.add(timeInterpretLabel);
		timeWidgets.add(Box.createGlue());

		acceptThisList.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				println(ChannelListGUI.class, "Num channels: " + resultsTable.getRowCount());
				for (int row = 0; row < resultsTable.getRowCount(); row++) {
					Channel chan = (Channel) resultsTable.getValueAt(row, 2);
					System.out.println("     " + row + ": Chan #" + chan.getNumber() + " [" + chan.getName() + "] dwell="
							+ chan.getTime());
				}
				databaseNameOfThisListHolder.setText(saveRestore.getListName() + " [" + resultsTable.getRowCount() + " entries]");
			}
		});

		instructionsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JTextPane textPane = new JTextPane();
				textPane.setContentType("text/html");
				textPane.setEditable(false);

				String text = "<html>";
				text += "<p><font size='+1'>This panel is for creating, editing, and saving lists of channels in the Dynamic Display system.</font></p>";
				text += "<p>This panel contains the \"Approx Dwell Time\" at the top, manipulation buttons on the bottom, and the main panel in the middle.</p>\n";
				text += "<p>The main panel is split in half with the list of channel you are creating on the left, and the list of all the channels in the entire system on the right.</p>";
				text += "<p><b>To create a channel list</b>, you copy a channel from the right to the left panels. Do this by touching the desired channel in the full list on the right.</p>\n"
						+ " You can add the same channel several times.";
				text += " The dwell time this channel gets is the \"Approx Dwell Time\" that appear at the top of the panel.</p>";
				text += "<p>Remove a channel from the left list by selecting the channel "
						+ "on the left and then touching the \"✖\" button at the bottom.</p>";
				text += "<p>With a channel selected on the left you can use the \"⇧\" "
						+ "and the \"⇩\" buttons to move this channel up and down on the list.</p>";
				text += "<p>To change the dwell time of a channel in the left panel, double-click the number in the dwell column,\n"
						+ "enter the number you want (a keyboard is required--ask for help on how to get the on-screen keyboard) and then hit \"enter\".</p>";
				text += "<p>When you have your list the way you want it, hit \"Accept\" to send this channel list to your display,\n"
						+ " or use the \"Save this list\" button to put this channel list permanently into the database.</p>";
				text += "<p>Note that the size of each column, and the ordering of the rows, can be manipulated just like column values in Excel.</p>";
				text += "<p>Feel free to contact the author (Elliott) if you have any ideas on this, e.g. how to make the process, or these instructions, simpler. (June, 2017)</p>";
				text += "</html>";
				textPane.setText(text);
				textPane.setMaximumSize(new Dimension(700, 700));
				textPane.setPreferredSize(new Dimension(700, 700));

				JOptionPane.showMessageDialog(null, textPane, "Channel List Instructions", JOptionPane.QUESTION_MESSAGE);

			}
		});

		doubleButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Need to refresh the table after doing this
				for (Channel C : ChannelListGUI.this.getList()) {
					ChannelInList CIL = (ChannelInList) C;
					CIL.setSequenceNumber(CIL.getSequenceNumber() * 2);
				}

				((AbstractTableModel) resultsTable.getModel()).fireTableDataChanged();
			}
		});

		float fontSize = 18.0f;
		if (SHOW_IN_WINDOW)
			fontSize = 14.0f;
		moveRowUp.setFont(getFont().deriveFont(fontSize));
		moveRowUp.setEnabled(false);
		moveRowUp.setActionCommand("up");
		moveRowUp.addActionListener(this);

		moveRowDown.setFont(getFont().deriveFont(fontSize));
		moveRowDown.setEnabled(false);
		moveRowDown.setActionCommand("down");
		moveRowDown.addActionListener(this);

		deleteRow.setFont(getFont().deriveFont(fontSize));
		deleteRow.setForeground(Color.red);
		deleteRow.setEnabled(false);
		deleteRow.setActionCommand("delete");
		deleteRow.addActionListener(this);

		clearUserList.setFont(getFont().deriveFont(fontSize));
		clearUserList.setActionCommand("clear");
		clearUserList.setToolTipText("Clear the user-defined lists created here");
		clearUserList.addActionListener(this);

		selectedRowLabel.setFont(new Font("Arial", Font.PLAIN, (int) fontSize));

		bottomBox.add(clearUserList);
		bottomBox.add(Box.createRigidArea(new Dimension(10, 10)));
		bottomBox.add(moveRowUp);
		bottomBox.add(Box.createRigidArea(new Dimension(10, 10)));
		bottomBox.add(moveRowDown);
		bottomBox.add(Box.createRigidArea(new Dimension(10, 10)));
		bottomBox.add(deleteRow);
		bottomBox.add(Box.createRigidArea(new Dimension(10, 10)));
		bottomBox.add(doubleButton);
		bottomBox.add(Box.createRigidArea(new Dimension(10, 10)));

		// Beware: This is a bit of a kludge
		if (PART_OF_CHANNEL_SELECTOR) {
			bottomBox.add(acceptThisList);
			bottomBox.add(Box.createRigidArea(new Dimension(10, 10)));
		}
		bottomBox.add(instructionsButton);

		Box bottomBoxHolder = Box.createVerticalBox();
		selectedRowLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 10));
		bottomBoxHolder.add(selectedRowLabel);
		bottomBoxHolder.add(bottomBox);

		resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane jScrollPane = new JScrollPane(resultsTable);
		if (!SHOW_IN_WINDOW)
			jScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(50, 0));

		Box vb = Box.createVerticalBox();
		header1.setFont(new Font("Arial", Font.BOLD, 16));
		header1.setAlignmentX(CENTER_ALIGNMENT);
		vb.add(header1);
		vb.add(jScrollPane);

		databaseNameOfThisListHolder.setAlignmentX(CENTER_ALIGNMENT);
		vb.add(databaseNameOfThisListHolder);
		tableBox.add(vb);

		choiceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jScrollPane = new JScrollPane(choiceTable);
		if (!SHOW_IN_WINDOW)
			jScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(50, 0));
		tableBox.add(Box.createRigidArea(new Dimension(5, 5)));

		vb = Box.createVerticalBox();
		header2.setFont(new Font("Arial", Font.BOLD, (SHOW_IN_WINDOW ? 16 : 20)));
		header2.setAlignmentX(CENTER_ALIGNMENT);
		JTabbedPane channelTypePane = new JTabbedPane();
		channelTypePane.add("Channels", jScrollPane);

		jScrollPane = new JScrollPane(imageTable);
		if (!SHOW_IN_WINDOW)
			jScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(50, 0));
		channelTypePane.add("Images", jScrollPane);

		vb.add(header2);
		vb.add(channelTypePane);
		tableBox.add(vb);

		tableBox.setOpaque(true);

		add(timeWidgets, BorderLayout.NORTH);
		add(tableBox, BorderLayout.CENTER);

		Box bb = Box.createVerticalBox();
		bb.add(bottomBoxHolder);
		bb.add(saveRestore);
		add(bb, BorderLayout.SOUTH);

		setOpaque(true);
	}

	/**
	 * @param bg
	 *            The decoration color to be used for this widget
	 */
	public void setBackgroundBypass(final Color bg) {
		tableBox.setOpaque(true);
		tableBox.setBackground(bg);
		tableBox.setBorder(BorderFactory.createLineBorder(bg, 3));
		resultsTable.setBorder(BorderFactory.createLineBorder(bg, 1));
		choiceTable.setBorder(BorderFactory.createLineBorder(bg, 1));

		float brightness = (Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), null))[2];

		if (brightness < 0.6) {
			header1.setForeground(Color.white);
			header2.setForeground(Color.white);
			databaseNameOfThisListHolder.setForeground(Color.white);
		}
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching thread.
	 */
	private static void createAndShowGUI() {
		// Create and set up the window.
		JFrame frame = new JFrame("Dynamic Displays: Manipulate Channel Lists");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		ChannelListGUI clg = new ChannelListGUI();
		clg.setBackgroundBypass(defaultColor);
		frame.setContentPane(clg);

		// Display the window.
		frame.setSize(1300, 800);
		frame.setVisible(true);
	}

	private void addRowToResultsTable() {
		int viewRow = choiceTable.convertRowIndexToModel(choiceTable.getSelectedRow());
		// Some debugging, commented out here:
		// int numCols = choiceTable.getColumnCount();
		// TableModel model = choiceTable.getModel();
		// int numRows = table.getRowCount();
		// System.out.print("    row " + viewRow + ": [");
		// for (int j = 0; j < numCols; j++) {
		// System.out.print("  " + model.getValueAt(viewRow, j));
		// }
		// System.out.println("] Default dwell time=" + time.getValue());

		// If the default dwell time is less than the requested dwell time, add it several times
		Channel chan = choiceTable.getRow(viewRow);
		long chanDwell = chan.getTime(); // / 1000L;
		long askedFor = (long) time.getValue();
		int k = 1;
		if (chanDwell == 0 || chanDwell > askedFor) {
			resultsTable.add(chan, askedFor);
		} else {
			resultsTable.add(chan, chanDwell);
			for (long totalTime = chanDwell; totalTime + chanDwell < askedFor; totalTime += chanDwell) {
				resultsTable.add(chan, chanDwell);
				k++;
			}
		}
		if (showThePopup(k, askedFor, chanDwell))
			println(getClass(), "Results table now has " + resultsTable.getRowCount() + " rows.");
		databaseNameOfThisListHolder.setText(saveRestore.getListName() + " [" + resultsTable.getRowCount() + " entries]");
	}

	private void addImageRowToResultsTable() {
		int viewRow = imageTable.convertRowIndexToModel(imageTable.getSelectedRow());
		ChannelInList chan = imageTable.getRow(viewRow);
		resultsTable.add(chan, (long) time.getValue());
	}

	private long	whenToShowThePopup	= 0L;

	private boolean showThePopup(final int count, final long d1, final long d2) {
		if (count <= 1)
			return false;

		if (System.currentTimeMillis() < whenToShowThePopup) {
			// Check this assumption: Assume that the user is adding channels a lot over a long period, so reset this timer
			whenToShowThePopup = System.currentTimeMillis() + ONE_HOUR;
			// This means that it will only show this popup again when there is a one hour break
			return false;
		}

		// Create the popup and return
		JCheckBox hideMe = new JCheckBox("Hide this message for an hour");
		JLabel countLabel = new JLabel("<html><h2>This channel was entered into your list " + count
				+ " times</h2>This is because you asked for a dwell time for this entry (" + d1
				+ " secs) that was greater than natural durartion of this channel (" + d2 + " secs).</html>");
		Box message = Box.createVerticalBox();
		message.add(countLabel);
		message.add(Box.createRigidArea(new Dimension(20, 20)));
		message.add(hideMe);
		JOptionPane.showMessageDialog(this, message);
		if (hideMe.isSelected())
			whenToShowThePopup = System.currentTimeMillis() + ONE_HOUR;
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int here = resultsTable.getSelectedRow();
		int prev = here - 1;
		int next = here + 1;
		SelectedChannelsTableModel model = (SelectedChannelsTableModel) resultsTable.getModel();
		switch (e.getActionCommand()) {
		case "up":
			if (model.swap(here, prev))
				resultsTable.setRowSelectionInterval(prev, prev);
			break;
		case "down":
			if (model.swap(here, next))
				resultsTable.setRowSelectionInterval(next, next);
			break;
		case "delete":
			model.delete(here);
			break;

		case "clear":
			model.clear();
			break;

		default:
			println(getClass(), "Invalid action command received.  This should not happen.");
			break;
		}
	}

	@Override
	public List<ChannelInList> getList() {
		SelectedChannelsTableModel model = (SelectedChannelsTableModel) resultsTable.getModel();
		return model.getAllChannels();
	}

	@Override
	public void clear() {
		SelectedChannelsTableModel model = (SelectedChannelsTableModel) resultsTable.getModel();
		model.reset();
	}

	@Override
	public void channelAdd(ChannelInList c) {
		SelectedChannelsTableModel model = (SelectedChannelsTableModel) resultsTable.getModel();
		model.addChannel(c);
		databaseNameOfThisListHolder.setText(saveRestore.getListName() + " [" + resultsTable.getRowCount() + " entries]");
	}

	@Override
	public void fix() {
		// Nothing to fix in this JTable representation.
	}

	/**
	 * @param newListCreationCallback
	 *            the newListCreationCallback to set
	 */
	public void setNewListCreationCallback(final NewListCreationListener newListCreationCallback) {
		saveRestore.setNewListListener(new NewListCreationListener() {

			@Override
			public void newListCreationCallback() {
				databaseNameOfThisListHolder.setText(saveRestore.getListName());
				newListCreationCallback.newListCreationCallback();
			}
		});
	}

	/**
	 * @return The list of channels, as a new channel, to be sent to the display
	 */
	public SignageContent getChannelList() {
		return new ChannelPlayList(resultsTable.getChannelList(), ONE_HOUR);
	}

	/**
	 * @param listener
	 *            The object that will handle the mechanisms to send my list to a display
	 */
	public void setListener(final ActionListener listener) {
		acceptThisList.addActionListener(listener);
	}
}
