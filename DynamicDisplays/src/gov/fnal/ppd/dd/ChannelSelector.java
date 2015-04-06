package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.INSET_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.PRIVATE_KEY_LOCATION;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.THIS_IP_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.THIS_IP_NAME_INSTANCE;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_FOLDER;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.lastDisplayChange;
import static gov.fnal.ppd.dd.GlobalVariables.locationCode;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.launchMemoryWatcher;
import gov.fnal.ppd.dd.changer.AddYourOwnURL;
import gov.fnal.ppd.dd.changer.CategoryDictionary;
import gov.fnal.ppd.dd.changer.ChannelButtonGrid;
import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.DDButton;
import gov.fnal.ppd.dd.changer.DetailedInformationGrid;
import gov.fnal.ppd.dd.changer.DisplayButtons;
import gov.fnal.ppd.dd.changer.DisplayChangeEvent;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.changer.DocentGrid;
import gov.fnal.ppd.dd.changer.ImageGrid;
import gov.fnal.ppd.dd.changer.InformationBox;
import gov.fnal.ppd.dd.channel.CreateListOfChannelsHelper;
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.display.DisplayFacade;
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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
	 * 
	 */
	public static final Dimension			screenDimension				= Toolkit.getDefaultToolkit().getScreenSize();
	private static JFrame					f							= new JFrame("Dynamic Display Channel Selector");

	// private static ActionListener fullRefreshAction = null;
	private static ActionListener			channelRefreshAction		= null;
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
	private JButton							addChannelButton			= new JButton("+");
	// private JCheckBox showBorderButton = new JCheckBox("Border?");
	private DisplayButtons					displaySelector;
	private CardLayout						card						= new CardLayout();
	private JPanel							displayChannelPanel			= new JPanel(card);

	private String							lastActiveDisplay			= null;

	private List<JTabbedPane>				listOfDisplayTabbedPanes	= new ArrayList<JTabbedPane>();

	private SplashScreens					splashScreens;
	private int								nowShowing					= DDButton.USE_NAME_FIELD;

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
			channelButtonGridList.add(allGrids);

			for (int cat = 0; cat < categories.length; cat++) {
				ChannelButtonGrid grid = new DetailedInformationGrid(display, bg);
				grid.makeGrid(categories[cat]);
				allGrids.add(grid);
				display.addListener(grid);
				String sp = SHOW_IN_WINDOW ? "" : " ";
				displayTabPane.add(grid, sp + categories[cat].getAbbreviation() + sp);
				progressMonitor.setNote(note);
				progressMonitor.setProgress(index * (1 + categories.length) + cat);
			}

			ChannelButtonGrid grid = new ImageGrid(display, bg);
			grid.makeGrid(ChannelCategory.IMAGE);
			allGrids.add(grid);
			display.addListener(grid);
			displayTabPane.add(grid, " Images ");

			grid = new DocentGrid(display, bg, "Default");
			grid.makeGrid(ChannelCategory.IMAGE);
			allGrids.add(grid);
			display.addListener(grid);
			displayTabPane.add(grid, " Docent ");

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

			final CreateListOfChannelsHelper channelLister = new CreateListOfChannelsHelper();
			displayTabPane.add(channelLister.listerPanel, " Lists ");
			channelLister.accept.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					System.out.println("Channel list accepted");
					display.setContent(channelLister.lister.getChannelList());
				}
			});

			if (SHOW_IN_WINDOW && false) {
				// Do not include the "wrap your own" URL until someone asks for it.
				AddYourOwnURL yourOwn = new AddYourOwnURL(display, bg);
				allGrids.add(yourOwn);
				displayTabPane.add(yourOwn, "New URL");
			}

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
					System.out.println(ChannelSelector.class.getSimpleName() + ".actionPerformed(), event=" + e);
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
						text = "Display " + display.getVirtualDisplayNumber() + " ERROR; " + ": " + display.getContent().getCategory() + "/'"
								+ (display.getContent().getName());
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
						text = "G Display " + display.getVirtualDisplayNumber() + " Idle; " + ": " + display.getContent().getCategory() + "/'"
								+ (display.getContent().getName());
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

			displayChannelPanel.add(inner, display.toString());

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
			refreshButton.setFont(new Font("SansSerif", Font.BOLD, (int) (FONT_SIZE / 2)));
			// showBorderButton.setFont(new Font("SansSerif", Font.BOLD, (int) (FONT_SIZE / 2)));
		}
		// showBorderButton.setOpaque(false);
		// title.setFont(title.getFont().deriveFont(3 * FONT_SIZE / 4));
		title.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		title.setOpaque(true);
		title.setBackground(Color.white);
		titleBox = Box.createHorizontalBox();
		adjustTitle(displayList.get(0));

		refreshButton.setMargin(new Insets(5, 5, 5, 5));
		// Add this in when I get it to work!! refreshButton.addActionListener(fullRefreshAction);
		refreshButton.addActionListener(channelRefreshAction);
		refreshButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new InformationBox((SHOW_IN_WINDOW ? 0.7f : 1.0f), refreshButton, "Channels refreshed",
						"The URLs of all the channels have been refreshed");
			}
		});

		if (SHOW_IN_WINDOW) {
			titleBox.add(refreshButton);
			// Create a button to add a channel to the database
			addChannelButton.addActionListener(new ActionListener() {
				boolean	showDialog	= true;

				@Override
				public void actionPerformed(ActionEvent arg0) {
					int n = 0;
					if (showDialog) {
						Box dialog = Box.createVerticalBox();
						dialog.add(new JLabel(
								"<html><center>You are about to launch a browser/web page to add a Channel to database.<br>"
										+ "You must restart the Channel Selector to see the new Channel once it is added.<br><hr>"
										+ "<em>This operation should be used sparingly</em></br><br>"
										+ "Continue with the addition?</center></html>"));
						JCheckBox cb = new JCheckBox("Do not show this again");
						dialog.add(cb);
						n = JOptionPane.showConfirmDialog(addChannelButton, dialog, "Add Channel?", JOptionPane.YES_NO_OPTION);
						showDialog = !cb.isSelected();
					}
					if (n == 0)
						try {
							Desktop.getDesktop().browse(
									new URI("http://" + WEB_SERVER_NAME + "/" + WEB_SERVER_FOLDER + "/channelAdd.php"));
						} catch (IOException e) {
							e.printStackTrace();
						} catch (URISyntaxException e) {
							e.printStackTrace();
						}
				}

			});
			addChannelButton.setToolTipText("<html><b>Add a channel to the system</b> -- "
					+ "<em>use sparingly!</em><br>Launches a web page on your local browser that "
					+ "<br>will allow you to add a Channel/URL to the system</html>");
			addChannelButton.setMargin(new Insets(5, 5, 5, 5));
		} else {
			// Create an exit button
			exitButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					JLabel message = new JLabel(
							"<html>Do you <em>really</em> want to exit the<br>Channel Selector Application?</html>");
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
		}

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

		if (!IS_PUBLIC_CONTROLLER) {
			refreshButton.setToolTipText("<html><b>Refresh URLs</b> -- Refresh the URLs on each Channel button "
					+ "<br>by re-reading this information from the Channel database."
					+ "<br><em>This will not create new buttons</em></html>");
			titleBox.add(refreshButton);
			if (!SHOW_IN_WINDOW)
				IdentifyAll.setup("ID", FONT_SIZE / 2, new Insets(5, 5, 5, 5));
			titleBox.add(Box.createRigidArea(new Dimension(5, 5)));
			// titleBox.add(showBorderButton);

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

		JButton flipTitles = new JButton(new ImageIcon("bin/gov/fnal/ppd/dd/images/2arrows.png"));
		flipTitles.setMargin(new Insets(2, 5, 2, 5));
		flipTitles.setToolTipText("<html><b>Toggle Text</b> -- Toggle between a succinct"
				+ "<br>Channel button label and one with more details");

		flipTitles.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				nowShowing = (nowShowing == DDButton.USE_NAME_FIELD ? DDButton.USE_DESCRIPTION_FIELD : DDButton.USE_NAME_FIELD);

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

			if (!IS_PUBLIC_CONTROLLER) {
				titleBox.add(Box.createRigidArea(new Dimension(5, 5)));
				titleBox.add(exitButton);
			}
		} else {
			titleBox.add(addChannelButton);
		}

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
	 * Run the Channel Selector application
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {

		final SignageType sType = (IS_PUBLIC_CONTROLLER ? SignageType.Public : SignageType.XOC);

		MessageCarrier.initializeSignature();
		System.out.println("Initialized our digital signature from '" + PRIVATE_KEY_LOCATION + "'.");
		System.out.println("\t Expect my client name to be '" + THIS_IP_NAME + " selector " + THIS_IP_NAME_INSTANCE + "'\n");

		displayList = DisplayListFactory.getInstance(sType, getLocationCode());
		channelSelector = new ChannelSelector();
		channelSelector.start();

		// SHOW_IN_WINDOW = args.length > 0 && "WINDOW".equals(args[0]);

		createRefreshActions(sType);

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
	 * @return the location code for this instance of the channel selector
	 */
	public static int getLocationCode() {
		return locationCode;
	}

	private static void createRefreshActions(final SignageType sType) {
		@SuppressWarnings("unused")
		ActionListener fullRefreshAction = new ActionListener() {

			// FIXME This operation does not work! I suspect this is because of the globals and the socket connections (maybe they
			// don't get closed properly?)
			// Another attempt on 8/25/14, and I get a "Socket Closed" exception. (?)

			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						DisplayFacade.tryToConnectToDisplaysNow = true;
						System.out.println("\n\nRefreshing Channel Selector\n\n");
						f.setVisible(false);
						f.dispose();
						f = null;

						channelSelector.destroy();
						for (Display D : displayList) {
							D.disconnect();
						}
						displayList.clear();

						channelSelector = null;
						displayList = null;
						Runtime.getRuntime().gc();
						catchSleep(1000);

						// Regenerate the Display list and the Channel list
						// DisplayListFactory.useRealDisplays(realDisplays);
						// ChannelCatalogFactory.useRealChannels(true);
						displayList = DisplayListFactory.getInstance(sType, getLocationCode());

						channelSelector = new ChannelSelector();
						channelSelector.start();
						// channelSelector.setRefreshAction(fullRefreshAction);

						f = new JFrame("XOC Display Channel Selector");
						f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

						f.setUndecorated(!SHOW_IN_WINDOW);

						// DisplayListFactory.useRealDisplays(realDisplays);
						// ChannelCatalogFactory.useRealChannels(true);

						f.setContentPane(channelSelector);
						if (SHOW_IN_WINDOW)
							f.pack();
						else
							f.setSize(screenDimension);
						f.setVisible(true);

					}
				});
			}
		};

		channelRefreshAction = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						System.out.println("Refreshing URLs of the channel buttons in this world");
						// An alternate idea on how to implement the refresh
						/*
						 * Rather than doing a full refresh, maybe we can just refresh the URLs of the channels that we already
						 * have. This does not fix the problem of adding channels to the system, but it helps somewhat.
						 */

						Set<SignageContent> list = ChannelCatalogFactory.refresh()
								.getChannelCatalog(ChannelCategory.PUBLIC_DETAILS);
						Set<SignageContent> subList = ChannelCatalogFactory.getInstance().getChannelCatalog(ChannelCategory.PUBLIC);
						list.addAll(subList);
						subList = ChannelCatalogFactory.getInstance().getChannelCatalog(ChannelCategory.EXPERIMENT_DETAILS);
						list.addAll(subList);
						subList = ChannelCatalogFactory.getInstance().getChannelCatalog(ChannelCategory.NOVA_DETAILS);
						list.addAll(subList);
						subList = ChannelCatalogFactory.getInstance().getChannelCatalog(ChannelCategory.NUMI_DETAILS);
						list.addAll(subList);
						subList = ChannelCatalogFactory.getInstance().getChannelCatalog(ChannelCategory.MISCELLANEOUS);
						list.addAll(subList);

						// list contains all the channels from the database Now check the URLs in the DB versus the ones we have now
						// that are attached to all the buttons in our realm
						int numButtons = 0;
						int numChanged = 0;
						for (List<ChannelButtonGrid> gridList : channelSelector.channelButtonGridList) {
							for (ChannelButtonGrid grid : gridList) {
								DisplayButtonGroup dbg = grid.getBg();
								for (int i = 0; i < dbg.getNumButtons(); i++) {
									numButtons++;
									DDButton ddb = dbg.getAButton(i);
									int channelNumber = ddb.getChannel().getNumber();
									// Loop over the new channels to see if the URL has changed for the exiting channel
									for (SignageContent CONTENT : list)
										if (CONTENT instanceof Channel) {
											Channel chan = (Channel) CONTENT;
											if (chan.getNumber() == channelNumber) {
												if (!ddb.getChannel().getURI().toString().equals(chan.getURI().toString())) {
													System.out.println("URI '" + ddb.getChannel().getURI() + "' changed to '"
															+ chan.getURI() + "'");
													numChanged++;
												}
												ddb.getChannel().setURI(chan.getURI());
											}
											// if (channelNumber == 75 || chan.getNumber() == 75)
											// System.out.println("Existing button for chan #" + channelNumber + " says URL="
											// + ddb.getChannel().getURI().toString() + "\n\t\tNew information for channel #"
											// + chan.getNumber() + " is " + chan.getURI());
										} else
											System.out.println("Hmmm.  Got a " + CONTENT.getClass().getCanonicalName()
													+ " instead of a Channel");
								}
							}
						}
						System.out.println("Looked at " + numButtons + " buttons and changed " + numChanged + " URLs");
					}
				});
			}
		};
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
