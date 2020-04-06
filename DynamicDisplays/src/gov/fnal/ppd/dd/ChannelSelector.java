package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.INSET_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.docentName;
import static gov.fnal.ppd.dd.GlobalVariables.getFlavor;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getSoftwareVersion;
import static gov.fnal.ppd.dd.GlobalVariables.userHasDoneSomething;
import static gov.fnal.ppd.dd.changer.FileMenu.CHECK_NEW_VERSION_MENU;
import static gov.fnal.ppd.dd.changer.FileMenu.HELP_MENU;
import static gov.fnal.ppd.dd.changer.FileMenu.INFO_BUTTON_MENU;
import static gov.fnal.ppd.dd.changer.FileMenu.REFRESH_MENU;
import static gov.fnal.ppd.dd.util.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.GeneralUtilities.launchErrorMessage;
import static gov.fnal.ppd.dd.util.GeneralUtilities.launchMemoryWatcher;
import static gov.fnal.ppd.dd.util.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.GeneralUtilities.shortDate;
import static gov.fnal.ppd.dd.util.PackageUtilities.getDisplayID;

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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import gov.fnal.ppd.dd.changer.ChangeDefaultsActionListener;
import gov.fnal.ppd.dd.changer.ChannelButtonGrid;
import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.changer.ChannelClassificationDictionary;
import gov.fnal.ppd.dd.changer.ChannelMap;
import gov.fnal.ppd.dd.changer.DDButton;
import gov.fnal.ppd.dd.changer.DDButton.ButtonFieldToUse;
import gov.fnal.ppd.dd.changer.DetailedInformationGrid;
import gov.fnal.ppd.dd.changer.DisplayButtons;
import gov.fnal.ppd.dd.changer.DisplayChangeEvent;
import gov.fnal.ppd.dd.changer.DocentGrid;
import gov.fnal.ppd.dd.changer.FileMenu;
import gov.fnal.ppd.dd.changer.ImageGrid;
import gov.fnal.ppd.dd.changer.InformationBox;
import gov.fnal.ppd.dd.changer.SaveRestoreDefaultChannels;
import gov.fnal.ppd.dd.changer.SimplifiedChannelGrid;
import gov.fnal.ppd.dd.channel.list.ChannelListGUI;
import gov.fnal.ppd.dd.channel.list.CreateListOfChannels;
import gov.fnal.ppd.dd.channel.list.CreateListOfChannelsHelper;
import gov.fnal.ppd.dd.channel.list.ExistingChannelLists;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.display.DisplayFacade;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.CheckDisplayStatus;
import gov.fnal.ppd.dd.util.DisplayButtonGroup;
import gov.fnal.ppd.dd.util.DisplayCardActivator;
import gov.fnal.ppd.dd.util.DisplayKeeper;
import gov.fnal.ppd.dd.util.DownloadNewSoftwareVersion;
import gov.fnal.ppd.dd.util.JLabelFooter;
import gov.fnal.ppd.dd.util.SelectorInstructions;
import gov.fnal.ppd.dd.util.SplashScreens;
import gov.fnal.ppd.dd.util.WhoIsInChatRoom;
import gov.fnal.ppd.dd.util.version.VersionInformation;
import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;
import gov.fnal.ppd.dd.util.version.VersionInformationComparison;

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

	private int								refreshActionUnderway		= 0;

	// private static ActionListener fullRefreshAction = null;
	private ActionListener					channelRefreshAction		= new ActionListener() {
																			@Override
																			public void actionPerformed(ActionEvent e) {
																				println(ChannelSelector.class,
																						": Default, unfunctional channel refresh action called");
																			}
																		};
	private List<List<ChannelButtonGrid>>	channelButtonGridList		= new ArrayList<List<ChannelButtonGrid>>();
	/*
	 * Each tab contains a specific type of Channels in a CardLayout There is one Card for each Display in the CardLayout A button
	 * might not be selected in a Card if that channel is not shown on the panel
	 */

	private Box								titleBox;
	private JLabel							title;
	protected int							selectedTab;
	// The circular arrow, as in the recycling symbol
	// private JButton refreshButton = new JButton("Refresh");
	private JButton							lockButton;
	private DisplayButtons					displaySelector;
	private CardLayout						card						= new CardLayout();
	private JPanel							displayChannelPanel			= new JPanel(card);

	private String							lastActiveDisplay			= null;

	private List<JTabbedPane>				listOfDisplayTabbedPanes	= new ArrayList<JTabbedPane>();

	private SplashScreens					splashScreens;
	private DDButton.ButtonFieldToUse		nowShowing					= ButtonFieldToUse.USE_NAME_FIELD;
	private int								numURLButtonsChanged;

	// Data for the status update
	private VersionInformation				versionInfo					= VersionInformation.getVersionInformation();
	private String							presentStatus;

	private int								myDB_ID;
	private long							lastChannelChange			= System.currentTimeMillis();
	private static final long				startTime					= System.currentTimeMillis();

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
		println(getClass(),
				" -- My screen is " + screenDimension + ". Basic Font_Size=" + FONT_SIZE + ", Inset_Size=" + INSET_SIZE);

	}

	/**
	 * In order to obey the rule-of-thumb to not start threads within the context of a constructor, this start method is provided.
	 */
	public void start() {
		initComponents();
		launchMemoryWatcher();
		launchStatusThread();
	}

	private static SimpleDateFormat	sdf		= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static String			OFFLINE	= "**Off Line**";

	private void launchStatusThread() {
		Timer timer = new Timer("StatusUpdate");

		timer.scheduleAtFixedRate(new TimerTask() {

			public void run() {
				updateControllerStatus();
			}
		}, 500L, 5000L);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				presentStatus = OFFLINE;
				updateControllerStatus();
			}
		});

	}

	private void updateControllerStatus() {
		try {
			long uptime = System.currentTimeMillis() - startTime;
			String query = "UPDATE ControllerStatus SET Time='" + sdf.format(new Date()) + "',";

			if (presentStatus.equals(OFFLINE)) {
				query += " Status='" + OFFLINE + "'";
			} else {
				long delta = (System.currentTimeMillis() - lastChannelChange) / 1000;
				String time = delta + " sec";
				if (delta > 72 * 3600)
					time = (delta / 3600 / 24) + " day";
				else if (delta > 7200)
					time = (delta / 3600) + " hr";
				else if (delta > 120)
					time = (delta / 60) + " min";
				String timeAgo = ", " + time + " ago. ";
				String status = presentStatus.replace("'", "");
				String moreInfo = DisplayFacade.getPresentStatus().replace("'", "");

				int MAX_MESSAGE_SIZE = 2048;
				String theWholeStatus = status + timeAgo + " " + moreInfo;
				if (theWholeStatus.length() > MAX_MESSAGE_SIZE) {
					String theRest = timeAgo + " " + moreInfo;
					if (theRest.length() > (MAX_MESSAGE_SIZE - 100)) {
						theWholeStatus = status + timeAgo;
					} else {
						theWholeStatus = status.substring(0,
								Math.max((MAX_MESSAGE_SIZE - 100), MAX_MESSAGE_SIZE - theRest.length())) + " ..." + theRest;
					}
				}
				if (theWholeStatus.length() > MAX_MESSAGE_SIZE)
					theWholeStatus = theWholeStatus.substring(0, MAX_MESSAGE_SIZE - 5) + " ...";

				query += " Status='" + theWholeStatus + "'";
			}

			query += ", Version='" + versionInfo.getVersionString() + "', Uptime=" + uptime + " WHERE ControllerID=" + myDB_ID;
			Connection connection = ConnectionToDatabase.getDbConnection();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {
					int numRows = stmt.executeUpdate(query);
					if (numRows == 0 || numRows > 1) {
						println(getClass(),
								" Problem while updating status of Controler: Expected to modify exactly one row, but  modified "
										+ numRows + " rows instead. SQL='" + query + "'");
					}
					stmt.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		} catch (Exception ex) {
			// Catch any exception in this method.
			ex.printStackTrace();
		}
	}

	private void initComponents() {
		presentStatus = "Initializing the GUI";
		SaveRestoreDefaultChannels.setup("Change Default Configurations", 30.0f, new Insets(20, 50, 20, 50));

		removeAll();

		displaySelector = new DisplayButtons(this);
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
		presentStatus = "GUI constructed";
	}

	private String getChannelChangeStatus(final Display display) {
		lastChannelChange = System.currentTimeMillis();
		return "Last channel change sent to display " + display.getVirtualDisplayNumber() + "/" + display.getDBDisplayNumber()
				+ " [" + display.getContent() + "] at " + shortDate();
	}

	public static ProgressMonitor progressMonitor;
	private void initializeTabs() {
		ChannelClassification[] categories = ChannelClassificationDictionary.getCategories();

		progressMonitor = new ProgressMonitor(null,
				"Building Channel Selector GUI for location=" + getLocationCode(), "", 0,
				displayList.size() * (1 + categories.length));
		progressMonitor.setNote("Launching");
		String note = "Starting to build image database";
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
			DisplayButtonGroup displayButtonGrooup = new DisplayButtonGroup();

			final List<ChannelButtonGrid> allGrids = new ArrayList<ChannelButtonGrid>();
			synchronized (channelButtonGridList) {
				channelButtonGridList.add(allGrids);
			}

			Component theSelectedTab = null;
			for (int cat = 0; cat < categories.length; cat++) {
				ChannelButtonGrid grid = new DetailedInformationGrid(display, displayButtonGrooup);
				grid.makeGrid(categories[cat]);
				allGrids.add(grid);
				display.addListener(grid);
				display.addListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread() {
							public void run() {
								catchSleep(3000);
								presentStatus = getChannelChangeStatus(display);
							}
						}.run();
					}
				});
				String sp = SHOW_IN_WINDOW ? "" : " ";
				displayTabPane.add(grid, sp + categories[cat].getAbbreviation() + sp);
				note = "Building image database";
				progressMonitor.setNote(note + cat);
				progressMonitor.setProgress(index * (1 + categories.length) + cat);
			}

			ChannelButtonGrid grid = new ImageGrid(display, displayButtonGrooup);
			grid.makeGrid(ChannelClassification.IMAGE);
			allGrids.add(grid);
			display.addListener(grid);
			displayTabPane.add(grid, " Images ");

			if (SHOW_DOCENT_TAB) {
				GetMessagingServer.getDocentNameSelector();
				ChannelButtonGrid gridDocent = new DocentGrid(display, displayButtonGrooup, docentName);
				gridDocent.makeGrid(ChannelClassification.IMAGE);
				allGrids.add(gridDocent);
				display.addListener(gridDocent);

				display.addListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread() {
							public void run() {
								catchSleep(3000);
								presentStatus = getChannelChangeStatus(display);
							}
						}.run();
					}
				});
				displayTabPane.add(gridDocent, " Docent ");
			}

			SimplifiedChannelGrid scg = new SimplifiedChannelGrid(display, displayButtonGrooup);
			scg.makeGrid(null);
			if (scg.hasSpecialChannels()) {
				allGrids.add(scg);
				display.addListener(scg);
				display.addListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread() {
							public void run() {
								catchSleep(3000);
								presentStatus = getChannelChangeStatus(display);
							}
						}.run();
					}
				});
				displayTabPane.add(scg, " Display " + display.getVirtualDisplayNumber() + " channels ");
				theSelectedTab = scg;
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

			boolean useNewList = true;
			if (!useNewList) {
				// The tried-and-true list GUI:
				final CreateListOfChannelsHelper channelListHelper = new CreateListOfChannelsHelper(
						display.getPreferredHighlightColor());
				ActionListener listener = new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						lastChannelChange = System.currentTimeMillis();
						presentStatus = "List " + channelListHelper.lister.getChannelList() + " sent to display " + display;
						println(ChannelSelector.class, " -- Channel list accepted");
						display.setContent(channelListHelper.lister.getChannelList());
					}
				};

				displayTabPane.add(CreateListOfChannels.getContainer(channelListHelper, listener), " Lists ");

			} else {
				// The new List GUI that allows lists to contain the same channel many times.
				final ChannelListGUI clg = new ChannelListGUI();
				ActionListener listener = new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						println(ChannelSelector.class, " -- Channel list accepted");
						display.setContent(clg.getChannelList());
					}
				};
				clg.setListener(listener);
				clg.setBackgroundBypass(display.getPreferredHighlightColor());
				displayTabPane.add(clg, " Create or edit a List ");

				ExistingChannelLists ecl = new ExistingChannelLists(display, displayButtonGrooup);
				ecl.setBackground(display.getPreferredHighlightColor());
				displayTabPane.add(ecl, " Existing Lists ");

				clg.setNewListCreationCallback(ecl);
			}

			if (theSelectedTab != null)
				displayTabPane.setSelectedComponent(theSelectedTab);

			// Add the Display Tabbed Pane to the main screen
			inner.setBorder(
					BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(display.getPreferredHighlightColor(), wid1),
							BorderFactory.createEmptyBorder(wid2, wid2, wid2, wid2)));
			inner.add(displayTabPane, BorderLayout.CENTER);
			final JLabelFooter footer = makeChannelIndicator(display);
			display.addListener(new ActionListener() {
				private long lastUpdated = System.currentTimeMillis();

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
						text = "Display " + display.getVirtualDisplayNumber() + " ERROR; "
								+ display.getContent().getChannelClassification() + "/'" + (display.getContent().getName()) + "'";
						// System.out.println("ChannelSelector error from display: " + text);
						launchErrorMessage(e);
						break;

					case ALIVE:
						// These remaining items are not ever reached. (1/28/2015)
						alive = true;
						text = "Display " + display.getVirtualDisplayNumber() + ": "
								+ display.getContent().getChannelClassification() + "/'" + (display.getContent().getName()) + " (#"
								+ ((Channel) display.getContent()).getNumber() + ")";
						break;

					case IDLE:
						alive = true;
						if (lastUpdated + 30000l > System.currentTimeMillis())
							break;
						text = "Display " + display.getVirtualDisplayNumber() + " Idle; " + ": "
								+ display.getContent().getChannelClassification() + "/'" + (display.getContent().getName());
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
			displayButtonGrooup.setActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					setTabColor(((DDButton) e.getSource()).getDisplay(), fi);
				}
			});
			setTabColor(display, index);

			// Set up a database check every now and then to see what a Display is actually doing (this is inSTEAD of the Ping
			// thread, above).
			new CheckDisplayStatus(display, index, footer).start();
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
			// Enable all of this display's Channel buttons, too
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
		title = new JLabel(" Dynamic Display 00, V0.0.0 ");
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
		int rp = 5;

		FileMenu fmBar = new FileMenu(this, new ChangeDefaultsActionListener(this));
		fmBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED),
				BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(rp, rp, rp, rp), fmBar.getBorder())));
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

		fmBar.setBackground(c);

		JLabel versionIdentifier = new JLabel("V" + getSoftwareVersion());
		versionIdentifier.setFont(new Font("Arial", Font.PLAIN, 8));
		versionIdentifier.setOpaque(false);

		fmBar.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		title.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		versionIdentifier.setAlignmentY(JComponent.CENTER_ALIGNMENT);

		titleBox.add(fmBar);

		// titleBox.add(Box.createRigidArea(new Dimension(10,10)));
		titleBox.add(Box.createHorizontalGlue());
		titleBox.add(title);
		titleBox.add(Box.createHorizontalGlue());

		titleBox.add(versionIdentifier);
		titleBox.add(Box.createRigidArea(new Dimension(rp, rp)));

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
		}

		titleBox.setOpaque(true);
	}

	private static JLabelFooter makeChannelIndicator(Display display) {
		JLabelFooter footer = new JLabelFooter("Display " + display.getVirtualDisplayNumber());
		DisplayButtons.setToolTip(display);

		if (!SHOW_IN_WINDOW) {
			footer.setFont(new Font("Arial", Font.PLAIN, 18));
		}
		footer.setAlignmentX(CENTER_ALIGNMENT);
		return footer;
	}

	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {

		case REFRESH_MENU:
			String mess = "<html>When you hit OK here, the Channel Selector will exit and start again."
					+ "<br>This will refresh the ChannelSelector with all the new channels"
					+ "<br>that have been added to the system recently.<br><br>Do you want to do this now?<br></html>";
			Object[] options = { "OK: Restart ChannelSelector", "Cancel" };
			int n = JOptionPane.showOptionDialog(ChannelSelector.this, mess, "Restart the ChannelSelector?",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if (n == 0)
				System.exit(-1);
			new Thread("RefreshURLPopupWait2") {
				public void run() {
					refreshActionUnderway = 1;
					while (refreshActionUnderway > 0)
						catchSleep(1000);
					int numURLs = numURLButtonsChanged / displayList.size();
					String mess = "<html>The URLs of all the channels have been refreshed, " + numURLButtonsChanged
							+ " URL buttons changed.";
					if (numURLButtonsChanged > 0)
						mess += "  <br>This is " + numURLs + " specific URLs that changed, distributed over the "
								+ displayList.size() + " displays in the system";
					new InformationBox((SHOW_IN_WINDOW ? 0.7f : 1.0f), "Channels refreshed", mess + "</html>");
					presentStatus = "Refresh of GUI requested by user at " + new java.util.Date();
					lastChannelChange = System.currentTimeMillis();
				}
			}.start();

			channelRefreshAction.actionPerformed(e);
			break;

		case INFO_BUTTON_MENU:
			nowShowing = nowShowing.next();
			userHasDoneSomething();

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

			break;

		case HELP_MENU:
			String text = SelectorInstructions.getInstructions().replace("</html>",
					"<br><br>For more information, see <u>https://dynamicdisplays.fnal.gov/about.php</u></html>");
			JOptionPane.showMessageDialog(ChannelSelector.this, text, "The Dynamic Displays Channel Selector",
					JOptionPane.PLAIN_MESSAGE);
			break;

		case CHECK_NEW_VERSION_MENU:
			FLAVOR flavor = getFlavor(true);
			VersionInformation me = VersionInformation.getVersionInformation();
			if (VersionInformationComparison.lookup(flavor, true)) {
				VersionInformation them = VersionInformation.getDBVersionInformation(flavor);
				Object[] o2 = { "YES: Download new software and restart (recommended)", "No, not now" };
				if (JOptionPane.showOptionDialog(this, "Download and restart now?",
						"There is a new " + flavor + " version of the software.\nWe are running version " + me.getVersionString()
								+ "\nThe newest version available is " + them.getVersionString() + "\n\nUpdate right now?",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, o2, o2[1]) == 0) {
					// Update now
					new DownloadNewSoftwareVersion(them.getVersionString());
					// this call will exit the JVM, so nothing should fall here.
				} else {
					JOptionPane.showMessageDialog(this, "This update should happen soon. Please say 'yes' next time.",
							"Update postponed", JOptionPane.PLAIN_MESSAGE);
				}
			} else {
				// there is not a new version
				JOptionPane.showMessageDialog(this,
						"We are running version " + me.getVersionString()
								+ " of the Dynamic Displays software.\nThis is the latest " + flavor + " version.",
						"No need to update", JOptionPane.PLAIN_MESSAGE);
			}
			break;

		default:
			// The display selection has been changed, Select the "card" that shows this panel
			card.show(displayChannelPanel, e.getActionCommand());
			lastActiveDisplay = e.getActionCommand();

			int displayNum = e.getID();
			adjustTitle(displayList.get(displayNum));
			setTabColor(displayList.get(displayNum), displayNum);
			userHasDoneSomething();
			break;
		}
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
	 * 
	 */
	public void createRefreshActions() {
		channelRefreshAction = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				refreshActionUnderway = channelButtonGridList.size();
				println(ChannelSelector.class, ": Preparing to refresh all of the URLs of the channel buttons for the "
						+ refreshActionUnderway + " displays in the system.");
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						catchSleep(100);
						println(ChannelSelector.class, ": Refreshing URLs of the channel buttons in this world");
						userHasDoneSomething();

						Collection<SignageContent> list = ((ChannelMap) ChannelCatalogFactory.refresh()).values();

						// TODO - refresh the channel lists, too (not sure how to do that)

						// list contains all the channels from the database Now check the URLs in the DB versus the ones we have now
						// that are attached to all the buttons in our realm
						int numButtons = 0;
						numURLButtonsChanged = 0;
						synchronized (channelButtonGridList) {
							for (List<ChannelButtonGrid> gridList : channelButtonGridList) {
								refreshActionUnderway--;
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
						println(ChannelSelector.class,
								": Looked at " + numButtons + " buttons and changed " + numURLButtonsChanged + " URLs");
						refreshActionUnderway = 0;
					}
				});
			}
		};
		// refreshButton.addActionListener(channelRefreshAction);
	}

	protected void destroy() {
		// Do everything we can to forget everything we can.
		// TODO -- Make this work (no luck so far!)
	}

	// private void setRefreshAction(ActionListener refreshAction2) {
	// refreshButton.addActionListener(refreshAction2);
	// }

	@Override
	public void activateCard(boolean showingSplash) {
		if (showingSplash) {
			card.show(displayChannelPanel, lastActiveDisplay);
		} else {
			card.show(displayChannelPanel, splashScreens.getNext());
		}
	}

	public void setSelectorID(int m) {
		myDB_ID = m;
	}

}
