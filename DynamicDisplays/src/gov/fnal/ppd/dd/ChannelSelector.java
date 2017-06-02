package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.INSET_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.IS_DOCENT_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.PRIVATE_KEY_LOCATION;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.docentName;
import static gov.fnal.ppd.dd.GlobalVariables.getFullSelectorName;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.lastDisplayChange;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.getDisplayID;
import static gov.fnal.ppd.dd.util.Util.launchErrorMessage;
import static gov.fnal.ppd.dd.util.Util.launchMemoryWatcher;
import static gov.fnal.ppd.dd.util.Util.println;
import gov.fnal.ppd.dd.changer.CategoryDictionary;
import gov.fnal.ppd.dd.changer.ChannelButtonGrid;
import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.ChannelsFromDatabase;
import gov.fnal.ppd.dd.changer.DDButton;
import gov.fnal.ppd.dd.changer.DetailedInformationGrid;
import gov.fnal.ppd.dd.changer.DisplayButtons;
import gov.fnal.ppd.dd.changer.DisplayChangeEvent;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.changer.DocentGrid;
import gov.fnal.ppd.dd.changer.ImageGrid;
import gov.fnal.ppd.dd.changer.InformationBox;
import gov.fnal.ppd.dd.changer.SaveRestoreDefaultChannels;
import gov.fnal.ppd.dd.channel.CreateListOfChannels;
import gov.fnal.ppd.dd.channel.CreateListOfChannelsHelper;
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.CheckDisplayStatus;
import gov.fnal.ppd.dd.util.DisplayButtonGroup;
import gov.fnal.ppd.dd.util.DisplayCardActivator;
import gov.fnal.ppd.dd.util.DisplayKeeper;
import gov.fnal.ppd.dd.util.JLabelFooter;
import gov.fnal.ppd.dd.util.SelectorInstructions;
import gov.fnal.ppd.dd.util.SplashScreens;
import gov.fnal.ppd.dd.util.WhoIsInChatRoom;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * <p>
 * Allows the user to select the Channel that is being displayed on each Display in the Dynamic Displays system
 * </p>
 * <p>
 * This is the main GUI class for performing the Channel selection on the Displays. The configuration of the GUI is determined by a
 * series of system parameters, which are held in {@link gov.fnal.ppd.dd.GlobalVariables}:
 * <ul>
 * <li>{@link gov.fnal.ppd.dd.GlobalVariables#SHOW_IN_WINDOW} -- false means make it full screen</li>
 * <li>{@link gov.fnal.ppd.dd.GlobalVariables#IS_PUBLIC_CONTROLLER} -- false mean show all possible channels</li>
 * </ul>
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2013-14
 */
public class ChannelSelector extends JPanel implements ActionListener, DisplayCardActivator, DisplayKeeper {

	private static final long				serialVersionUID			= 5044030472140151291L;

	/**
	 * Soes this instance (of the Channel selector) need to show the docent tab?
	 */
	public static boolean					SHOW_DOCENT_TAB				= false;

	/**
	 * 
	 */
	public static final Dimension			screenDimension				= Toolkit.getDefaultToolkit().getScreenSize();
	private static JFrame					f							= new JFrame("Dynamic Display Channel Selector");

	// private static ActionListener fullRefreshAction = null;
	private static ActionListener			channelRefreshAction		= new ActionListener() {
																			@Override
																			public void actionPerformed(ActionEvent e) {
																				println(ChannelSelector.class,
																						": Default, unfunctional channel refresh action called");
																			}
																		};
	private static ChannelSelector			channelSelector;

	private List<List<ChannelButtonGrid>>	channelButtonGridList		= new ArrayList<List<ChannelButtonGrid>>();
	/*
	 * Each tab contains a specific type of Channels in a CardLayout There is one Card for each Display in the CardLayout A button
	 * might not be selected in a Card if that channel is not shown on the panel
	 */

	private Box								titleBox;
	private JLabel							title;
	protected int							selectedTab;
	// The circular arrow, as in the recycling symbol
	private JButton							refreshButton				= new JButton("↺");
	private JButton							exitButton					= new JButton("X");
	private JButton							lockButton;
	private JButton							changeDefaultsButton		= new JButton("S/R");
	// private JButton addChannelButton = new JButton("+");
	// private JCheckBox showBorderButton = new JCheckBox("Border?");
	private DisplayButtons					displaySelector;
	private CardLayout						card						= new CardLayout();
	private JPanel							displayChannelPanel			= new JPanel(card);

	private String							lastActiveDisplay			= null;

	private List<JTabbedPane>				listOfDisplayTabbedPanes	= new ArrayList<JTabbedPane>();

	private SplashScreens					splashScreens;
	private int								nowShowing					= DDButton.USE_NAME_FIELD;
	private int								numURLButtonsChanged;

	private ActionListener					changeDefaultsListener		= new ActionListener() {
																			boolean	visible	= false, firstTime = true;
																			JFrame	f		= new JFrame(
																									"SaveRestoreDefaultChannels");

																			@Override
																			public void actionPerformed(ActionEvent e) {
																				if (firstTime) {
																					firstTime = false;
																					f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
																					f.setContentPane(SaveRestoreDefaultChannels
																							.getGUI());
																					int height = 400 + 30 * (displayList.size() + 1);
																					if (height > 1000)
																						height = 1000;
																					println(ChannelSelector.class,
																							" -- Height of popup is " + height);
																					f.setSize(900, height);
																					f.addWindowListener(new WindowListener() {

																						@Override
																						public void windowOpened(WindowEvent e) {
																						}

																						@Override
																						public void windowIconified(WindowEvent e) {
																							visible = false;
																						}

																						@Override
																						public void windowDeiconified(WindowEvent e) {
																							visible = true;
																						}

																						@Override
																						public void windowDeactivated(WindowEvent e) {
																						}

																						@Override
																						public void windowClosing(WindowEvent e) {
																						}

																						@Override
																						public void windowClosed(WindowEvent e) {
																							visible = false;
																							f.setVisible(false);
																						}

																						@Override
																						public void windowActivated(WindowEvent e) {
																						}
																					});
																				}
																				visible = !visible;
																				f.setVisible(visible);
																				if (visible) {
																					f.toFront();
																					f.repaint();
																				}
																			}
																		};

	/**
	 * Create the channel selector GUI in the normal way
	 */
	public ChannelSelector() {
		super(new BorderLayout());

		// Determine the size of the screen and thus the appropriate font size.

		if (SHOW_IN_WINDOW) {
			FONT_SIZE = 30.0f;
			INSET_SIZE = 3;
		} else {
			int width = screenDimension.width;
			int height = screenDimension.height;

			if (height < 500) {
				FONT_SIZE = 14.0f;
				INSET_SIZE = 1;
			} else if (width < 801 || height < 600) {
				FONT_SIZE = 18.0f;
				INSET_SIZE = 1;
			} else if (width < 1000 || height < 800) {
				// An atom-based thing falls here (2015 testing)
				FONT_SIZE = 24.0f;
				INSET_SIZE = 2;
			} else if (width < 1400 || height < 1000) {
				FONT_SIZE = 30.0f;
				INSET_SIZE = 4;
			} else if (width < 2880 || height < 1620) {
				// 1080p (HD; "2K") displays fall here.
				FONT_SIZE = 34.0f;
				INSET_SIZE = 6;
			} else if (width < 3840 || height < 2160) {
				// "3K" displays
				FONT_SIZE = 36.0f;
				INSET_SIZE = 8;
			} else {
				// 4K and bigger
				FONT_SIZE = 40.0f;
				INSET_SIZE = 10;
			}
		}
		println(getClass(), " -- My screen is " + screenDimension + ". Basic Font_Size=" + FONT_SIZE + ", Inset_Size=" + INSET_SIZE);

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
		lastActiveDisplay = getDisplayID(displaySelector.getFirstDisplay());
		// initChannelSelectors();
		initializeTabs();

		add(displayChannelPanel, BorderLayout.CENTER);
		if (SHOW_IN_WINDOW) {
			int height = screenDimension.height;
			if (height < 1000)
				displayChannelPanel.setPreferredSize(new Dimension(600, 525));
			else if (height < 1500)
				displayChannelPanel.setPreferredSize(new Dimension(700, 640));
		}

		add(displaySelector, BorderLayout.EAST);
		add(makeTitle(), BorderLayout.NORTH);

		splashScreens = new SplashScreens(this, displayChannelPanel);

		if (!SHOW_IN_WINDOW)
			// Only enable the splash screen for the full-screen version
			splashScreens.start();

	}

	private void initializeTabs() {
		ChannelCategory[] categories = CategoryDictionary.getCategories();

		ProgressMonitor progressMonitor = new ProgressMonitor(null, "Building Channel Selector GUI", "", 0, displayList.size()
				* (1 + categories.length));
		String note = "Building image database";
		progressMonitor.setNote(note);
		progressMonitor.setProgress(0);

		catchSleep(100); // Let the progress monitor start

		int index = 0;
		for (final Display display : displayList) {
			progressMonitor.setNote(note);
			progressMonitor.setProgress(index * (1 + categories.length));

			// NOTE: Each display has the same set of tabs and channels to offer.

			final JTabbedPane displayTabPane = new JTabbedPane();
			listOfDisplayTabbedPanes.add(displayTabPane);
			float sz = 12.0f;
			if (!SHOW_IN_WINDOW)
				sz = categories.length > 7 ? 30.0f - 2 * (categories.length - 7) : 30.0f;

			displayTabPane.setFont(getFont().deriveFont(sz));
			displayTabPane.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					selectedTab = ((JTabbedPane) e.getSource()).getSelectedIndex();
				}
			});
			DisplayButtonGroup bg = new DisplayButtonGroup();

			final List<ChannelButtonGrid> allGrids = new ArrayList<ChannelButtonGrid>();
			synchronized (channelButtonGridList) {
				channelButtonGridList.add(allGrids);
			}

			Component theSelectedTab = null;
			for (int cat = 0; cat < categories.length; cat++) {
				ChannelButtonGrid grid = new DetailedInformationGrid(display, bg);
				grid.makeGrid(categories[cat]);
				allGrids.add(grid);
				display.addListener(grid);
				String sp = SHOW_IN_WINDOW ? "" : " ";
				displayTabPane.add(grid, sp + categories[cat].getAbbreviation() + sp);
				if ("Public".equalsIgnoreCase(categories[cat].getAbbreviation()))
					theSelectedTab = grid;
				progressMonitor.setNote(note);
				progressMonitor.setProgress(index * (1 + categories.length) + cat);
			}

			ChannelButtonGrid grid = new ImageGrid(display, bg);
			grid.makeGrid(ChannelCategory.IMAGE);
			allGrids.add(grid);
			display.addListener(grid);
			displayTabPane.add(grid, " Images ");

			if (SHOW_DOCENT_TAB) {
				GetMessagingServer.getDocentNameSelector();
				grid = new DocentGrid(display, bg, docentName);
				grid.makeGrid(ChannelCategory.IMAGE);
				allGrids.add(grid);
				display.addListener(grid);
				displayTabPane.add(grid, " Docent ");
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
					lastDisplayChange = System.currentTimeMillis();
					setAllTabs(displayTabPane.getSelectedIndex());
				}
			});

			final CreateListOfChannelsHelper channelListHelper = new CreateListOfChannelsHelper(display.getPreferredHighlightColor());
			ActionListener listener = new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					println(ChannelSelector.class, " -- Channel list accepted");
					display.setContent(channelListHelper.lister.getChannelList());
				}
			};

			displayTabPane.add(CreateListOfChannels.getContainer(channelListHelper, listener),  " Lists ");
			
			// The new List GUI that allows lists to contain the same channel many times.
			// ChannelListGUI clg = new ChannelListGUI();
			// clg.setBackgroundBypass(display.getPreferredHighlightColor());
			// displayTabPane.add(clg, " ListsN ");

			if (theSelectedTab != null)
				displayTabPane.setSelectedComponent(theSelectedTab);

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
					// System.out.println(ChannelSelector.class.getSimpleName() + ".actionPerformed(), event=" + e);
					DisplayChangeEvent ev = (DisplayChangeEvent) e;
					// catchSleep(100);

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
						text = "Display " + display.getVirtualDisplayNumber() + " ERROR; " + display.getContent().getCategory()
								+ "/'" + (display.getContent().getName()) + "'";
						// System.out.println("ChannelSelector error from display: " + text);
						launchErrorMessage(e);
						break;

					case ALIVE:
						// These remaining items are not ever reached. (1/28/2015)
						alive = true;
						text = "Display " + display.getVirtualDisplayNumber() + ": " + display.getContent().getCategory() + "/'"
								+ (display.getContent().getName()) + " (#" + ((Channel) display.getContent()).getNumber() + ")";
						break;

					case IDLE:
						alive = true;
						if (lastUpdated + 30000l > System.currentTimeMillis())
							break;
						text = "Display " + display.getVirtualDisplayNumber() + " Idle; " + ": "
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

			note = "Building the rest of the GUI";
			progressMonitor.setNote(note);
			progressMonitor.setProgress(index * (1 + categories.length) + categories.length);

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

		progressMonitor.close();
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
		// println(getClass(), ": Display " + number + (alive ? " is alive" : " is NOT alive"));
		displaySelector.setIsAlive(number, alive);

		synchronized (channelButtonGridList) {
			// Enable the Channel buttons, too
			for (List<ChannelButtonGrid> allGrids : channelButtonGridList)
				if (allGrids.get(0).getDisplay().getDBDisplayNumber() == number)
					for (ChannelButtonGrid cbg : allGrids)
						if (cbg != null)
							cbg.setAlive(alive);
		}
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
			title.setFont(new Font("Serif", Font.ITALIC, (int) (FONT_SIZE / 2)));
			refreshButton.setFont(new Font("SansSerif", Font.BOLD, (int) (FONT_SIZE / 2)));
		}
		title.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		title.setOpaque(true);
		title.setBackground(Color.white);
		titleBox = Box.createHorizontalBox();
		adjustTitle(displayList.get(0));

		// Add this in when I get it to work!! refreshButton.addActionListener(fullRefreshAction);
		refreshButton.addActionListener(channelRefreshAction);
		refreshButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread("RefreshURLPopupWait") {
					public void run() {
						catchSleep(2000);
						int numURLs = numURLButtonsChanged / displayList.size();
						String mess = "<html>The URLs of all the channels have been refreshed, " + numURLButtonsChanged
								+ " URL buttons changed.";
						if (numURLButtonsChanged > 0)
							mess += "  <br>This is " + numURLs + " specific URLs that changed, distributed over the "
									+ displayList.size() + " displays in the system";
						new InformationBox((SHOW_IN_WINDOW ? 0.7f : 1.0f), refreshButton, "Channels refreshed", mess + "</html>");
					}
				}.start();
			}
		});

		SaveRestoreDefaultChannels.setup("Change Default Configurations", 30.0f, new Insets(20, 50, 20, 50));

		changeDefaultsButton.addActionListener(changeDefaultsListener);

		changeDefaultsButton.setToolTipText("Save, restore, and/or change the configuration defaults");
		changeDefaultsButton.setFont(changeDefaultsButton.getFont().deriveFont(FONT_SIZE / 3));

		// Create an exit button
		exitButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JLabel message = new JLabel("<html>Do you <em>really</em> want to exit the<br>Channel Selector Application?</html>");
				Font f = message.getFont();
				message.setFont(new Font(f.getName(), Font.BOLD, f.getSize() * 2));

				if (JOptionPane.showConfirmDialog(exitButton, message, "Exit?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
					System.exit(0);
			}
		});
		Border bor = exitButton.getBorder();
		exitButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 5), bor));
		exitButton.setBackground(new Color(255, 200, 200));
		exitButton.setFont(new Font("SansSerif", Font.BOLD, (int) (FONT_SIZE / 3)));
		exitButton.setMargin(new Insets(6, 6, 6, 6));

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
		int wid = (SHOW_IN_WINDOW ? 1 : 4);
		titleBox.setBorder(BorderFactory.createLineBorder(c, wid));

		if (!IS_PUBLIC_CONTROLLER) {
			refreshButton.setToolTipText("<html><b>Refresh URLs</b> -- Refresh the URLs on each Channel button "
					+ "<br>by re-reading this information from the Channel database."
					+ "<br><em>This will not create new buttons</em></html>");
			refreshButton.setFont(refreshButton.getFont().deriveFont(FONT_SIZE / 3));
			refreshButton.setMargin(new Insets(8, 4, 8, 4));
			titleBox.add(refreshButton);
			titleBox.add(Box.createRigidArea(new Dimension(5, 5)));

			if (displayList.size() > 1) {
				changeDefaultsButton.setMargin(new Insets(8, 4, 8, 4));
				titleBox.add(changeDefaultsButton);
				titleBox.add(Box.createRigidArea(new Dimension(5, 5)));
			}

			if (SHOW_IN_WINDOW)
				IdentifyAll.setup("Identify all displays", FONT_SIZE / 3, new Insets(8, 4, 8, 4));
			else
				IdentifyAll.setup("ID", FONT_SIZE / 2, new Insets(6, 4, 6, 4));

			IdentifyAll.setListOfDisplays(displayList);
			JButton idAll = IdentifyAll.getButton();
			titleBox.add(idAll);
			idAll.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					new InformationBox((SHOW_IN_WINDOW ? 0.7f : 1.0f), refreshButton, "Displays Identified",
							"Each Display should be showing a self-identify splash screen now.");
				}
			});
		}

		ImageIcon ic = new ImageIcon("bin/gov/fnal/ppd/dd/images/2arrows.png");
		JButton flipTitles = new JButton(ic);
		if (SHOW_IN_WINDOW)
			flipTitles.setMargin(new Insets(2, 5, 2, 5));
		else
			flipTitles.setMargin(new Insets(2, 5, 2, 5)); // Same
		flipTitles.setToolTipText("<html><b>Toggle Text</b> -- Toggle between a succinct"
				+ "<br>Channel button label and one with more details");

		flipTitles.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				nowShowing = (nowShowing == DDButton.USE_NAME_FIELD ? DDButton.USE_DESCRIPTION_FIELD : DDButton.USE_NAME_FIELD);

				synchronized (channelButtonGridList) {
					for (List<ChannelButtonGrid> gridList : channelButtonGridList) {
						for (ChannelButtonGrid grid : gridList) {
							DisplayButtonGroup dbg = grid.getBg();
							for (int i = 0; i < dbg.getNumButtons(); i++) {
								DDButton ddb = dbg.getAButton(i);
								ddb.setText(nowShowing);
							}
						}
					}
				}
			}
		});
		titleBox.add(Box.createRigidArea(new Dimension(5, 5)));
		titleBox.add(flipTitles);
		titleBox.add(Box.createHorizontalGlue());
		titleBox.add(title);
		titleBox.add(Box.createHorizontalGlue());

		// Do not let the simple public controller exit
		if (!SHOW_IN_WINDOW) {
			if (lockButton == null) {
				// Observation on 9/15/2014 -- A user of the touch screen wanted to "go back" to the pretty pictures. This lock
				// button accomplishes this task
				ImageIcon icon;

				// if (SHOW_IN_WINDOW)
				// icon = new ImageIcon("src/gov/fnal/ppd/images/lock20.jpg");
				// else
				icon = new ImageIcon("bin/gov/fnal/ppd/dd/images/lock40.jpg");

				lockButton = new JButton(icon);
				lockButton.setMargin(new Insets(2, 5, 2, 5));
				lockButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						activateCard(false);
					}
				});
			}
			titleBox.add(lockButton);

			// Do not add the exit button to the full-screen version for a truly public controller.
			if (IS_DOCENT_CONTROLLER) {
				titleBox.add(Box.createRigidArea(new Dimension(5, 5)));
				titleBox.add(exitButton);
			}
			// } else {
			// titleBox.add(addChannelButton);
		}

		titleBox.setOpaque(true);
	}

	private static JLabelFooter makeChannelIndicator(Display display) {
		// JLabel footer = new JLabel(" Display " + display.getNumber() + " set to the '" + display.getContent().getCategory()
		// + "' channel '" + (display.getContent().getName()) + "' (" + shortDate() + ")");
		JLabelFooter footer = new JLabelFooter("Display " + display.getVirtualDisplayNumber());
		DisplayButtons.setToolTip(display);

		if (!SHOW_IN_WINDOW) {
			footer.setFont(new Font("Arial", Font.PLAIN, 18));
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
		lastDisplayChange = System.currentTimeMillis();
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
	 * Run the Channel Selector application. Renamed from "main" so it cannot (easily) be run
	 * 
	 * @param args
	 */
	public static void oldMain(final String[] args) {

		final SignageType sType = (IS_PUBLIC_CONTROLLER ? SignageType.Public : SignageType.XOC);

		MessageCarrier.initializeSignature();
		println(ChannelSelector.class, ": Initialized our digital signature from '" + PRIVATE_KEY_LOCATION + "'.");
		println(ChannelSelector.class, ": Expect my client name to be '" + getFullSelectorName() + "'\n");

		displayList = DisplayListFactory.getInstance(sType, getLocationCode());
		channelSelector = new ChannelSelector();
		channelSelector.start();

		// SHOW_IN_WINDOW = args.length > 0 && "WINDOW".equals(args[0]);

		channelSelector.createRefreshActions();

		channelSelector.setRefreshAction(channelRefreshAction);

		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		if (SHOW_IN_WINDOW) {
			f.setContentPane(channelSelector);
			f.pack();
		} else {
			// Full-screen display of this app!
			Box h = Box.createVerticalBox();
			h.add(channelSelector);
			SelectorInstructions label = new SelectorInstructions();
			label.start();
			h.add(label);
			channelSelector.setAlignmentX(CENTER_ALIGNMENT);
			label.setAlignmentX(CENTER_ALIGNMENT);
			f.setUndecorated(true);
			f.setContentPane(h);
			f.setSize(screenDimension);
		}
		f.setVisible(true);

	}

	/**
	 * 
	 */
	public void createRefreshActions() {
		channelRefreshAction = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				println(ChannelSelector.class, ": Preparing to refreshing all of the URLs of the channel buttons in this world");
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						println(ChannelSelector.class, ": Refreshing URLs of the channel buttons in this world");

						Collection<SignageContent> list = ((ChannelsFromDatabase) ChannelCatalogFactory.refresh()).values();

						// list contains all the channels from the database Now check the URLs in the DB versus the ones we have now
						// that are attached to all the buttons in our realm
						int numButtons = 0;
						numURLButtonsChanged = 0;
						synchronized (channelButtonGridList) {
							for (List<ChannelButtonGrid> gridList : channelButtonGridList) {
								for (ChannelButtonGrid grid : gridList) {
									DisplayButtonGroup dbg = grid.getBg();
									for (int i = 0; i < dbg.getNumButtons(); i++) {
										numButtons++;
										DDButton ddb = dbg.getAButton(i);
										int channelNumber = ddb.getChannel().getNumber();
										if (channelNumber <= 0)
											// This is a flag that this is some sort of special channel. At this time (1/28/2016),
											// channel 0 is used by the individual photos "channels". It will be appropriate (in the
											// future) to pay attention to these channels, too, in this update. But for now, things
											// are not set up to deal with this.
											continue;
										// Loop over the new channels to see if the URL has changed for the exiting channel
										for (SignageContent CONTENT : list)
											if (CONTENT instanceof Channel) {
												Channel chan = (Channel) CONTENT;
												if (chan.getNumber() == channelNumber) {
													if (!ddb.getChannel().getURI().toString().equals(chan.getURI().toString())) {
														println(ChannelSelector.class,
																": " + channelNumber + "/" + chan.getNumber() + " URI '"
																		+ ddb.getChannel().getURI() + "' changed to '"
																		+ chan.getURI() + "'");
														ddb.getChannel().setURI(chan.getURI());
														numURLButtonsChanged++;
													}
												}
												// if (channelNumber == 75 || chan.getNumber() == 75)
												// System.out.println("Existing button for chan #" + channelNumber + " says URL="
												// + ddb.getChannel().getURI().toString() + "\n\t\tNew information for channel #"
												// + chan.getNumber() + " is " + chan.getURI());
											} else
												println(ChannelSelector.class, ": Hmmm.  Got a "
														+ CONTENT.getClass().getCanonicalName() + " instead of a Channel");
									}
								}
							}
						}
						println(ChannelSelector.class, ": Looked at " + numButtons + " buttons and changed " + numURLButtonsChanged
								+ " URLs");
					}
				});
			}
		};
		refreshButton.addActionListener(channelRefreshAction);
	}

	protected void destroy() {
		// Do everything we can to forget everything we can.
		// TODO Make this work -- no luck so far!
	}

	private void setRefreshAction(ActionListener refreshAction2) {
		refreshButton.addActionListener(refreshAction2);
	}

	@Override
	public void activateCard(boolean showingSplash) {
		if (showingSplash) {
			card.show(displayChannelPanel, lastActiveDisplay);
		} else {
			card.show(displayChannelPanel, splashScreens.getNext());
		}
	}

}
