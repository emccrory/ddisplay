package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.INSET_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_MINUTE;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.docentName;
import static gov.fnal.ppd.dd.GlobalVariables.getFlavor;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.GlobalVariables.getSoftwareVersion;
import static gov.fnal.ppd.dd.GlobalVariables.userHasDoneSomething;
import static gov.fnal.ppd.dd.util.guiUtils.ChannelSelectorFileMenu.CHECK_NEW_VERSION_MENU;
import static gov.fnal.ppd.dd.util.guiUtils.ChannelSelectorFileMenu.HELP_MENU;
import static gov.fnal.ppd.dd.util.guiUtils.ChannelSelectorFileMenu.INFO_BUTTON_MENU;
import static gov.fnal.ppd.dd.util.guiUtils.ChannelSelectorFileMenu.REFRESH_MENU;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.launchErrorMessage;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.launchMemoryWatcher;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.shortDate;
import static gov.fnal.ppd.dd.util.specific.PackageUtilities.getDisplayID;

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
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import gov.fnal.ppd.dd.changer.ChangeDefaultsActionListener;
import gov.fnal.ppd.dd.changer.ChannelButtonGrid;
import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.changer.ChannelClassificationDictionary;
import gov.fnal.ppd.dd.changer.DDButton;
import gov.fnal.ppd.dd.changer.DDButton.ButtonFieldToUse;
import gov.fnal.ppd.dd.changer.DetailedInformationGrid;
import gov.fnal.ppd.dd.changer.DisplayButtons;
import gov.fnal.ppd.dd.changer.DisplayChangeEvent;
import gov.fnal.ppd.dd.changer.DocentGrid;
import gov.fnal.ppd.dd.changer.ImageGrid;
import gov.fnal.ppd.dd.changer.SaveRestoreDefaultChannels;
import gov.fnal.ppd.dd.changer.SimplifiedChannelGrid;
import gov.fnal.ppd.dd.channel.list.ChannelListGUI;
import gov.fnal.ppd.dd.channel.list.CreateListOfChannels;
import gov.fnal.ppd.dd.channel.list.CreateListOfChannelsHelper;
import gov.fnal.ppd.dd.channel.list.ExistingChannelLists;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.display.DisplayFacade;
import gov.fnal.ppd.dd.interfaces.DisplayCardActivator;
import gov.fnal.ppd.dd.interfaces.DisplayKeeper;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.guiUtils.ChannelSelectorFileMenu;
import gov.fnal.ppd.dd.util.guiUtils.JLabelFooter;
import gov.fnal.ppd.dd.util.guiUtils.SplashScreens;
import gov.fnal.ppd.dd.util.nonguiUtils.JavaChangeListener;
import gov.fnal.ppd.dd.util.nonguiUtils.JavaVersion;
import gov.fnal.ppd.dd.util.specific.CheckDisplayStatus;
import gov.fnal.ppd.dd.util.specific.DisplayButtonGroup;
import gov.fnal.ppd.dd.util.specific.DownloadNewSoftwareVersion;
import gov.fnal.ppd.dd.util.specific.SelectorInstructions;
import gov.fnal.ppd.dd.util.specific.WhoIsInChatRoom;
import gov.fnal.ppd.dd.util.version.VersionInformation;
import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;
import gov.fnal.ppd.dd.util.version.VersionInformationComparison;

/**
 * <p>
 * This is the class that is used to create the GUI for the so-called Dynamic Displays Channel Selector.
 * </p>
 * <p>
 * Allows the user to select the Channel that is being displayed on each Display in the Dynamic Displays system
 * </p>
 * <p>
 * The configuration of the GUI is determined by a series of system parameters, which are held in
 * {@link gov.fnal.ppd.dd.GlobalVariables}, in the DB and in the PropertiesFile.
 * </p>
 * <p>
 * <b>TODO - Refactor this complicated class.</b> There are several aspects of the channel selector that are mixed together in this
 * class:
 * <ul>
 * <li>Building the GUI (Commented with STARTUP)</li>
 * <li>maintaining the GUI (e.g., responding to user input; RUN_GUI),</li>
 * <li>dealing with the logic of the channel selector (LOGIC),</li>
 * <li>presenting the user with various bits of "eye candy" (CANDY).</li>
 * </ul>
 * A reasonable goal would be to split this into more testable pieces (e.g., using the model-view-controller pattern). More simply,
 * It seems like we should be able to factor out the building of the GUI (at least).
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2013-21
 */
public class ChannelSelector extends JPanel implements ActionListener, DisplayCardActivator, DisplayKeeper, JavaChangeListener {

