package gov.fnal.ppd;

import static gov.fnal.ppd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.GlobalVariables.INACTIVITY_TIMEOUT;
import static gov.fnal.ppd.GlobalVariables.INSET_SIZE;
import static gov.fnal.ppd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.GlobalVariables.MESSAGING_SERVER_NAME;
import static gov.fnal.ppd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.GlobalVariables.PING_INTERVAL;
import static gov.fnal.ppd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.GlobalVariables.WEB_SERVER_NAME;
import static gov.fnal.ppd.GlobalVariables.bgImage;
import static gov.fnal.ppd.GlobalVariables.displayList;
import static gov.fnal.ppd.GlobalVariables.imageHeight;
import static gov.fnal.ppd.GlobalVariables.imageNames;
import static gov.fnal.ppd.GlobalVariables.imageWidth;
import static gov.fnal.ppd.GlobalVariables.lastDisplayChange;
import static gov.fnal.ppd.GlobalVariables.locationCode;
import static gov.fnal.ppd.GlobalVariables.locationDescription;
import static gov.fnal.ppd.GlobalVariables.locationName;
import static gov.fnal.ppd.GlobalVariables.offsets;
import static gov.fnal.ppd.signage.util.Util.launchMemoryWatcher;
import gov.fnal.ppd.chat.MessageCarrier;
import gov.fnal.ppd.chat.MessagingClient;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.ChannelButtonGrid;
import gov.fnal.ppd.signage.changer.ChannelCategory;
import gov.fnal.ppd.signage.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.signage.changer.DDButton;
import gov.fnal.ppd.signage.changer.DetailedInformationGrid;
import gov.fnal.ppd.signage.changer.DisplayButtons;
import gov.fnal.ppd.signage.changer.DisplayChangeEvent;
import gov.fnal.ppd.signage.changer.DisplayListFactory;
import gov.fnal.ppd.signage.changer.PublicInformationGrid;
import gov.fnal.ppd.signage.channel.CreateListOfChannelsHelper;
import gov.fnal.ppd.signage.display.DisplayFacade;
import gov.fnal.ppd.signage.display.DisplayListDatabaseRemote;
import gov.fnal.ppd.signage.util.DisplayButtonGroup;
import gov.fnal.ppd.signage.util.DisplayCardActivator;
import gov.fnal.ppd.signage.util.JLabelCenter;
import gov.fnal.ppd.signage.util.JLabelFooter;
import gov.fnal.ppd.signage.util.SimpleMouseListener;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
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
public class ChannelSelector extends JPanel implements ActionListener, DisplayCardActivator {

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

	private static final Dimension			screenDimension			= Toolkit.getDefaultToolkit().getScreenSize();
	private static JFrame					f						= new JFrame("XOC Display Channel Selector");

	private static ActionListener			fullRefreshAction		= null;
	private static ActionListener			channelRefreshAction	= null;
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
	private Box[]							splashPanel				= new Box[imageNames.length];

