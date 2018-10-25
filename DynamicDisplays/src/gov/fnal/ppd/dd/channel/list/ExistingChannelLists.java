package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.db.ListUtilsDatabase.readTheChannelLists;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.changer.ChannelButtonGrid;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.DDButton;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.BigLabel;
import gov.fnal.ppd.dd.util.DisplayButtonGroup;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2017-18
 * 
 */
public class ExistingChannelLists extends ChannelButtonGrid implements NewListCreationListener {
	private static final long	serialVersionUID	= -8050761379148979848L;

	// FIXME - This attribute should probably be created elsewhere and handed to this class in the constructor.
	private Map<String, ChannelPlayList>		listofChannelLists	= readTheChannelLists();

	private float				FS;

	private class ListDDButton extends DDButton implements ActionListener {
		private static final long			serialVersionUID	= 3718209609564437340L;
		private ArrayList<ActionListener>	listeners			= new ArrayList<ActionListener>();
		private String						buttonName;

		public ListDDButton(String buttonName, Channel channel, Display display, int maxLen) {
			super(channel, display, maxLen);
			super.addActionListener(this);
			this.buttonName = buttonName;
			super.setText(buttonName);
		}

		public void addActionListener(ActionListener a) {
			listeners.add(a);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String listContents = this.getChannel().toString().replace("(", "\n(");
			String succinctName = buttonName.replace("<html>", "").replace("</html>", "").replace("<br>", "");
			succinctName = succinctName.substring(0, succinctName.indexOf("<i>[Length"));
			if (JOptionPane.showConfirmDialog(this, "Change the channel to '" + succinctName + "'?\n" + listContents, "Continue?",
					JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
				for (ActionListener L : listeners)
					L.actionPerformed(e);
			} else {
				// Would use this temporary dialog, but this class shows "Success" -- I need "Rejected by user"
				// new TemporaryDialogBox(this, getDisplay());
			}
		}
	}

	/**
	 * @param display
	 * @param bg
	 * 
	 */
	public ExistingChannelLists(final Display display, final DisplayButtonGroup bg) {
		super(display, bg);

		makeGrid(null);
	}
	
	@Override
	public void makeGrid(ChannelCategory cat) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(makeTheGrid(), BorderLayout.CENTER);
		BigLabel title = new BigLabel("Lists of channels", Font.BOLD);
		title.setAlignmentX(CENTER_ALIGNMENT);
		title.setAlignmentY(CENTER_ALIGNMENT);

		JLabel instructions = new JLabel(
				"To see what is in the list, click the button and it will show you before sending it to the Display", JLabel.CENTER);
		instructions.setFont(new Font("Arial", Font.ITALIC, 12));
		instructions.setAlignmentX(CENTER_ALIGNMENT);
		instructions.setAlignmentY(CENTER_ALIGNMENT);

		Box vb = Box.createVerticalBox();
		vb.add(title);
		vb.add(instructions);
		vb.setAlignmentX(CENTER_ALIGNMENT);
		vb.setAlignmentY(CENTER_ALIGNMENT);

		panel.add(vb, BorderLayout.NORTH);
		add(panel);
	}

	private JComponent makeTheGrid() {
		int cols = 4;
		// int gap = 5;
		int maxLen = 50;

		if (listofChannelLists.size() > 39) {
			FS = 0.5f * FONT_SIZE;
			cols = 5;
			maxLen = 17;
		} else if (listofChannelLists.size() > 18) {
			FS = 0.6f * FONT_SIZE;
			cols = 3;
			maxLen = 15;
		} else {
			switch (listofChannelLists.size()) {
			case 18:
			case 17:
			case 16:
				FS = 0.6f * FONT_SIZE;
				cols = 3;
				maxLen = 13;
				break;

			case 15:
			case 14:
			case 13:
				FS = 0.75f * FONT_SIZE;
				cols = 3;
				maxLen = 14;
				break;

			case 12:
			case 11:
				FS = 0.9f * FONT_SIZE;
				cols = 2;
				maxLen = 14;
				break;

			case 10:
			case 9:
			case 8:
				FS = 0.94f * FONT_SIZE;
				cols = 2;
				maxLen = 13;
				break;

			case 7:
			case 6:
				FS = FONT_SIZE;
				cols = 1;
				maxLen = 90;
				break;

			default:
				FS = 1.2f * FONT_SIZE;
				cols = 1;
				maxLen = 80;
				break;
			}
		}

		FS /= 1.5f;

		GridLayout g = new GridLayout(0, cols);

		expGrid = new JPanel(g);
		expGrid.setOpaque(true);
		expGrid.setBackground(display.getPreferredHighlightColor());
		int numButtons = 0;
		for (String listName : listofChannelLists.keySet()) {
			numButtons++;
			ChannelPlayList theChannelList = listofChannelLists.get(listName);
			final DDButton button = new ListDDButton("<html>" + listName + " <i>[Length=" + theChannelList.getChannels().size()
					+ "]</i></html>", theChannelList, display, maxLen);

			// FIXME - The button that shows the details on this panel messes up the simple names.

			if (SHOW_IN_WINDOW) {
				button.setFont(button.getFont().deriveFont(FS));
			} else {
				switch (button.numLinesInTitle()) {
				case 2:
					button.setFont(button.getFont().deriveFont(FS * 0.8300f));
					break;
				case 3:
					button.setFont(button.getFont().deriveFont(FS * 0.7000f));
					break;
				default:
					if (cols == 2 && button.getText().length() > 15)
						button.setFont(button.getFont().deriveFont(FS * 0.8500f));
					else
						button.setFont(button.getFont().deriveFont(FS));
					break;
				}

			}
			button.setSelected(theChannelList.equals(display.getContent()));
			button.addActionListener(this);
			button.addActionListener(display);

			bg.add(button);

			expGrid.add(button);
		}
		for (; numButtons < 5; numButtons++) {
			expGrid.add(Box.createRigidArea(new Dimension(1, 1)));
		}

		expGrid.setAlignmentX(JComponent.TOP_ALIGNMENT);
		return expGrid;
	}

	@Override
	public void newListCreationCallback() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				// FIXME - This is not working. The call comes in, but the grid is not recreated from scratch.
				println(ExistingChannelLists.class, ": Attempting to redraw my grid");
				invalidate();
				removeAll();
				listofChannelLists = readTheChannelLists();
				makeGrid(null);
				validate();
			}
		});
	}
}
