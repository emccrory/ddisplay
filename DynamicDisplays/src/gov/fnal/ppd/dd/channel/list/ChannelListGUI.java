package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.prepareSaverImages;
import static gov.fnal.ppd.dd.channel.ListUtils.getDwellStrings;
import static gov.fnal.ppd.dd.channel.ListUtils.interp;
import gov.fnal.ppd.dd.MakeChannelSelector;
import gov.fnal.ppd.dd.channel.BigLabel;
import gov.fnal.ppd.dd.channel.ChannelListHolder;
import gov.fnal.ppd.dd.channel.SaveRestoreListOfChannels;
import gov.fnal.ppd.dd.signage.Channel;

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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2017
 * 
 */
public class ChannelListGUI extends JPanel implements ActionListener, ChannelListHolder {
	private static final long				serialVersionUID	= 7371440415155147051L;
	private ChannelCooserAsTable			choiceTable			= new ChannelCooserAsTable();
	private SelectedChannelsDisplayAsTable	resultsTable		= new SelectedChannelsDisplayAsTable();
	private JSpinner						time;
	private Box								timeWidgets			= Box.createHorizontalBox();

	private BigLabel						selectedRowLabel	= new BigLabel(" ", 20);
	private JButton							moveUp				= new JButton("⇧");
	private JButton							moveDown			= new JButton("⇩");
	private JButton							delete				= new JButton("✖");
	private JButton							accept				= new JButton("Accept");
	private Box								bottomBox			= Box.createHorizontalBox();
	private Box								tableBox			= Box.createHorizontalBox();
	private SaveRestoreListOfChannels		saveRestore;

	/**
	 * For testing the operation of this GUI (only)
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		prepareSaverImages();
		credentialsSetup();
		MakeChannelSelector.selectorSetup();

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

		resultsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				// do some actions here, for example
				// print first column value from selected row
				Channel chan = (Channel) resultsTable.getValueAt(resultsTable.getSelectedRow(), 1);
				if (chan != null) {
					selectedRowLabel.setText("Row " + resultsTable.getSelectedRow() + " selected: channel #" + chan.getNumber()
							+ " [" + chan + ", dwell=" + chan.getTime() + "]");
					moveUp.setEnabled(true);
					moveDown.setEnabled(true);
					delete.setEnabled(true);
				} else {
					selectedRowLabel.setText(" ");
					moveUp.setEnabled(false);
					moveDown.setEnabled(false);
					delete.setEnabled(false);
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

		accept.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Num channels: " + resultsTable.getRowCount());
				for (int row = 0; row < resultsTable.getRowCount(); row++) {
					Channel chan = (Channel) resultsTable.getValueAt(row, 1);
					System.out.println(row + ": Chan #" + chan.getNumber() + " [" + chan.getName() + "] dwell=" + chan.getTime());
				}
			}
		});

		float fontSize = 18.0f;
		if (SHOW_IN_WINDOW)
			fontSize = 14.0f;
		moveUp.setFont(getFont().deriveFont(fontSize));
		moveUp.setEnabled(false);
		moveUp.setActionCommand("up");
		moveUp.addActionListener(this);

		moveDown.setFont(getFont().deriveFont(fontSize));
		moveDown.setEnabled(false);
		moveDown.setActionCommand("down");
		moveDown.addActionListener(this);

		delete.setFont(getFont().deriveFont(fontSize));
		delete.setForeground(Color.red);
		delete.setEnabled(false);
		delete.setActionCommand("delete");
		delete.addActionListener(this);

		bottomBox.add(moveUp);
		bottomBox.add(Box.createRigidArea(new Dimension(10, 10)));
		bottomBox.add(moveDown);
		bottomBox.add(Box.createRigidArea(new Dimension(10, 10)));
		bottomBox.add(delete);
		bottomBox.add(Box.createRigidArea(new Dimension(10, 10)));
		bottomBox.add(accept);
		bottomBox.add(Box.createRigidArea(new Dimension(10, 10)));
		bottomBox.add(selectedRowLabel);

		resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane jScrollPane = new JScrollPane(resultsTable);
		if (!SHOW_IN_WINDOW)
			jScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(50, 0));

		Box vb = Box.createVerticalBox();
		vb.add(new JLabel("The channels in the list so far"));
		vb.add(jScrollPane);
		tableBox.add(vb);

		choiceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jScrollPane = new JScrollPane(choiceTable);
		if (!SHOW_IN_WINDOW)
			jScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(50, 0));
		tableBox.add(Box.createRigidArea(new Dimension(5, 5)));
		
		vb = Box.createVerticalBox();
		vb.add(new JLabel("Choose channels to add to the list"));
		vb.add(jScrollPane);
		tableBox.add(vb);

		tableBox.setOpaque(true);

		add(timeWidgets, BorderLayout.NORTH);
		add(tableBox, BorderLayout.CENTER);

		Box bb = Box.createVerticalBox();
		bb.add(bottomBox);
		bb.add(saveRestore);
		add(bb, BorderLayout.SOUTH);

		setOpaque(true);
	}

	/**
	 * @param bg
	 *            The decoration color to be used for this wiodget
	 */
	public void setBackgroundBypass(final Color bg) {
		tableBox.setOpaque(true);
		tableBox.setBackground(bg);
		tableBox.setBorder(BorderFactory.createLineBorder(bg, 3));
		resultsTable.setBorder(BorderFactory.createLineBorder(bg, 1));
		choiceTable.setBorder(BorderFactory.createLineBorder(bg, 1));
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching thread.
	 */
	private static void createAndShowGUI() {
		// Create and set up the window.
		JFrame frame = new JFrame("ChannelTableTest");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		ChannelListGUI clg = new ChannelListGUI();
		clg.setBackgroundBypass(Color.green);
		frame.setContentPane(clg);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	private void addRowToResultsTable() {
		// int numRows = table.getRowCount();
		int numCols = choiceTable.getColumnCount();
		TableModel model = choiceTable.getModel();
		int viewRow = choiceTable.convertRowIndexToModel(choiceTable.getSelectedRow());

		System.out.print("    row " + viewRow + ": [");
		for (int j = 0; j < numCols; j++) {
			System.out.print("  " + model.getValueAt(viewRow, j));
		}

		System.out.println("] Default dwell time=" + time.getValue());

		Channel chan = choiceTable.getRow(viewRow);
		resultsTable.add(chan, (Long) time.getValue());
		System.out.println("Results table now has " + resultsTable.getRowCount() + " rows.");
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
		default:
			System.out.println("oops");
			break;
		}

	}

	@Override
	public List<Channel> getList() {
		ChannelChooserTableModel model = (ChannelChooserTableModel) choiceTable.getModel();
		return model.getAllChannels();
	}

	@Override
	public void clear() {
		SelectedChannelsTableModel model = (SelectedChannelsTableModel) resultsTable.getModel();
		model.reset();
	}

	@Override
	public void channelAdd(Channel c) {
		SelectedChannelsTableModel model = (SelectedChannelsTableModel) resultsTable.getModel();
		model.addChannel(c);
	}

	@Override
	public void fix() {
	}
}
