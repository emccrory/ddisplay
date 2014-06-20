package gov.fnal.ppd;

import static gov.fnal.ppd.signage.util.Util.shortDate;
import static gov.fnal.ppd.signage.util.Util.truncate;
import gov.fnal.ppd.chat.MessageCarrier;
import gov.fnal.ppd.chat.MessagingClient;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.ChannelButtonGrid;
import gov.fnal.ppd.signage.changer.ChannelCategory;
import gov.fnal.ppd.signage.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.signage.changer.DetailedInformationGrid;
import gov.fnal.ppd.signage.changer.DisplayButtons;
import gov.fnal.ppd.signage.changer.DisplayChangeEvent;
import gov.fnal.ppd.signage.changer.DisplayList;
import gov.fnal.ppd.signage.changer.DisplayListFactory;
import gov.fnal.ppd.signage.changer.MyButton;
import gov.fnal.ppd.signage.changer.PublicInformationGrid;
import gov.fnal.ppd.signage.channel.CreateListOfChannelsHelper;
import gov.fnal.ppd.signage.display.DisplayFacade;
import gov.fnal.ppd.signage.display.DisplayListDatabaseRemote;
import gov.fnal.ppd.signage.util.MyButtonGroup;
import gov.fnal.ppd.signage.util.Util;

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
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
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
import javax.swing.JTabbedPane;
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
public class ChannelSelector extends JPanel implements ActionListener {

	/**
	 * Encapsulate the orientation of the display (not really used yet)
	 * 
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * @copy 2014
	 * 
	 */
	public static enum Orientation {
		/**
		 * The screen orientation is not known
		 */
		UNDEFINED,
		/**
		 * The screen is perfectly square
		 */
		SQUARE,
		/**
		 * the screen is in a portrait orientation (taller)
		 */
		PORTRAIT,
		/**
		 * The screen is in a landscape orientation (wider)
		 */
		LANDSCAPE
	}

	private static final long				serialVersionUID		= 5044030472140151291L;
	/**
	 * 
	 */
	public static int						INSET_SIZE				= 32;
	/**
	 * 
	 */
	public static float						FONT_SIZE				= 68.0f;

	private static final int				MESSAGING_SERVER_PORT	= 1500;

	private static final Dimension			screenDimension			= Toolkit.getDefaultToolkit().getScreenSize();
	private static final JFrame				f						= new JFrame("XOC Display Channel Selector");

	private static final long				PING_INTERVAL			= 5000l;														// 60000l;
	protected static final long				FIFTEEN_MINUTES			= 15 * 60 * 1000;

	private static ActionListener			refreshAction			= null;
	private static DisplayList				displayList;
	// private static DisplayDebugTypes realDisplays = DisplayDebugTypes.REAL_AND_REMOTE; // DisplayDebugTypes.REAL_BUT_LOCAL;

	/**
	 * Do we show in full screen or in a window?
	 */
	public static boolean					SHOW_IN_WINDOW			= Boolean.getBoolean("signage.selector.inwindow");
	/**
	 * Is this a PUBLIC controller?
	 */
	public static boolean					IS_PUBLIC_CONTROLLER	= Boolean.getBoolean("signage.selector.public");
	private static ChannelSelector			channelSelector;

	private List<List<ChannelButtonGrid>>	channelButtonGridList	= new ArrayList<List<ChannelButtonGrid>>();
	/*
	 * Each tab contains a specific type of Channels in a CardLayout There is one Card for each Display in the CardLayout A button
	 * might not be selected in a Card if that channel is not shown on the panel
	 */

	private Box								titleBox;
	private JLabel							title;
	protected int							selectedTab;
	// The circular arrow, as in the recycling symbol
	private JButton							refreshButton			= new JButton("↺");
	private JButton							exitButton				= new JButton("X");
	private JButton							addChannelButton		= new JButton("+");
	private DisplayButtons					displaySelector;
	private CardLayout						card					= new CardLayout();
	private JPanel							displayChannelPanel		= new JPanel(card);

