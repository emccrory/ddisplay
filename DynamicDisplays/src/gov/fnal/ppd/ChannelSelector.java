package gov.fnal.ppd;

import static gov.fnal.ppd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.GlobalVariables.INSET_SIZE;
import static gov.fnal.ppd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.GlobalVariables.WEB_SERVER_NAME;
import static gov.fnal.ppd.GlobalVariables.displayList;
import static gov.fnal.ppd.GlobalVariables.lastDisplayChange;
import static gov.fnal.ppd.GlobalVariables.locationCode;
import static gov.fnal.ppd.signage.util.Util.catchSleep;
import static gov.fnal.ppd.signage.util.Util.launchMemoryWatcher;
import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.AddYourOwnURL;
import gov.fnal.ppd.signage.changer.ChannelButtonGrid;
import gov.fnal.ppd.signage.changer.ChannelCatalogFactory;
import gov.fnal.ppd.signage.changer.ChannelCategory;
import gov.fnal.ppd.signage.changer.DDButton;
import gov.fnal.ppd.signage.changer.DetailedInformationGrid;
import gov.fnal.ppd.signage.changer.DisplayButtons;
import gov.fnal.ppd.signage.changer.DisplayChangeEvent;
import gov.fnal.ppd.signage.changer.DisplayListFactory;
import gov.fnal.ppd.signage.changer.ImageGrid;
import gov.fnal.ppd.signage.changer.InformationBox;
import gov.fnal.ppd.signage.channel.CreateListOfChannelsHelper;
import gov.fnal.ppd.signage.display.DisplayFacade;
import gov.fnal.ppd.signage.util.CheckDisplayStatus;
import gov.fnal.ppd.signage.util.DisplayButtonGroup;
import gov.fnal.ppd.signage.util.DisplayCardActivator;
import gov.fnal.ppd.signage.util.DisplayKeeper;
import gov.fnal.ppd.signage.util.JLabelFooter;
import gov.fnal.ppd.signage.util.SplashScreens;
import gov.fnal.ppd.signage.util.WhoIsInChatRoom;

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
 * Allows the user to select the Channel that is being displayed on every Signage Display
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2013
 */
public class ChannelSelector extends JPanel implements ActionListener, DisplayCardActivator, DisplayKeeper {

	private static final long				serialVersionUID			= 5044030472140151291L;

	private static final Dimension			screenDimension				= Toolkit.getDefaultToolkit().getScreenSize();
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