	private static final long				serialVersionUID			= 5044030472140151291L;

	// ---------- RUN_GUI
	/**
	 * The dimensions of the screen as fetched from java.awt.Toolkit
	 */
	public static final Dimension			screenDimension				= Toolkit.getDefaultToolkit().getScreenSize();
	private static final long				startTime					= System.currentTimeMillis();
	private static final SimpleDateFormat	sdf							= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final String				OFFLINE						= "**Off Line**";
	private static final long				DB_STATUS_UPDATE_PERIOD		= 7000L;

	// ---------- STARTUP
	/**
	 * Does this instance (of the Channel selector) need to show the docent tab?
	 */
	public static boolean					SHOW_DOCENT_TAB				= false;

	// ---------- STARTUP and RUN_GUI
	private List<List<ChannelButtonGrid>>	channelButtonGridList		= new ArrayList<List<ChannelButtonGrid>>();

	/*
	 * Each tab contains a specific type of Channels in a CardLayout There is one Card for each Display in the CardLayout A button
	 * might not be selected in a Card if that channel is not shown on the panel
	 * 
	 * ---------- STARTUP and RUN_GUI
	 */
	private Box								titleBox;
	private JLabel							title;
	private JButton							lockButton, versionIdentifier;
	private ChannelSelectorFileMenu			fmBar;
	private DisplayButtons					displaySelector;
	private CardLayout						card						= new CardLayout();
	private JPanel							displayChannelPanel			= new JPanel(card);
	private List<JTabbedPane>				listOfDisplayTabbedPanes	= new ArrayList<JTabbedPane>();
	private ProgressMonitor					progressMonitor;

	// ---------- CANDY
	private String							lastActiveDisplay			= null;
	private SplashScreens					splashScreens;
	private long							lastChannelChange			= System.currentTimeMillis();

	// ---------- RUN_GUI
	private DDButton.ButtonFieldToUse		nowShowing					= ButtonFieldToUse.USE_NAME_FIELD;
	private int								myDB_ID;

	// ---------- LOGIC
	// Data for the status update
	private VersionInformation				applicationVersionInfo		= VersionInformation.getVersionInformation();
	private String							presentStatus;

	private boolean							_firstAdjustTitle			= true;

	// ---------- beginning of methods that are largely STARTUP ----------------------------------------------------------------
	/**
	 * Create the channel selector GUI in the normal way
	 */
	public ChannelSelector(ProgressMonitor progressMonitor) {
		super(new BorderLayout());
		this.progressMonitor = progressMonitor;

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

	// ---------- STARTUP
	/**
	 * In order to obey the rule-of-thumb to not start threads within the context of a constructor, this start method is provided.
	 */
	public void start() {
		initComponents();
		launchMemoryWatcher();
		launchStatusThread();
		JavaVersion.getInstance().addJavaChangeListener(this);
	}

	// ---------- STARTUP
	private void initComponents() {
		presentStatus = "Initializing the GUI";
		SaveRestoreDefaultChannels.setup("Change Default Configurations", 30.0f, new Insets(20, 50, 20, 50));

		removeAll();

		displaySelector = new DisplayButtons(this);
		lastActiveDisplay = getDisplayID(displaySelector.getFirstDisplay());

		initializeTabs();

		progressMonitor.setNote("Finished building display GUI(s)");
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
		adjustTitle(displaySelector.getFirstDisplay());

		splashScreens = new SplashScreens(this, displayChannelPanel);

		if (!SHOW_IN_WINDOW)
			// Only enable the splash screen for the full-screen version
			splashScreens.start();
		presentStatus = "GUI constructed";
	}

	// ---------- STARTUP
	private static JLabelFooter makeChannelIndicator(Display display) {
		JLabelFooter footer = new JLabelFooter("Display " + display.getVirtualDisplayNumber());
		DisplayButtons.setToolTip(display);

		if (!SHOW_IN_WINDOW) {
			footer.setFont(new Font("Arial", Font.PLAIN, 18));
		}
		footer.setAlignmentX(CENTER_ALIGNMENT);
		return footer;
	}

	// ---------- STARTUP
	private void initializeTabs() {
		int progress = 1;
		ChannelClassification[] categories = ChannelClassificationDictionary.getCategories();
		int index = 0;
		for (final Display display : displayList) {
			progressMonitor.setNote("Building display " + (index + 1) + " of " + displayList.size());
			progressMonitor.setProgress(10 * progress);

			// NOTE: Each display has the same set of tabs and channels to offer.

			final JTabbedPane displayTabPane = new JTabbedPane();
			listOfDisplayTabbedPanes.add(displayTabPane);
			float sz = 12.0f;
			if (!SHOW_IN_WINDOW)
				sz = categories.length > 7 ? 30.0f - 2 * (categories.length - 7) : 30.0f;

			displayTabPane.setFont(getFont().deriveFont(sz));
			displayTabPane.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					// This was not used anywhere
					// selectedTab = ((JTabbedPane) e.getSource()).getSelectedIndex();
				}
			});