	/**
	 * Create the channel selector in the normal way
	 */
	public ChannelSelector() {
		super(new BorderLayout());

		// Determine the size of the screen and thus the appropriate font size.

		if (SHOW_IN_WINDOW) {
			FONT_SIZE = 15.0f;
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
		initComponents();
	}

	private void initComponents() {
		removeAll();
		SignageType[] cats = displayList.getCategories();
		if (IS_PUBLIC_CONTROLLER)
			cats = new SignageType[] { SignageType.Public };

		displaySelector = new DisplayButtons(cats, this);
		initChannelSelectors();

		add(displayChannelPanel, BorderLayout.CENTER);
		if (SHOW_IN_WINDOW)
			displayChannelPanel.setPreferredSize(new Dimension(400, 320));
		add(displaySelector, BorderLayout.EAST);
		add(makeTitle(), BorderLayout.NORTH);

		new Thread("MemoryWatcher.DEBUG") {
			public void run() {
				long time = FIFTEEN_MINUTES / 512;
				double sq2 = Math.sqrt(2.0);
				while (true) {
					try {
						sleep(time);
					} catch (InterruptedException e) {
					}
					if (time < FIFTEEN_MINUTES) {
						time *= sq2;
						if (time >= FIFTEEN_MINUTES) {
							time = FIFTEEN_MINUTES;
						}
					}
					int tCount = java.lang.Thread.activeCount();
					int activeTCount = ManagementFactory.getThreadMXBean().getThreadCount();
					long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;
					long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
					long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
					System.out.println("Threads: " + tCount + " (" + activeTCount + "), mem: " + total + "M " + free + "M " + max
							+ "M at " + (new Date()) + " (Sleep " + (time / 1000) + " sec.)");
				}
			}
		}.start();
	}

	private void initChannelSelectors() {
		// Three (or more) tabs representing Public, First set of Details, Full set of Details (or
		// something like that) Each Display has its own set of channels.

		int index = 0;
		for (final Display display : displayList) {
			JTabbedPane displayTabPane = new JTabbedPane();
			displayTabPane.setFont(getFont().deriveFont((SHOW_IN_WINDOW ? 10.0f : 28.0f)));
			displayTabPane.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					selectedTab = ((JTabbedPane) e.getSource()).getSelectedIndex();
				}
			});
			MyButtonGroup bg = new MyButtonGroup();

			final List<ChannelButtonGrid> allGrids = new ArrayList<ChannelButtonGrid>();
			channelButtonGridList.add(allGrids);

			ChannelButtonGrid grid = new PublicInformationGrid(display, bg);
			allGrids.add(grid);
			display.addListener(grid);
			String sp = SHOW_IN_WINDOW ? "" : " ";
			displayTabPane.add(grid, sp + ChannelCategory.PUBLIC + sp);

			grid = new DetailedInformationGrid(display, bg, 1);
			allGrids.add(grid);
			display.addListener(grid);
			displayTabPane.add(grid, sp + ChannelCategory.PUBLIC_DETAILS + sp);

			if (!IS_PUBLIC_CONTROLLER) {
				// Remove the MoreDetails tab until such time that it is needed
				grid = new DetailedInformationGrid(display, bg, 2);
				allGrids.add(grid);
				display.addListener(grid);
				displayTabPane.add(grid, sp + (SHOW_IN_WINDOW ? "EXP" : ChannelCategory.EXPERIMENT_DETAILS) + sp);
			}

			grid = new DetailedInformationGrid(display, bg, 3);
			allGrids.add(grid);
			display.addListener(grid);
			displayTabPane.add(grid, sp + (SHOW_IN_WINDOW ? "MISC" : ChannelCategory.MISCELLANEOUS) + sp);

			final JPanel inner = new JPanel(new BorderLayout());
			int wid1 = 6;
			int wid2 = 5;
			if (SHOW_IN_WINDOW) {
				wid1 = 2;
				wid2 = 1;
			}

