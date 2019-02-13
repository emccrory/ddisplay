package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.INSET_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.userHasDoneSomething;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.getDisplayID;
import static gov.fnal.ppd.dd.util.Util.launchMemoryWatcher;
import gov.fnal.ppd.dd.changer.ChannelButtonGrid;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.DDButton;
import gov.fnal.ppd.dd.changer.DetailedInformationGrid;
import gov.fnal.ppd.dd.changer.DisplayButtons;
import gov.fnal.ppd.dd.changer.DisplayChangeEvent;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.CheckDisplayStatus;
import gov.fnal.ppd.dd.util.DisplayButtonGroup;
import gov.fnal.ppd.dd.util.DisplayCardActivator;
import gov.fnal.ppd.dd.util.DisplayKeeper;
import gov.fnal.ppd.dd.util.JLabelFooter;
import gov.fnal.ppd.dd.util.SelectorInstructions;
import gov.fnal.ppd.dd.util.WhoIsInChatRoom;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * <p>
 * This is a pared-down version of the main class, ChannelSelector, that only allows you to send a very restricted subset of
 * channels to the "alternate frames" at each display. The Use Case is that the channel will be a text web page of lab-wide
 * announcements. A separate mechanism will be provided to change the text in this web page.
 * </p>
 * <p>
 * TODO - with the removal of frames, this may need some work
 * </p>
 * <p>
 * TODO -- Turn off the extra frame
 * </p>
 * 
 * FIXME -- Is this replaced by the Emergency Message use case? 2/7/2019
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2015
 */
public class ChangeChannelOnFrame extends JPanel implements ActionListener, DisplayCardActivator, DisplayKeeper {

	private static final long				serialVersionUID			= 380454913639069818L;
	/**
	 * 
	 */
	public static final Dimension			screenDimension				= Toolkit.getDefaultToolkit().getScreenSize();
	private static final int				NUMBER_OF_FRAMES			= 4;
	private static JFrame					f							= new JFrame("Dynamic Display Channel Selector");

	private List<List<ChannelButtonGrid>>	channelButtonGridList		= new ArrayList<List<ChannelButtonGrid>>();
	/*
	 * Each tab contains a specific type of Channels in a CardLayout There is one Card for each Display in the CardLayout A button
	 * might not be selected in a Card if that channel is not shown on the panel
	 */

	private Box								titleBox;
	private JLabel							title;
	protected int							selectedTab;

	private DisplayButtons					displaySelector;
	private CardLayout						card						= new CardLayout();
	private JPanel							displayChannelPanel			= new JPanel(card);

	private String							lastActiveDisplay			= null;

	private List<JTabbedPane>				listOfDisplayTabbedPanes	= new ArrayList<JTabbedPane>();

	/**
	 * Create the channel selector GUI for sending web pages to other frames in the display
	 * 
	 * TODO -- Make a command to send the same thing to ALL the displays at once
	 * 
	 * TODO -- Probably no need (at this time) to have lots of different frames--one is probably OK
	 * 
	 * TODO -- There should be a way to turn off the extra frame (rather than just waiting for it to time out).
	 */
	public ChangeChannelOnFrame() {
		super(new BorderLayout());

		// Determine the size of the screen and thus the appropriate font size.

		if (SHOW_IN_WINDOW) {
			FONT_SIZE = 30.0f;
			INSET_SIZE = 3;
		} else {
			int width = screenDimension.width;
			System.out.println("My screen is " + screenDimension);

			if (width < 801) {
				// An older phone
				FONT_SIZE = 20.0f;
				INSET_SIZE = 5;
			} else if (width < 1000) {
				// An older iPad or a newer phone
				FONT_SIZE = 30.0f;
				INSET_SIZE = 16;
			} else if (width < 1400) {
				// An older screen
				FONT_SIZE = 40.0f;
				INSET_SIZE = 24;
			}
		}
	}

	/**
	 * In order to obey the rule-of-thumb to not start threads within the context of a constructor, this start method is provided.
	 */
	public void start() {
		initComponents();
		launchMemoryWatcher();
	}

	private void initComponents() {
		removeAll();
		SignageType cat = SignageType.XOC; // Show everything
		if (IS_PUBLIC_CONTROLLER)
			cat = SignageType.Public;

		displaySelector = new DisplayButtons(cat, this);
		lastActiveDisplay = displaySelector.getFirstDisplay().toString();
		// initChannelSelectors();
		initializeTabs();

		add(displayChannelPanel, BorderLayout.CENTER);
		if (SHOW_IN_WINDOW) {
			displayChannelPanel.setPreferredSize(new Dimension(700, 640));
		}

		add(displaySelector, BorderLayout.EAST);
		add(makeTitle(), BorderLayout.NORTH);

	}