			final List<ChannelButtonGrid> allGrids = new ArrayList<ChannelButtonGrid>();
			synchronized (channelButtonGridList) {
				channelButtonGridList.add(allGrids);
			}

			DisplayButtonGroup displayButtonGroup = new DisplayButtonGroup();

			// Make a tab for each of the channel categories (Public, Misc, Videos, Weather, CMS, NoVA, etc.)
			Component theSelectedTab = null;
			for (int cat = 0; cat < categories.length; cat++) {
				progressMonitor.setNote("Building display " + (index + 1) + " - " + categories[cat]);

				ChannelButtonGrid grid = new DetailedInformationGrid(display, displayButtonGroup);
				grid.makeGrid(categories[cat]);
				allGrids.add(grid);
				display.addListener(grid);
				display.addListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread() {
							public void run() {
								/*
								 * When the display button is pressed, wait a bit and then change the status that is shown at the
								 * bottom of the screen
								 */
								catchSleep(3000);
								presentStatus = getChannelChangeStatus(display);
							}
						}.run();
					}
				});
				String sp = SHOW_IN_WINDOW ? "" : " ";
				displayTabPane.add(grid, sp + categories[cat].getAbbreviation() + sp);
			}

			progressMonitor.setNote("Building display " + (index + 1) + " of " + displayList.size() + " - Image tab");
			if (index == 0) {
				final int pp = progress;
				new Thread("progMonKludge") {
					public void run() {
						// A little eye candy to have the progress bar be more realistic
						for (int p = 1; p < 10; p++) {
							catchSleep(1900);
							progressMonitor.setProgress(10 * pp + p);
						}
					}
				}.start();
			}
			ChannelButtonGrid grid = new ImageGrid(display, displayButtonGroup);
			grid.makeGrid(ChannelClassification.IMAGE);
			allGrids.add(grid);
			display.addListener(grid);
			displayTabPane.add(grid, " Images ");

			if (SHOW_DOCENT_TAB) {
				GetMessagingServer.getDocentNameSelector();
				ChannelButtonGrid gridDocent = new DocentGrid(display, displayButtonGroup, docentName);
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

			SimplifiedChannelGrid scg = new SimplifiedChannelGrid(display, displayButtonGroup);
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

				ExistingChannelLists ecl = new ExistingChannelLists(display, displayButtonGroup);
				ecl.setBackground(display.getPreferredHighlightColor());
				displayTabPane.add(ecl, " Existing Lists ");

				clg.setNewListCreationCallback(ecl);
			}

			if (theSelectedTab != null)
				displayTabPane.setSelectedComponent(theSelectedTab);

			// Add the Display TabbedPane to the main screen
			inner.setBorder(
					BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(display.getPreferredHighlightColor(), wid1),
							BorderFactory.createEmptyBorder(wid2, wid2, wid2, wid2)));
			inner.add(displayTabPane, BorderLayout.CENTER);
			final JLabelFooter footer = makeChannelIndicator(display);
			display.addListener(new ActionListener() {
				private long lastUpdated = System.currentTimeMillis();

				// A simple listener to change the title after the Display has a chance to react.
				public void actionPerformed(ActionEvent e) {
					DisplayChangeEvent ev = (DisplayChangeEvent) e;

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

			final int fi = index;
			displayButtonGroup.setActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					setTabColor(((DDButton) e.getSource()).getDisplay(), fi);
				}
			});
			setTabColor(display, index);

			// Set up a database check every now and then to see what a Display is actually doing (this is inSTEAD of the Ping
			// thread, above).
			new CheckDisplayStatus(display, index, footer).start();
			index++;
			progress++;
		}

		new WhoIsInChatRoom(this).start();
	}

	// ---------- STARTUP
	protected static Border getTitleBorder(Color c) {
		if (SHOW_IN_WINDOW)
			return BorderFactory.createEmptyBorder(1, 5, 1, 5);
		return BorderFactory.createEmptyBorder(2, 30, 2, 30);
	}

	// ---------- STARTUP

	private JComponent makeTitle() {
		title = new JLabel(" Dynamic Display 00, V0.0.0 ");
		title.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		title.setOpaque(false);
		// title.setBackground(Color.white);
		titleBox = Box.createHorizontalBox();

		final int goodSpacing = 5;

		fmBar = new ChannelSelectorFileMenu(this, new ChangeDefaultsActionListener(this));
		fmBar.setBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED),
						BorderFactory.createCompoundBorder(
								BorderFactory.createEmptyBorder(goodSpacing, goodSpacing, goodSpacing, goodSpacing),
								fmBar.getBorder())));

		titleBox.setOpaque(true);

		versionIdentifier = new JButton("V" + getSoftwareVersion());
		versionIdentifier.setFont(new Font("Arial", Font.PLAIN, 8));
		versionIdentifier.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		versionIdentifier.setToolTipText("Running Dynamic Displays Java Suite, Version " + getSoftwareVersion());
		versionIdentifier.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ChannelSelector.this.actionPerformed(new ActionEvent(this, 0, CHECK_NEW_VERSION_MENU));
				// JOptionPane.showMessageDialog(ChannelSelector.this,
				// "This is running Dynamic Displays Java suite version " + getSoftwareVersion());
			}
		});

		fmBar.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		title.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		versionIdentifier.setAlignmentY(JComponent.CENTER_ALIGNMENT);

		titleBox.add(fmBar);
		titleBox.add(Box.createHorizontalGlue());
		titleBox.add(title);
		titleBox.add(Box.createHorizontalGlue());
		titleBox.add(versionIdentifier);
		titleBox.add(Box.createRigidArea(new Dimension(goodSpacing, goodSpacing)));

		// Do not let the simple public controller exit
		if (!SHOW_IN_WINDOW) {
			if (lockButton == null) {
				// Observation on 9/15/2014 -- A user of the touch screen wanted to "go back" to the pretty pictures. This lock
				// button accomplishes this task
				ImageIcon icon;
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

		return titleBox;
	}

	// ---------- STARTUP
	public void setSelectorID(int m) {
		myDB_ID = m;
	}

	// ---------- STARTUP
	private void launchStatusThread() {
		Timer timer = new Timer("StatusUpdate");
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				updateControllerStatus();
			}
		}, DB_STATUS_UPDATE_PERIOD / 4L, DB_STATUS_UPDATE_PERIOD);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				presentStatus = OFFLINE;
				updateControllerStatus();
			}
		});

	}
	// ---------- beginning of methods that are both STARTUP and RUN_GUI ---------------------------

	// ---------- STARTUP and RUN_GUI
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

	// ---------- Beginning of the methods that are business LOGIC------------------------------------------

	// ---------- LOGIC
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

				final int MAX_MESSAGE_SIZE = 2048;
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

			query += ", Version='" + applicationVersionInfo.getVersionString() + "', Uptime=" + uptime + " WHERE ControllerID="
					+ myDB_ID;
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

	// ---------- beginning of methods that are largely RUN_GUI -----------------------------------------------------

	// ---------- RUN_GUI
	private void adjustTitle(Display display) {
		if (display == null)
			return;

		Color c = display.getPreferredHighlightColor();
		// These constants taken from
		// https://stackoverflow.com/questions/596216/formula-to-determine-perceived-brightness-of-rgb-color
		int lightness = (int) Math
				.sqrt(0.299 * c.getRed() * c.getRed() + 0.587 * c.getGreen() * c.getGreen() + 0.114 * c.getBlue() * c.getBlue());
		System.out.println("For color " + c + ", lightness is " + lightness);

		String num = "" + display.getVirtualDisplayNumber();
		String t = display.getLocation();
		synchronized (title) {
			if (t.contains(num))
				title.setText("   " + shortDate().substring(0, 9) + " -  " + t + "   ");
			else
				title.setText("   " + shortDate().substring(0, 9) + " -  " + t + " (" + num + ")  ");
		}
		if (_firstAdjustTitle) {
			_firstAdjustTitle = false;
			new Thread("TitleAdjust") {
				public void run() {
					int offset = ("   " + shortDate().substring(0, 9) + " -- ").length();
					while (true) {
						catchSleep(500L);
						String dash = ((System.currentTimeMillis()/1000)%2) == 1 ? " -  " : "  - ";
						synchronized (title) {
							title.setText("   " + shortDate().substring(0, 9) + dash + title.getText().substring(offset));
						}
					}
				}
			}.start();
		}

		versionIdentifier.setBackground(c);
		if (lightness < 128) {
			versionIdentifier.setForeground(Color.white);
			title.setForeground(Color.white);
		} else {
			versionIdentifier.setForeground(Color.black);
			title.setForeground(Color.black);
		}

		titleBox.setOpaque(true);
		titleBox.setBackground(c);
	}

	// ---------- RUN_GUI
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

	// ---------- RUN_GUI
	@Override
	public void setDisplayIsAlive(int number, boolean alive) {
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

	// ---------- RUN_GUI
	/**
	 * Service the pull-down menu
	 * 
	 * @param e
	 */
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
				// The user has asked to restart the GUI!
				System.exit(-1);
			// Otherwise, do nothing.
			break;

		case INFO_BUTTON_MENU:
			nowShowing = nowShowing.next();
			userHasDoneSomething();

			synchronized (channelButtonGridList) {
				for (List<ChannelButtonGrid> gridList : channelButtonGridList)
					for (ChannelButtonGrid grid : gridList) {
						DisplayButtonGroup dbg = grid.getBg();
						for (int i = 0; i < dbg.getNumButtons(); i++) {
							DDButton ddb = dbg.getAButton(i);
							ddb.setText(nowShowing);
						}
					}
			}

			break;

		case HELP_MENU:
			String text = SelectorInstructions.getInstructions().replace("</html>",
					"<br><br>For more information, see <u>" + getFullURLPrefix() + "/about.php</u></html>");
			JOptionPane.showMessageDialog(ChannelSelector.this, text, "The Dynamic Displays Channel Selector",
					JOptionPane.PLAIN_MESSAGE);
			break;

		case CHECK_NEW_VERSION_MENU:
			FLAVOR flavor = getFlavor(true);
			VersionInformation me = VersionInformation.getVersionInformation();
			if (VersionInformationComparison.lookup(flavor, true)) {
				VersionInformation them = VersionInformation.getDBVersionInformation(flavor);
				Object[] o2 = { "YES: Download new software and restart (recommended)", "No, not now" };
				if (JOptionPane.showOptionDialog(this, shortDate() + ": Download and restart now?",
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
						"No need to update, " + shortDate(), JOptionPane.PLAIN_MESSAGE);
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

	// ---------- RUN_GUI
	@Override
	public void activateCard(boolean showingSplash) {
		if (showingSplash) {
			card.show(displayChannelPanel, lastActiveDisplay);
		} else {
			card.show(displayChannelPanel, splashScreens.getNext());
		}
	}

	// ---------- RUN_GUI
	@Override
	public void javaHasChanged() {
		System.out.println("The Java version has changed!");
		println(getClass(), "The Java version has changed!");
		showJavaChangePopup();
	}

	// ---------- methods that are mostly CANDY -----------------------------------------------------------------

	// ---------- CANDY
	private int delayTime = 30;

	private void showJavaChangePopup() {
		if (delayTime < 2)
			delayTime = 2;
		println(getClass(), "showJavaChangePopup() called with delayTime=" + delayTime);
		String mess = "<html>" + "<b>The version of Java has changed from '" + JavaVersion.getInstance().get() + "' to '"
				+ JavaVersion.getInstance().getCurrentVersion() + "'</b><br>" + "<br>The ChannelSelector must be restarted<br>"
				+ "<br>This is crucial because when the Java version changes, this app breaks in weird ways."
				+ "<br>When you hit 'Yes' here, the Channel Selector will exit and start again."
				+ "<br><br>Do you want to restart now?<br></html>";
		Object[] options = { "Yes: Restart the ChannelSelector app now",
				"I acknowledge that this app may break; delay by " + delayTime + " min anyway" };
		int n = JOptionPane.showOptionDialog(ChannelSelector.this, mess, "JVM Updated - Restart the ChannelSelector?",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		if (n == 0)
			System.exit(-1);
		else {
			new Thread("Wait30") {
				public void run() {
					catchSleep(delayTime * ONE_MINUTE);
					delayTime /= 2;
					showJavaChangePopup();
				}
			}.start();
		}

	}

	// ---------- CANDY
	private String getChannelChangeStatus(final Display display) {
		lastChannelChange = System.currentTimeMillis();
		return "Last channel change sent to display " + display.getVirtualDisplayNumber() + "/" + display.getDBDisplayNumber()
				+ " [" + display.getContent() + "] at " + shortDate();
	}

	// This code snippet would be used if this selector was to be run on an Android device.
	// @Override
	// public void onConfigurationChanged(Configuration newConfig) {
	// super.onConfigurationChanged(newConfig);
	// //Do stuff here
	// }

	/**
	 * <p>
	 * These two methods are not used. Go to the GIT history to find the code
	 * <ul>
	 * <li>public void createRefreshActions()</li>
	 * <li>protected void destroy()</li>
	 * </ul>
	 * </p>
	 */
}