			final CreateListOfChannelsHelper channelLister = new CreateListOfChannelsHelper();
			displayTabPane.add(channelLister.listerPanel, " Lists ");
			channelLister.accept.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					System.out.println("Channel list accepted");
					display.setContent(channelLister.lister.getChannelList());
				}
			});

			inner.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(display.getPreferredHighlightColor(), wid1),
					BorderFactory.createEmptyBorder(wid2, wid2, wid2, wid2)));
			inner.add(displayTabPane, BorderLayout.CENTER);
			final JLabel footer = makeChannelIndicator(display);
			display.addListener(new ActionListener() {
				private long	lastUpdated	= System.currentTimeMillis();

				// A simple listener to change the title after the Display has a chance to react.
				public void actionPerformed(ActionEvent e) {
					DisplayChangeEvent ev = (DisplayChangeEvent) e;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}

					String text = display.getNumber() + " to " + display.getContent().getCategory() + " channel '"
							+ truncate(display.getContent().getName()) + "' (" + shortDate() + ")</html>";
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
						text = "<html>A Display " + display.getNumber() + ": " + display.getContent().getCategory() + "/'"
								+ truncate(display.getContent().getName()) + "' (" + shortDate() + ")</html>";
						alive = true;
						break;
					case ERROR:
						text = "<html>Display " + display.getNumber() + " ERROR; " + ": " + display.getContent().getCategory()
								+ "/'" + truncate(display.getContent().getName()) + "' (" + shortDate() + ")</html>";
						break;
					case IDLE:
						if (lastUpdated + 30000l > System.currentTimeMillis())
							break;
						text = "<html>G Display " + display.getNumber() + " Idle; " + ": " + display.getContent().getCategory()
								+ "/'" + truncate(display.getContent().getName()) + "' (" + shortDate() + ")</html>";
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

			displayChannelPanel.add(display.toString(), inner);

			final int fi = index;
			bg.setActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					setTabColor(((MyButton) e.getSource()).getDisplay(), fi);
				}
			});
			setTabColor(display, index);

			// Set up a database check every now and then to see what a Display is actually doing (this is inSTEAD of the Ping
			// thread, above.
			new Thread("Display." + display.getNumber() + "." + display.getScreenNumber() + ".StatusUpdate") {
				private Statement	stmt;

				public void run() {
					try {
						sleep(25 * fi); // Put in a slight delay to get the Pings to each Display offset from each other a little
										// bit.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Connection connection = DisplayListDatabaseRemote.getConnection();
					String query = "";
					while (true) {
						try {
							sleep(PING_INTERVAL);
							// read from the database to see what's up with this Display

							try {
								stmt = connection.createStatement();
								query = "SELECT Content,ContentName FROM DisplayStatus WHERE DisplayID=" + display.getNumber();
								ResultSet rs = stmt.executeQuery(query);
								if (rs.first()) { // Move to first returned row
									while (!rs.isAfterLast()) {
										InputStream cn = rs.getAsciiStream("Content");
										if (cn != null) {
											String contentName = ConnectionToDynamicDisplaysDatabase.makeString(cn);
											if (contentName.startsWith("0: "))
												contentName = contentName.substring(3);
											if (contentName.contains(" is being displayed"))
												contentName = contentName.substring(0, contentName.indexOf(" is being displayed"));
											// Create a new footer
											String text = "<html><center>Disp " + display.getNumber() + ": "
													+ truncate(contentName, 27) + " " + shortDate() + "</center></html>";
											footer.setText(text);
											footer.setToolTipText(contentName);
											DisplayButtons.setToolTip(display);

											// Enable the Channel buttons, too
											// System.out.println("\nReceived content '" + contentName + "'");
											for (List<ChannelButtonGrid> allGrids : channelButtonGridList)
												if (allGrids.get(0).getDisplay().getNumber() == display.getNumber())
													for (ChannelButtonGrid cbg : allGrids)
														// if (contentName.contains(cbg.getId())) {
														for (int i = 0; i < cbg.getBg().getNumButtons(); i++) {
															MyButton myButton = cbg.getBg().getAButton(i);
															boolean selected = myButton.getChannel().toString().equals(contentName);
															String myButtonText = myButton.getText().replaceAll("\\<.*?>", "");
															// boolean selected = contentName.contains(myButtonText);
															myButton.setSelected(selected);
															// System.out.println("Display '" + display + "' button '"
															// + myButton.getText() + "'/'" + myButton.getChannel().toString()
															// + "/" + myButtonText + "' set to " + selected
															// + " for this message '" + contentName + "'");
														}
											// } else {
											// System.out.println("Display '" + display + ": Ignoring grid "
											// + cbg.getId() + " for this message '" + contentName + "'");
											// }

										}
										rs.next();
									}
								}
								rs.close();
								stmt.clearBatch();
								stmt.close();
							} catch (SQLException e) {
								System.out.println(query);
								e.printStackTrace();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();

			index++;
		}

		new Thread("WhoIsInChatRoom") {
			private MessagingClient	client;
			private boolean[]		aliveList		= null;
			private boolean[]		lastAliveList	= null;

			public void run() {
				login();
				long sleepTime = 1000;
				while (true) {
					try {
						sleep(sleepTime);
						sleepTime = 10000;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					lastAliveList = aliveList;
					aliveList = new boolean[displayList.size()];
					for (int i = 0; i < displayList.size(); i++) {
						aliveList[i] = false;
					}
					client.sendMessage(new MessageCarrier(MessageCarrier.WHOISIN, ""));

					try {
						sleep(2000); // Wait long enough for all the messages to come in.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					for (int i = 0; i < displayList.size(); i++) {
						if (lastAliveList == null || lastAliveList[i] != aliveList[i])
							setDisplayIsAlive(displayList.get(i).getNumber(), aliveList[i]);
					}
				}
			}

			private void login() {
				try {
					long ran = new Date().getTime() % 1000L;
					final String myName = InetAddress.getLocalHost().getCanonicalHostName() + "_" + ran;

					client = new MessagingClient(Util.MESSAGING_SERVER_NAME, MESSAGING_SERVER_PORT, myName) {
						public void displayIncomingMessage(final String msg) {
							if (msg.startsWith("WHOISIN") && !msg.contains("FA\u00c7ADE") && !msg.contains("Error")
									&& !msg.contains(myName)) {
								// Match the client name, "WHOISIN [(.*)] since <date>"
								String clientName = msg.substring(msg.indexOf('[') + 1, msg.indexOf(']'));
								// System.out.println("A client named '" + clientName + "' is alive");
								for (int i = 0; i < displayList.size(); i++) {
									if (displayList.get(i).getMessagingName().contains(clientName)) {
										// System.out.println("A client named '" + clientName + "' is a Display I know about!");
										// setDisplayIsAlive(D.getNumber(), true);
										aliveList[i] = true;
									}
								}
							}
						}
					};
					// start the Client
					if (!client.start())
						return;
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private void setDisplayIsAlive(int number, boolean alive) {
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
		title = new JLabel(" Control for XOC Display 00 ");
		if (!SHOW_IN_WINDOW)
			title.setFont(new Font("Serif", Font.ITALIC, (int) (3 * FONT_SIZE / 4)));
		// title.setFont(title.getFont().deriveFont(3 * FONT_SIZE / 4));
		title.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		title.setOpaque(true);
		title.setBackground(Color.white);
		titleBox = Box.createHorizontalBox();
		adjustTitle(displayList.get(0));

		if (!SHOW_IN_WINDOW) {
			refreshButton.setFont(new Font("SansSerif", Font.BOLD, (int) (FONT_SIZE / 2)));
		}
		refreshButton.setMargin(new Insets(5, 5, 5, 5));
		refreshButton.addActionListener(refreshAction);

		if (SHOW_IN_WINDOW) {
			// Create a button to add a channel to the database
			addChannelButton.addActionListener(new ActionListener() {
				boolean	showDialog	= true;

				@Override
				public void actionPerformed(ActionEvent arg0) {
					int n = 0;
					if (showDialog) {
						Box dialog = Box.createVerticalBox();
						dialog.add(new JLabel("<html>Launching web page to add a channel to database.<br>"
								+ "You must restart the Channel Selector to see the new channel once it is added.<br>"
								+ "Continue with the addition?"));
						JCheckBox cb = new JCheckBox("Do not show this again");
						dialog.add(cb);
						n = JOptionPane.showConfirmDialog(addChannelButton, dialog, "Add Channel?", JOptionPane.YES_NO_OPTION);
						showDialog = !cb.isSelected();
					}
					if (n == 0)
						try {
							Desktop.getDesktop().browse(new URI("http://" + Util.WEB_SERVER_NAME + "/XOC/channelAdd.php"));
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
					if (JOptionPane.showConfirmDialog(exitButton, "Do you _really_ want to exit the Channel Selector Application?",
							"Exit?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
						System.exit(0);
				}
			});
			Border bor = exitButton.getBorder();
			exitButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 5), bor));
			exitButton.setBackground(new Color(255, 200, 200));
			exitButton.setFont(new Font("SansSerif", Font.BOLD, (int) (FONT_SIZE / 4)));
			exitButton.setMargin(new Insets(5, 5, 5, 5));
		}
		return titleBox;
	}

	private void adjustTitle(Display display) {
		Color c = display.getPreferredHighlightColor();
		if (display != null) {
			title.setText(" Control for XOC " + (display.getCategory() == SignageType.XOC ? "" : display.getCategory() + " ")
					+ "Display " + display.getNumber() + " '" + display.getLocation() + "'");
		}
		titleBox.removeAll();
		titleBox.setOpaque(true);
		titleBox.setBackground(c);
		int wid = (SHOW_IN_WINDOW ? 1 : 8);
		titleBox.setBorder(BorderFactory.createLineBorder(c, wid));
		// This does not work, so remove it. titleBox.add(refreshButton);
		titleBox.add(Box.createHorizontalGlue());
		titleBox.add(title);
		titleBox.add(Box.createHorizontalGlue());
		if (!SHOW_IN_WINDOW) {
			titleBox.add(exitButton);
		} else {
			titleBox.add(addChannelButton);
		}
		titleBox.setOpaque(true);
	}

	private static JLabel makeChannelIndicator(Display display) {
		// JLabel footer = new JLabel(" Display " + display.getNumber() + " set to the '" + display.getContent().getCategory()
		// + "' channel '" + truncate(display.getContent().getName()) + "' (" + shortDate() + ")");
		JLabel footer = new JLabel("Display " + display.getNumber() + " (" + shortDate() + ")");
		DisplayButtons.setToolTip(display);

		int wid = 1, is = 1;
		if (!SHOW_IN_WINDOW) {
			wid = 3;
			is = 2;
			footer.setFont(new Font("Arial", Font.PLAIN, 25));
		}
		footer.setAlignmentX(CENTER_ALIGNMENT);
		// footer.setBorder(BorderFactory.createCompoundBorder(
		// BorderFactory.createLineBorder(display.getPreferredHighlightColor(), wid),
		// BorderFactory.createEmptyBorder(is, is, is, is)));

		return footer;
	}

	public void actionPerformed(ActionEvent e) {
		// The display selection has been changed, Select the "card" that shows this panel
		card.show(displayChannelPanel, e.getActionCommand());
		int displayNum = e.getID();
		adjustTitle(displayList.get(displayNum));
		setTabColor(displayList.get(displayNum), displayNum);
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

		displayList = DisplayListFactory.getInstance();
		channelSelector = new ChannelSelector();

		// SHOW_IN_WINDOW = args.length > 0 && "WINDOW".equals(args[0]);

		refreshAction = new ActionListener() {

			// FIXME This operation does not work! I suspect this is because of the globals and the socket connections (maybe they
			// don't get closed properly?)

			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						DisplayFacade.tryToConnectToDisplaysNow = true;
						System.out.println("Refreshing touch panel");
						f.setContentPane(new JLabel("refresh in progress"));
						channelSelector.destroy();
						channelSelector = null;
						displayList = null;
						Runtime.getRuntime().gc();
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						// Regenerate the Display list and the Channel list
						// DisplayListFactory.useRealDisplays(realDisplays);
						// ChannelCatalogFactory.useRealChannels(true);
						displayList = DisplayListFactory.getInstance();

						channelSelector = new ChannelSelector();
						f.setContentPane(channelSelector);
						if (SHOW_IN_WINDOW) {
							f.pack();
						} else {
							f.setSize(screenDimension);
						}
						f.setVisible(true);

					}
				});
			}
		};

		channelSelector.setRefreshAction(refreshAction);

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

	protected void destroy() {
		// TODO Auto-generated method stub

	}

	private void setRefreshAction(ActionListener refreshAction2) {
		refreshButton.addActionListener(refreshAction2);
	}
}