	/**
	 * Create the channel selector in the normal way
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

		int[] categories = { 0, 1, 3, 4, 2, 6, 5, -1 };
		String[] labels = { "Public", "Details", "NOvA", "NuMI", "EXP", "BEAM", "Videos", "MISC" };
		if (!IS_PUBLIC_CONTROLLER) {
			categories = new int[] { 0, 1, 5, -1 };
			labels = new String[] { "Public", "Details", "Videos", "MISC" };
		}

		ProgressMonitor progressMonitor = new ProgressMonitor(null, "Building Channel Selector GUI", "", 0, displayList.size()
				* (1 + categories.length));
		String note = "Building image database";
		progressMonitor.setNote(note);
		progressMonitor.setProgress(0);

		if (categories.length != labels.length)
			throw new RuntimeException();

		int index = 0;
		for (final Display display : displayList) {
			progressMonitor.setNote(note);
			progressMonitor.setProgress(index * 5);

			// NOTE: Each display has the same set of tabs and channels to offer.

			final JTabbedPane displayTabPane = new JTabbedPane();
			listOfDisplayTabbedPanes.add(displayTabPane);
			displayTabPane.setFont(getFont().deriveFont((SHOW_IN_WINDOW ? 12.0f : 30.0f)));
			displayTabPane.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					selectedTab = ((JTabbedPane) e.getSource()).getSelectedIndex();
				}
			});
			DisplayButtonGroup bg = new DisplayButtonGroup();

			final List<ChannelButtonGrid> allGrids = new ArrayList<ChannelButtonGrid>();
			channelButtonGridList.add(allGrids);

			for (int cat = 0; cat < categories.length; cat++) {
				ChannelButtonGrid grid = new DetailedInformationGrid(display, bg, categories[cat]);
				allGrids.add(grid);
				display.addListener(grid);
				String sp = SHOW_IN_WINDOW ? "" : " ";
				displayTabPane.add(grid, sp + labels[cat] + sp);
				progressMonitor.setNote(note);
				progressMonitor.setProgress(index * 5 + cat);
			}

			ChannelButtonGrid grid = new ImageGrid(display, bg);
			allGrids.add(grid);
			display.addListener(grid);
			displayTabPane.add(grid, " Images ");

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

			if (SHOW_IN_WINDOW) {
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

					String text = display.getNumber() + " to " + display.getContent().getCategory() + " channel '"
							+ (display.getContent().getName());
					boolean alive = false;

					switch (ev.getType()) {
					case CHANGE_RECEIVED:
						text = "<html>Setting Display " + text;
						break;
					case CHANGE_COMPLETED:
						text = "<html>Changed Display " + text;
						alive = true;
						break;
					case ALIVE:
						text = "A Display " + display.getNumber() + ": " + display.getContent().getCategory() + "/'"
								+ (display.getContent().getName());
						alive = true;
						break;
					case ERROR:
						text = "Display " + display.getNumber() + " ERROR; " + ": " + display.getContent().getCategory() + "/'"
								+ (display.getContent().getName());
						break;
					case IDLE:
						if (lastUpdated + 30000l > System.currentTimeMillis())
							break;
						text = "G Display " + display.getNumber() + " Idle; " + ": " + display.getContent().getCategory() + "/'"
								+ (display.getContent().getName());
						alive = true;
						break;
					}
					lastUpdated = System.currentTimeMillis();

					footer.setText(text);
					displaySelector.resetToolTip(display);
					setDisplayIsAlive(display.getNumber(), alive);
				}
			});

			// TODO Have the Display button react to a change in the status of a Display,
			// in particular, when the Display goes off-line or comes online.

			footer.setAlignmentX(CENTER_ALIGNMENT);
			Box b = Box.createHorizontalBox();
			b.add(Box.createHorizontalGlue());
			b.add(footer);
			b.add(Box.createHorizontalGlue());
			inner.add(b, BorderLayout.SOUTH);

			displayChannelPanel.add(inner, display.toString());

			note = "Building the rest of the GUI";
			progressMonitor.setNote(note);
			progressMonitor.setProgress(index * 5 + categories.length);

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
		// System.out.println(ChannelSelector.class.getSimpleName() + ": Display " + number + (alive ? " is alive" :
		// " is NOT alive"));
		displaySelector.setIsAlive(number, alive);

		// Enable the Channel buttons, too
		for (List<ChannelButtonGrid> allGrids : channelButtonGridList)
			if (allGrids.get(0).getDisplay().getNumber() == number)
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
							Desktop.getDesktop().browse(new URI("http://" + WEB_SERVER_NAME + "/XOC/channelAdd.php"));
						} catch (IOException e) {
							e.printStackTrace();
						} catch (URISyntaxException e) {
							e.printStackTrace();
						}
				}

			});
			addChannelButton.setToolTipText("<html><b>Add a channel to the system</b> -- "
					+ "<em>use sparingly!</em><br>Launches a web page on your local browser that will "
					+ "allow you to add a Channel/URL to the system</html>");
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

		title.setText("   " + display.getLocation() + "   ");
		titleBox.removeAll();
		titleBox.setOpaque(true);
		titleBox.setBackground(c);
		int wid = (SHOW_IN_WINDOW ? 1 : 8);
		titleBox.setBorder(BorderFactory.createLineBorder(c, wid));

		if (!IS_PUBLIC_CONTROLLER) {
			titleBox.add(refreshButton);
			if (!SHOW_IN_WINDOW)
				IdentifyAll.setup("?", FONT_SIZE / 2, new Insets(5, 5, 5, 5));
			titleBox.add(Box.createRigidArea(new Dimension(5, 5)));
			// titleBox.add(showBorderButton);

			// TODO -- This IdentifyAll class was an expedient way to implement this functionality, but at the price of twice as
			// many messaging clients in this ChannelSelector class. It should be easy enough to re-implement this using the
			// messaging clients we already have.
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
				icon = new ImageIcon("src/gov/fnal/ppd/images/lock40.jpg");

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
		JLabelFooter footer = new JLabelFooter("Display " + display.getNumber());
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

	/**
	 * For future enhancements, say, when the selector is on a phone or an iPad.
	 * 
	 * @param args
	 */
	@SuppressWarnings("unused")
	private Orientation getScreenOrientation() {
		Orientation orientation = Orientation.UNDEFINED;
		if (screenDimension.width == screenDimension.height) {
			orientation = Orientation.SQUARE;
		} else if (screenDimension.width < screenDimension.height) {
			orientation = Orientation.PORTRAIT;
		} else {
			orientation = Orientation.LANDSCAPE;
		}

		return orientation;
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