	private String							lastActiveDisplay		= null;

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
		launchMemoryWatcher();
	}

	private void initComponents() {
		removeAll();
		SignageType cat = SignageType.XOC; // Show everything
		if (IS_PUBLIC_CONTROLLER)
			cat = SignageType.Public;

		displaySelector = new DisplayButtons(cat, this);
		lastActiveDisplay = displaySelector.getFirstDisplay().toString();
		initChannelSelectors();

		add(displayChannelPanel, BorderLayout.CENTER);
		if (SHOW_IN_WINDOW) {
			displayChannelPanel.setPreferredSize(new Dimension(700, 640));
		}

		add(displaySelector, BorderLayout.EAST);
		add(makeTitle(), BorderLayout.NORTH);

		createSplashScreen();

		if (!SHOW_IN_WINDOW)
			// Only enable the splash screen for the full-screen version
			new Thread() {
				public void run() {
					int index = 0;
					while (true) {
						try {
							if (System.currentTimeMillis() > INACTIVITY_TIMEOUT + lastDisplayChange) {
								card.show(displayChannelPanel, "Splash Screen" + index);
								// System.out.println("Showing splash screen " + index);
								index = (index + 1) % (splashPanel.length);
								lastDisplayChange = System.currentTimeMillis();
							}
							sleep(ONE_SECOND);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();

	}

	private void createSplashScreen() {
		// Create a nice splash screen that it comes back to after a period of inactivity
		SimpleMouseListener splashListener = new SimpleMouseListener(this);

		// char arrow = '\u21E8';
		float headline = 30, subHead = 18;
		int gap = 25;
		if (!SHOW_IN_WINDOW) {
			headline = 72;
			subHead = 32;
			gap = 50;
		}
	

		int lc = (locationCode < 0 ? locationName.length - 1 : locationCode);
		for (int index = 0; index<splashPanel.length; index++) {
			Box splash = splashPanel[index] = Box.createVerticalBox();
			final int mine = index;
			JPanel p = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);
					// Implement a "fit" algorithm so the image is seen in its entirety, not stretched or cropped.
					int w = getWidth();
					int h = getHeight();
					int x = 0, y = 0;
					try {
						double imgAspect = ((double) imageWidth[mine]) / ((double) imageHeight[mine]); // Say 16:9 or 1.778
						double scrAspect = ((double) w) / ((double) h); // Say 3:2 or 1.5
						if (imgAspect > scrAspect) {
							// image is wider than the screen: reduce the height of the screen (and it will fill the width)
							h = (int) (((double) w) / imgAspect);
							y = (getHeight() - h) / 2;
						} else {
							// screen is wider than the image: Reduce the width of the screen (and it will fill the height)
							w = (int) (((double) h) * imgAspect);
							x = (getWidth() - w) / 2;
						}
						// System.out.println(w + "," + h + "," + getWidth() + "," + getHeight() + "," + x + "," + y + "," +
						// imgAspect + "," + scrAspect);

					} catch (Exception e) {
						// ignore what is probably a divide-by-zero exception
					}
					g.drawImage(bgImage[mine], x, y, w, h, this); // draw the image
				}
			};
			p.setBackground(Color.black);
			p.setOpaque(true);
			splash.addMouseListener(splashListener);
			splash.add(Box.createRigidArea(new Dimension(50, 50)));
			int h = offsets[index];
			System.out.println("Splash screen " + index + " has vertical offset of " + h);
			splash.add(Box.createRigidArea(new Dimension(100, h)));
			splash.add(new JLabelCenter("   Welcome to " + locationName[lc] + "!   ", headline));
			splash.add(Box.createRigidArea(new Dimension(50, gap)));
			splash.add(new JLabelCenter("   " + locationDescription[lc] + "   ", subHead));
			splash.add(Box.createRigidArea(new Dimension(50, gap)));
			splash.add(new JLabelCenter("<html><em>Touch to continue</em></html>", subHead));
			// splash.add(new JLabelCenter("" + arrow, arrowSize));

			// splash.setOpaque(true);
			// splash.setBackground(new Color(200 + (int) (Math.random() * 40.0), 200 + (int) (Math.random() * 40.0),
			// 200 + (int) (Math.random() * 40.0)));
			p.add(splash);
			displayChannelPanel.add(p, "Splash Screen" + index);
		}
		if (!SHOW_IN_WINDOW)
			card.show(displayChannelPanel, "Splash Screen0");
	}

	private void initChannelSelectors() {
		// Three (or more) tabs representing Public, First set of Details, Full set of Details (or
		// something like that) Each Display has its own set of channels.

		int index = 0;
		for (final Display display : displayList) {
			JTabbedPane displayTabPane = new JTabbedPane();
			displayTabPane.setFont(getFont().deriveFont((SHOW_IN_WINDOW ? 12.0f : 40.0f)));
			displayTabPane.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					selectedTab = ((JTabbedPane) e.getSource()).getSelectedIndex();
				}
			});
			DisplayButtonGroup bg = new DisplayButtonGroup();

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
				// Do not add the "Experiment Details" tabs to a Public controller

				grid = new DetailedInformationGrid(display, bg, 3);
				allGrids.add(grid);
				display.addListener(grid);
				displayTabPane.add(grid, " NOvA ");

				grid = new DetailedInformationGrid(display, bg, 4);
				allGrids.add(grid);
				display.addListener(grid);
				displayTabPane.add(grid, " NuMI ");

				grid = new DetailedInformationGrid(display, bg, 2);
				allGrids.add(grid);
				display.addListener(grid);
				displayTabPane.add(grid, " EXP ");
			}

			grid = new DetailedInformationGrid(display, bg, -1);
			allGrids.add(grid);
			display.addListener(grid);
			displayTabPane.add(grid, " MISC ");

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
				}

			});

			if (!IS_PUBLIC_CONTROLLER) {
				final CreateListOfChannelsHelper channelLister = new CreateListOfChannelsHelper();
				displayTabPane.add(channelLister.listerPanel, " Lists ");
				channelLister.accept.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						System.out.println("Channel list accepted");
						display.setContent(channelLister.lister.getChannelList());
					}
				});
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
					DisplayChangeEvent ev = (DisplayChangeEvent) e;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}

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

			final int fi = index;
			bg.setActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					setTabColor(((DDButton) e.getSource()).getDisplay(), fi);
				}
			});
			setTabColor(display, index);

			// Set up a database check every now and then to see what a Display is actually doing (this is inSTEAD of the Ping
			// thread, above).
			new Thread("Display." + display.getNumber() + "." + display.getScreenNumber() + ".StatusUpdate") {

				public void run() {
					try {
						sleep(25 * fi); // Put in a slight delay to get the Pings to each Display offset from each other a little
										// bit.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Connection connection = DisplayListDatabaseRemote.getConnection();
					while (true) {
						try {
							sleep(PING_INTERVAL);
							// read from the database to see what's up with this Display

							String query = "SELECT Content,ContentName FROM DisplayStatus WHERE DisplayID=" + display.getNumber();
							// Use ARM (Automatic Resource Management) to assure that things get closed properly (a new Java 7
							// feature)
							try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query);) {
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
											String text = "Disp " + display.getNumber() + ": " + (contentName);
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
															DDButton myButton = cbg.getBg().getAButton(i);
															boolean selected = myButton.getChannel().toString().equals(contentName);
															// String myButtonText = myButton.getText().replaceAll("\\<.*?>", "");
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
							} catch (SQLException e) {
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
					client.sendMessage(MessageCarrier.getWhoIsIn());

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

					System.out.println("\n**Starting messaging client named '" + myName + "'");
					client = new MessagingClient(MESSAGING_SERVER_NAME, MESSAGING_SERVER_PORT, myName) {
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
		// refreshButton.addActionListener(fullRefreshAction);
		refreshButton.addActionListener(channelRefreshAction);

		if (SHOW_IN_WINDOW) {
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
			/*
			 * title.setText("  Control for XOC " + (display.getCategory() == SignageType.XOC ? "" : display.getCategory() + " ") +
			 * "Display " + display.getNumber() + " '" + display.getLocation() + "'  ");
			 */
			// title.setText("  Control for Display " + display.getNumber() + " '" + display.getLocation() + "'  ");
			title.setText("   " + display.getLocation() + "   ");
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
		// Do not let the simple public controller exit
		if (!SHOW_IN_WINDOW) {
			if (!IS_PUBLIC_CONTROLLER)
				titleBox.add(exitButton);
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

		// int wid = 1, is = 1;
		if (!SHOW_IN_WINDOW) {
			// wid = 3;
			// is = 2;
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

		// SHOW_IN_WINDOW = args.length > 0 && "WINDOW".equals(args[0]);

		createRefreshActions(sType);

		channelSelector.setRefreshAction(fullRefreshAction);

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

	/**
	 * @return the location code for this instance of the channel selector
	 */
	public static int getLocationCode() {
		return locationCode;
	}

	private static void createRefreshActions(final SignageType sType) {
		fullRefreshAction = new ActionListener() {

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
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						// Regenerate the Display list and the Channel list
						// DisplayListFactory.useRealDisplays(realDisplays);
						// ChannelCatalogFactory.useRealChannels(true);
						displayList = DisplayListFactory.getInstance(sType, getLocationCode());

						channelSelector = new ChannelSelector();
						channelSelector.setRefreshAction(fullRefreshAction);

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
					}
				});
			}
		};
	}

	protected void destroy() {
		// Do everything we can to forget everything we can.
		// TOD Make this work -- no luck so far!
	}

	private void setRefreshAction(ActionListener refreshAction2) {
		refreshButton.addActionListener(refreshAction2);
	}

	@Override
	public void activateCard() {
		card.show(displayChannelPanel, lastActiveDisplay);
		System.out.println("Activating card '" + lastActiveDisplay + "'");
	}
}