	private void initializeTabs() {
		int index = 0;
		for (final Display display : displayList) {

			// NOTE: Each display has the same set of tabs and channels to offer.

			final JTabbedPane displayTabPane = new JTabbedPane();
			listOfDisplayTabbedPanes.add(displayTabPane);
			float sz = 12.0f;

			displayTabPane.setFont(getFont().deriveFont(sz));
			displayTabPane.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					selectedTab = ((JTabbedPane) e.getSource()).getSelectedIndex();
				}
			});
			DisplayButtonGroup bg = new DisplayButtonGroup();

			final List<ChannelButtonGrid> allGrids = new ArrayList<ChannelButtonGrid>();
			channelButtonGridList.add(allGrids);

			ChannelCategory cat = new ChannelCategory("Special");
			for (int frameNumber = 1; frameNumber < NUMBER_OF_FRAMES; frameNumber++) {
				ChannelButtonGrid grid = new DetailedInformationGrid(display, bg);

				grid.makeGrid(cat);
				allGrids.add(grid);
				display.addListener(grid);
				displayTabPane.add(grid, "Frame " + frameNumber);
			}

			final JPanel inner = new JPanel(new BorderLayout());
			int wid1 = 6;
			int wid2 = 5;
			if (SHOW_IN_WINDOW) {
				wid1 = 2;
				wid2 = 1;
			}
			displayTabPane.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent arg0) {
					userHasDoneSomething();
					setAllTabs(displayTabPane.getSelectedIndex());
				}
			});

			// Add the Display Tabbed Pane to the main screen
			inner.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(display.getPreferredHighlightColor(), wid1),
					BorderFactory.createEmptyBorder(wid2, wid2, wid2, wid2)));
			inner.add(displayTabPane, BorderLayout.CENTER);
			final JLabelFooter footer = makeChannelIndicator(display);
			display.addListener(new ActionListener() {
				private long	lastUpdated	= System.currentTimeMillis();

				// A simple listener to change the title after the Display has a chance to react.
				public void actionPerformed(ActionEvent e) {
					System.out.println(ChangeChannelOnFrame.class.getSimpleName() + ".actionPerformed(), event=" + e);
					DisplayChangeEvent ev = (DisplayChangeEvent) e;
					catchSleep(100);

					String text = display.getVirtualDisplayNumber() + " to channel '" + (display.getContent().getName()) + "'";
					boolean alive = false;

					switch (ev.getType()) {
					case CHANGE_RECEIVED:
						text = "<html>Setting Display " + text;
						break;
					case CHANGE_COMPLETED:
						text = "<html>Changed Display " + text;
						alive = true;
						break;
					case ERROR:
						text = "Display " + display.getVirtualDisplayNumber() + " ERROR; " + ": "
								+ display.getContent().getCategory() + "/'" + (display.getContent().getName());
						launchErrorMessage(e);
						break;

					case ALIVE:
						// These remaining items are not ever reached. (1/28/2015)
						alive = true;
						text = "A Display " + display.getVirtualDisplayNumber() + ": " + display.getContent().getCategory() + "/'"
								+ (display.getContent().getName());
						break;

					case IDLE:
						alive = true;
						if (lastUpdated + 30000l > System.currentTimeMillis())
							break;
						text = "G Display " + display.getVirtualDisplayNumber() + " Idle; " + ": "
								+ display.getContent().getCategory() + "/'" + (display.getContent().getName());
						break;
					}
					lastUpdated = System.currentTimeMillis();

					footer.setText(text);
					displaySelector.resetToolTip(display);
					setDisplayIsAlive(display.getDBDisplayNumber(), alive);
				}

			});

			footer.setAlignmentX(CENTER_ALIGNMENT);
			Box b = Box.createHorizontalBox();
			b.add(Box.createHorizontalGlue());
			b.add(footer);
			b.add(Box.createHorizontalGlue());
			inner.add(b, BorderLayout.SOUTH);

			displayChannelPanel.add(inner, getDisplayID(display));

			final int fi = index;
			bg.setActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					setTabColor(((DDButton) e.getSource()).getDisplay(), fi);
				}
			});
			setTabColor(display, index);

			// Set up a database check every now and then to see what a Display is actually doing (this is inSTEAD of the Ping
			// thread, above).
			new CheckDisplayStatus(display, index, footer, channelButtonGridList).start();
			index++;
		}

		new WhoIsInChatRoom(this).start();
	}

	private static Thread	errorMessageThread	= null;

	private static void launchErrorMessage(final ActionEvent e) {
		if (errorMessageThread != null)
			return;
		errorMessageThread = new Thread("ErrorMessagePopup") {
			public void run() {
				JOptionPane.showMessageDialog(null,
						e.paramString().substring(e.paramString().indexOf("ERROR") + 9, e.paramString().indexOf(",when")),
						"Error in communications", JOptionPane.ERROR_MESSAGE);
				// catchSleep(3000L);
				errorMessageThread = null;
			}
		};
		errorMessageThread.start();
	}

	/**
	 * Set all tabs for all Displays to the same thing
	 * 
	 * @param selectedIndex
	 *            The index that should be selected
	 */
	protected void setAllTabs(int selectedIndex) {
		for (JTabbedPane TAB : listOfDisplayTabbedPanes) {
			TAB.setSelectedIndex(selectedIndex);
		}
	}

	@Override
	public void setDisplayIsAlive(int number, boolean alive) {
		// System.out.println(ChannelSelector.class.getSimpleName() + ": Display " + number + (alive ? " is alive" :
		// " is NOT alive"));
		displaySelector.setIsAlive(number, alive);

		// Enable the Channel buttons, too
		for (List<ChannelButtonGrid> allGrids : channelButtonGridList)
			if (allGrids.get(0).getDisplay().getDBDisplayNumber() == number)
				for (ChannelButtonGrid cbg : allGrids)
					if (cbg != null)
						cbg.setAlive(alive);
	}

	protected static Border getTitleBorder(Color c) {
		if (SHOW_IN_WINDOW)
			return BorderFactory.createEmptyBorder(1, 5, 1, 5);

		return BorderFactory.createEmptyBorder(2, 30, 2, 30);
	}

	private JComponent makeTitle() {
		// showBorderButton.setSelected(true);
		title = new JLabel(" Dynamic Display 00 ");
		if (!SHOW_IN_WINDOW) {
			title.setFont(new Font("Serif", Font.ITALIC, (int) (3 * FONT_SIZE / 4)));
			// showBorderButton.setFont(new Font("SansSerif", Font.BOLD, (int) (FONT_SIZE / 2)));
		}
		// showBorderButton.setOpaque(false);
		// title.setFont(title.getFont().deriveFont(3 * FONT_SIZE / 4));
		title.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		title.setOpaque(true);
		title.setBackground(Color.white);
		titleBox = Box.createHorizontalBox();
		adjustTitle(displayList.get(0));

		return titleBox;
	}

	private void adjustTitle(Display display) {
		if (display == null)
			return;
		Color c = display.getPreferredHighlightColor();

		String num = "" + display.getVirtualDisplayNumber();
		String t = display.getLocation();
		if (t.contains(num))
			title.setText("   " + display.getLocation() + "   ");
		else
			title.setText("   " + display.getLocation() + " (" + num + ")  ");

		titleBox.removeAll();
		titleBox.setOpaque(true);
		titleBox.setBackground(c);
		int wid = (SHOW_IN_WINDOW ? 1 : 8);
		titleBox.setBorder(BorderFactory.createLineBorder(c, wid));

		titleBox.add(title);
		titleBox.add(Box.createHorizontalGlue());

		titleBox.setOpaque(true);
	}

	private static JLabelFooter makeChannelIndicator(Display display) {
		// JLabel footer = new JLabel(" Display " + display.getNumber() + " set to the '" + display.getContent().getCategory()
		// + "' channel '" + (display.getContent().getName()) + "' (" + shortDate() + ")");
		JLabelFooter footer = new JLabelFooter("Display " + display.getVirtualDisplayNumber());
		DisplayButtons.setToolTip(display);

		if (!SHOW_IN_WINDOW) {
			footer.setFont(new Font("Arial", Font.PLAIN, 25));
		}
		footer.setAlignmentX(CENTER_ALIGNMENT);
		return footer;
	}

	public void actionPerformed(ActionEvent e) {
		// The display selection has been changed, Select the "card" that shows this panel

		card.show(displayChannelPanel, e.getActionCommand());
		lastActiveDisplay = e.getActionCommand();

		int displayNum = e.getID();
		adjustTitle(displayList.get(displayNum));
		setTabColor(displayList.get(displayNum), displayNum);
		userHasDoneSomething();
	}

	private void setTabColor(Display d, int index) {
		JPanel inner = (JPanel) displayChannelPanel.getComponents()[index];
		JTabbedPane tab = ((JTabbedPane) inner.getComponent(0));
		Color c = d.getPreferredHighlightColor();
		for (int i = 0; i < tab.getComponentCount(); i++) {
			if (tab.getComponentAt(i) instanceof ChannelButtonGrid) {
				ChannelButtonGrid grid = (ChannelButtonGrid) tab.getComponentAt(i);
				tab.setBackgroundAt(i, grid.hasSelectedChannel() ? c : null);
			}
		}
	}

	// This code snippet would be used if this selector was to be run on an Android device.
	// @Override
	// public void onConfigurationChanged(Configuration newConfig) {
	// super.onConfigurationChanged(newConfig);
	// //Do stuff here
	// }

	/**
	 * Run the ChangeChannelOnFrame application.
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		credentialsSetup();

		MakeChannelSelector.selectorSetup();

		@SuppressWarnings("unused")
		final SignageType sType = (IS_PUBLIC_CONTROLLER ? SignageType.Public : SignageType.XOC);

		ChangeChannelOnFrame changer = new ChangeChannelOnFrame();
		changer.start();

		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		if (SHOW_IN_WINDOW) {
			f.setContentPane(changer);
			f.pack();
		} else {
			// Full-screen display of this app!
			Box h = Box.createVerticalBox();
			h.add(changer);
			SelectorInstructions label = new SelectorInstructions();
			label.start();
			h.add(label);
			changer.setAlignmentX(CENTER_ALIGNMENT);
			label.setAlignmentX(CENTER_ALIGNMENT);
			f.setUndecorated(true);
			f.setContentPane(h);
			f.setSize(screenDimension);
		}
		f.setVisible(true);

	}

	@Override
	public void activateCard(boolean showingSplash) {
		card.show(displayChannelPanel, lastActiveDisplay);
	}
}
