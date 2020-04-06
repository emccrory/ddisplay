/*
 * CreateListOfChannels
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.ChannelSelector.screenDimension;
import static gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameSelector;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.channel.list.ListUtilsGUI.getDwellStrings;
import static gov.fnal.ppd.dd.channel.list.ListUtilsGUI.interp;
import static gov.fnal.ppd.dd.util.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.GeneralUtilities.getOrdinalSuffix;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import gov.fnal.ppd.dd.CredentialsNotFoundException;
import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.changer.ChannelClassificationDictionary;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelInListImpl;
import gov.fnal.ppd.dd.channel.ChannelListHolder;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.channel.ConcreteChannelListHolder;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.BigLabel;

/**
 * Let the user create a list of channels for a display to play. This is used in CreateListOfChannelsHelper as a GUI element that is
 * presented to ChannelSelector
 * 
 * FIXME This class mixes View, Model and Controller elements.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class CreateListOfChannels extends JPanel implements ChannelListHolder {

	private static final long			serialVersionUID	= 2157704848183269779L;
	static final String					NOT_SELECTED		= "                  ";
	/**
	 * 
	 */
	public static final String			CANCELLED			= "** CANCELLED **";

	// FIXME -- This kludge job is not working. Adding the Plus and Minus buttons has messed this up.

	ChannelListHolder					channelList			= new ConcreteChannelListHolder();
	List<JLabel>						labelList			= new ArrayList<JLabel>();
	List<ChanSelectButton>				allChannelButtons	= new ArrayList<ChanSelectButton>();
	private static List<TinyButton>		allTinyButtons		= new ArrayList<TinyButton>();

	private JSpinner					time;
	private SaveRestoreListOfChannels	saveRestore;

	Map<String, JLabel>					allLabels			= new HashMap<String, JLabel>();
	private Box							timeWidgets			= Box.createHorizontalBox();
	private Box							selectedChannels	= Box.createVerticalBox();

	static private class ChanSelectButton extends JCheckBox {
		// TODO -- Change this to a button that can add the same channel over and over again.
		private static final long	serialVersionUID	= 6517317474435639087L;
		private SignageContent		channel;
		private JButton				b1;
		private JButton				b2;

		public ChanSelectButton(String title, SignageContent c, JButton b1, JButton b2) {
			super(title);
			this.channel = c;
			this.b1 = b1;
			this.b2 = b2;
			if (!SHOW_IN_WINDOW) {
				setFont(new Font("Arial", Font.BOLD, 20));
				setMargin(new Insets(5, 15, 5, 14));
			}
			setAlignmentX(JComponent.CENTER_ALIGNMENT);
		}

		public boolean isChannel(final SignageContent c) {
			if (c != null)
				return c.equals(channel);
			return channel != null;
		}

		@Override
		public void setSelected(boolean s) {
			super.setSelected(s);
			b1.setVisible(s);
			b2.setVisible(s);
		}
	}

	private static class JLabelPlain extends JLabel {
		private static final long serialVersionUID = -2971746349833357406L;

		public JLabelPlain(String text) {
			super(text);
			setFont(new Font("Arial", Font.PLAIN, (SHOW_IN_WINDOW ? 11 : 13)));
		}
	}

	private class TinyButton extends JButton {
		private static final long	serialVersionUID	= 5525783748090052622L;
		private SignageContent		content;

		public TinyButton(final char title, final JLabel lab, SignageContent c) {
			super("" + (title == '+' ? "\u25b2" : "\u25bc"));
			if (!SHOW_IN_WINDOW) {
				setFont(new Font("Arial", Font.BOLD, 14));
				setMargin(new Insets(5, 24, 5, 24));
			} else {
				setFont(new Font("Arial", Font.PLAIN, 6));
				setMargin(new Insets(0, 2, 0, 2));
			}
			lab.setOpaque(false);

			this.content = c;

			setAlignmentX(JComponent.CENTER_ALIGNMENT);
			addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (!lab.getText().equals(NOT_SELECTED)) {
						String t = lab.getText();
						String ordinal = "";
						long dwell = 0;
						if (t.startsWith("* ")) {
							String c = t.substring(t.indexOf('[') + 1, t.indexOf(']'));
							dwell = Long.parseLong(c);
							ordinal = t.substring(2, t.substring(3).indexOf(" ") + 3);
						} else if (t.length() > 0)
							dwell = Long.parseLong(t);

						if (dwell * 1000L != content.getTime()) {
							System.out.println("** Have hit the bad spot in TinyButton increment/decrement.  " + dwell + ", "
									+ content.getTime() / 1000L + " -- PUNT! **");
						} else {
							long increment = 1L;
							if (dwell > 10800L)
								increment = 3600L;
							else if (dwell > 3600L)
								dwell = 600L;
							else if (dwell > 180L)
								increment = 60L;
							else if (dwell > 45L)
								increment = 5L;

							if (title == '+')
								dwell += increment;
							else if (title == '-')
								dwell -= increment;

							if (dwell <= 0)
								lab.setText(NOT_SELECTED);
							else
								lab.setText("* " + ordinal + " chann (show for [" + dwell + "] secs) *");

							content.setTime(dwell * 1000L);
						}
						fixLabels();
					}
				}
			});
			allTinyButtons.add(this);
		}
	}

	static void checkContent(SignageContent c) {
		if (c == null)
			return;
		for (TinyButton TB : allTinyButtons) {
			if (c.getName().equals(TB.content.getName()))
				TB.content = c;
		}
	}

	/**
	 * @return the save-restore widget
	 */
	public JPanel getSaveRestore() {
		return saveRestore;
	}

	CreateListOfChannels(Color c) {
		super();
		saveRestore = new SaveRestoreListOfChannels(this);
		layoutButtons(c);
	}

	private void layoutButtons(Color c) {
		setLayout(new BorderLayout());

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
		// timeWidgets.add(Box.createRigidArea(new Dimension(10, 10)));

		// ----------------------------------------

		ChannelClassification categories[] = ChannelClassificationDictionary.getCategories();
		Box mainPanel = Box.createVerticalBox();
		final Color bg1 = new Color(235, 230, 230);
		final Color bg2 = new Color(241, 241, 244);
		final Color bor1 = c; // Color.red.darker();
		final Color bor2 = Color.black; // Color.blue;

		Color bgColor = bg2;
		Color borColor = bor2;

		for (ChannelClassification C : categories) {
			if (C.getValue().equals("Archive"))
				continue;

			bgColor = (bgColor == bg2 ? bg1 : bg2);
			borColor = (borColor == bor2 ? bor1 : bor2);

			String sep = "----- " + C + " -----";
			for (int len = C.getValue().length(); len < 18; len += 4)
				sep = "--" + sep + "--";

			BigLabel catLabel = new BigLabel(" " + sep + " ", Font.BOLD);
			catLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borColor),
					BorderFactory.createEmptyBorder(2, 2, 2, 2)));
			mainPanel.add(catLabel);

			int nc = (SHOW_IN_WINDOW ? 2 : 2);
			JPanel inner = new JPanel(new GridLayout(0, nc, 1, 5));

			inner.setOpaque(true);
			inner.setBackground(bgColor);
			catLabel.setBackground(bgColor);

			mainPanel.add(inner);
			// mainPanel.add(Box.createRigidArea(new Dimension(100, 20)));
			Set<SignageContent> chans = ChannelCatalogFactory.getInstance().getChannelCatalog(C);
			for (final SignageContent CONTENT : chans) {
				String nm = CONTENT.getName();
				if (nm.length() > 30) {
					nm = nm.substring(0, 28) + "...";
				}

				final JLabel lab = new JLabel(NOT_SELECTED);
				final TinyButton addTime = new TinyButton('+', lab, CONTENT);
				final TinyButton subTime = new TinyButton('-', lab, CONTENT);
				final ChanSelectButton bb = new ChanSelectButton(nm, CONTENT, addTime, subTime);

				allChannelButtons.add(bb);
				lab.setBackground(Color.white);
				lab.setOpaque(true);
				lab.setFont(new Font("Arial", Font.PLAIN, (SHOW_IN_WINDOW ? 11 : 16)));

				final Box box = Box.createVerticalBox();
				box.setOpaque(false);
				JPanel p = new JPanel(new BorderLayout(0, 0));
				p.add(bb);
				box.add(p);
				box.add(Box.createRigidArea(new Dimension(5, 5)));
				Box hb = Box.createHorizontalBox();
				hb.add(lab);
				hb.add(Box.createRigidArea(new Dimension(2, 2)));

				addTime.setVisible(false);
				subTime.setVisible(false);
				hb.add(addTime);
				hb.add(Box.createRigidArea(new Dimension(2, 2)));
				hb.add(subTime);
				hb.add(Box.createGlue());
				box.add(hb);

				box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borColor),
						BorderFactory.createEmptyBorder(2, 2, 2, 2)));

				inner.add(box);

				allLabels.put(CONTENT.getName(), lab);
				bb.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						boolean selected = channelList.getList().contains(CONTENT);
						boolean visible = false;
						if (selected) {
							while (channelList.getList().remove(CONTENT))
								; // Remove them all
							lab.setText(NOT_SELECTED);
							lab.setOpaque(false);
							labelList.remove(lab);
						} else {
							long defaultDwell = (Long) time.getValue() * 1000L;
							long actualDwell = 0;
							if (CONTENT.getTime() == 0 || CONTENT.getTime() > defaultDwell) {
								CONTENT.setTime(defaultDwell);
								channelList.channelAdd(new ChannelInListImpl(CONTENT));
								actualDwell = defaultDwell;
								visible = true;
							} else {
								// Simple change here: Based on the internal refresh time of the channel, add 1 or more instances of
								// this channel the the list. E.g., if the user is asking for a dwell of an hour, but the channel
								// has a refresh of 20 minutes, it will put the channel in the list 3 times, at 20 minutes per.
								long tm = defaultDwell;
								int added = 0;
								for (; tm >= CONTENT.getTime(); tm -= CONTENT.getTime()) {
									if (CONTENT instanceof ChannelInList)
										channelList.channelAdd((ChannelInList) CONTENT);
									else
										channelList.channelAdd(new ChannelInListImpl(CONTENT));
									actualDwell += CONTENT.getTime();
									added++;
								}
								visible = added == 1;
							}
							lab.setText("" + actualDwell / 1000L);
							labelList.add(lab);
						}
						addTime.setVisible(visible);
						subTime.setVisible(visible);
						fixLabels();
					}
				});
			}
		}

		add(new JScrollPane(mainPanel), BorderLayout.CENTER);
	}

	protected void fixLabels() {
		int count = 1;

		// This loop modifies all the labels. Just prior to this call, the new channel selection was labeled with the dwell time
		// only. Now we adjust that label to be the complete, "* 5th chan (show for [30] secs) *".

		for (JLabel LAB : labelList) {
			if (!LAB.getText().equals(NOT_SELECTED)) {
				String t = LAB.getText();
				long dwell = 0;
				if (t.startsWith("* ")) {
					String c = t.substring(t.indexOf('[') + 1, t.indexOf(']'));
					dwell = Long.parseLong(c);
				} else if (t.length() > 0)
					dwell = Long.parseLong(t);

				if (dwell > 0) {
					String ordinal = getOrdinalSuffix(count);
					LAB.setText("* " + (count++) + ordinal + " chan (show for [" + dwell + "] secs) * ");
					LAB.setOpaque(true);
				} else {
					LAB.setText(NOT_SELECTED);
					LAB.setOpaque(false);
				}
			}
		}

		// Now populate the succinct list of channels that has been accumulated so far

		selectedChannels.removeAll();
		JLabel title = new JLabel(
				"  " + channelList.getList().size() + " channel" + (channelList.getList().size() != 1 ? 's' : "") + " in the list");
		title.setFont(new Font("Arial", Font.BOLD, (SHOW_IN_WINDOW ? 14 : 20)));
		selectedChannels.add(title);

		count = 1;
		for (SignageContent C : channelList.getList()) {
			selectedChannels.add(
					new JLabelPlain(count + getOrdinalSuffix(count) + ": " + C.getName() + " [" + C.getTime() / 1000L + " sec]"));
			count++;
			for (ChanSelectButton AB : allChannelButtons) {
				ChanSelectButton bb = AB;
				if (bb.isChannel(C))
					AB.setSelected(true);
			}
		}
		selectedChannels.revalidate();
		selectedChannels.repaint();
		invalidate();
	}

	/**
	 * Get the pre-assembled GUI widgets for doing the list of channels.
	 * 
	 * @param h
	 *            the helper object
	 * 
	 * @param listener
	 *            the listener for when the "Accept this list" button get pressed. Leave this null, and it will exit when the button
	 *            is pressed.
	 * @return The JPanel that contains all the GUI widgets
	 * 
	 */
	public static Container getContainer(CreateListOfChannelsHelper h, final ActionListener listener) {

		final CreateListOfChannelsHelper helper = h;

		if (listener == null)
			helper.accept.addActionListener(new ActionListener() {
				// Used ONLY for testing
				@Override
				public void actionPerformed(ActionEvent e) {
					int count = 1;
					long totalTime = 0;
					for (SignageContent CONTENT : helper.lister.channelList.getList())
						if (CONTENT instanceof Channel) {
							totalTime += CONTENT.getTime();
							System.out.println(count++ + " - Channel no. " + ((Channel) CONTENT).getNumber() + ": "
									+ CONTENT.getName() + ", " + CONTENT.getTime());
						} else {
							System.out.println(count++ + " - " + CONTENT.getName() + ", dwell=" + CONTENT.getTime() + " msec");
						}
					System.out.println("Total time: " + totalTime + " milliseconds");
					catchSleep(10);
					System.exit(0);
				}
			});
		else
			helper.accept.addActionListener(listener);

		// The thing we are going to return
		JPanel retval = new JPanel(new BorderLayout());

		// Make the button to accept this list really pretty
		helper.accept.setBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(h.color, 3), helper.accept.getBorder()));

		// The scroll panel of all the channels
		final JScrollPane mainSelectionScrollPanel = new JScrollPane(helper.lister);
		mainSelectionScrollPanel.getVerticalScrollBar().setUnitIncrement(16);
		if (!SHOW_IN_WINDOW)
			mainSelectionScrollPanel.getVerticalScrollBar().setPreferredSize(new Dimension(40, 0));

		// Set up the left-side list of channels that has been created
		JLabel title = new JLabel("Zero channels in the list");
		title.setFont(new Font("Arial", Font.BOLD, (SHOW_IN_WINDOW ? 14 : 20)));
		helper.lister.selectedChannels.add(title);
		final JScrollPane listOfChannelScrollPanel = new JScrollPane(helper.lister.selectedChannels);
		if (!SHOW_IN_WINDOW)
			listOfChannelScrollPanel.getHorizontalScrollBar().setPreferredSize(new Dimension(40, 0));

		// Put all of these panels nicely onto the JPanel we have
		JPanel hb = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(1, 1, 1, 1), 0, 0);
		hb.add(listOfChannelScrollPanel, gbc);
		gbc = new GridBagConstraints(2, 1, 3, 1, 3.0, 1.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1),
				0, 0);
		hb.add(mainSelectionScrollPanel, gbc);
		hb.setBorder(BorderFactory.createLineBorder(h.color, 2));
		retval.add(hb, BorderLayout.CENTER);

		Box vb = Box.createVerticalBox();
		vb.add(helper.listerPanel);
		vb.add(helper.lister.timeWidgets);

		retval.add(vb, BorderLayout.NORTH);

		retval.add(helper.lister.saveRestore, BorderLayout.SOUTH);
		return retval;
	}

	// {
	//
	// /*
	// * (non-Javadoc)
	// *
	// * @see javax.swing.JComponent#paint(java.awt.Graphics)
	// */
	// @Override
	// public void paint(Graphics g) {
	// // super.paint(g);
	//
	// int h = getHeight();
	// int w1 = getWidth() / 4;
	// int w2 = getWidth() - w1;
	//
	// listOfChannelScrollPanel.setSize(new Dimension(w1, h));
	// mainSelectionScrollPanel.setSize(new Dimension(w2, h));
	//
	// System.out.println("h,width,w1,w2:" + h + "," + getWidth() + "," + w1 + "," + w2);
	// super.paint(g);
	// }
	//
	// }

	/**
	 * @return A play list of channels
	 */
	public SignageContent getChannelList() {
		return new ChannelPlayList(channelList, getDwellTime());
	}

	/**
	 * @return How long (in milliseconds) to stay on each channel
	 */
	public long getDwellTime() {
		return (Long) time.getValue();
	}

	/**
	 * @param args
	 *            Command line arguments (none expected)
	 */
	public static void main(final String[] args) {

		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		getMessagingServerNameSelector();

		// ChannelCatalogFactory.useRealChannels(true);

		JFrame f = new JFrame(CreateListOfChannels.class.getCanonicalName() + " Testing;");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		CreateListOfChannelsHelper h = new CreateListOfChannelsHelper(Color.green);

		f.setContentPane(getContainer(h, null));
		if (SHOW_IN_WINDOW) {
			f.setSize(900, 900);
		} else {
			f.setUndecorated(true);
			f.setSize(screenDimension);
		}
		f.setVisible(true);
	}

	// The ChannelListHolder signature -->
	@Override
	public void clear() {
		channelList.clear();
		labelList.clear();

		for (JLabel L : allLabels.values()) {
			L.setText(CreateListOfChannels.NOT_SELECTED);
			L.setOpaque(false);
		}
		for (AbstractButton AB : allChannelButtons) {
			AB.setSelected(false);
		}
	}

	@Override
	public void channelAdd(ChannelInList c) {
		System.out.println("Adding channel [" + c + "] to inner list");
		channelList.channelAdd(c);

		JLabel lab = allLabels.get(c.getName());
		lab.setText("" + c.getTime() / 1000L);
		labelList.add(lab);
	}

	@Override
	public List<ChannelInList> getList() {
		return channelList.getList();
	}

	@Override
	public void fix() {
		fixLabels();
	}
}
