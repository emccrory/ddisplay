/*
 * CreateListOfChannels
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import gov.fnal.ppd.dd.changer.CategoryDictionary;
import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Let the user create a list of channels for a display to play. This is used in CreateListOfChannelsHelper as a GUI element that is
 * presented to ChannelSelector
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class CreateListOfChannels extends JPanel {

	private static final long		serialVersionUID	= 2157704848183269779L;
	private static final String		NOT_SELECTED		= "                  ";

	private List<SignageContent>	channelList			= new ArrayList<SignageContent>();
	private List<JLabel>			labelList			= new ArrayList<JLabel>();
	private JSpinner				time;

	static class BigButton extends JButton {
		private static final long	serialVersionUID	= 6517317474435639087L;

		public BigButton(String title) {
			super(title);
			if (!SHOW_IN_WINDOW) {
				setFont(getFont().deriveFont(30.0f));
				setMargin(new Insets(10, 10, 10, 10));
			}
			setAlignmentX(JComponent.CENTER_ALIGNMENT);
		}
	}

	private static class BigLabel extends JLabel {
		private static final long	serialVersionUID	= 8296427549410976741L;

		public BigLabel(String title, int style) {
			super(title);
			if (!SHOW_IN_WINDOW)
				setFont(new Font("Sans Serif", style, 20));
			setAlignmentX(JComponent.CENTER_ALIGNMENT);
		}
	}

	CreateListOfChannels() {
		super(new GridBagLayout());
		alt();

	}

	@SuppressWarnings("unused")
	private void makeButtons() {
	}

	private void alt() {
		setLayout(new GridBagLayout());

		//
		// TODO Instead of a long vertical list of channels in the "Channel List" GUI, organize them into separate panels, with a
		// titled border
		//

		GridBagConstraints bag = new GridBagConstraints();
		bag.fill = GridBagConstraints.HORIZONTAL;
		bag.gridx = bag.gridy = 1;
		if (SHOW_IN_WINDOW)
			bag.insets = new Insets(2, 2, 2, 2);
		else
			bag.insets = new Insets(6, 6, 6, 6);
		bag.anchor = GridBagConstraints.CENTER;

		add(Box.createRigidArea(new Dimension(10, 10)), bag);
		bag.gridy++;

		Box bh = Box.createHorizontalBox();
		bh.add(new BigLabel("Dwell time (msec): ", Font.PLAIN));

		final SpinnerModel model = new SpinnerListModel(getDwellStrings());
		time = new JSpinner(model);
		time.setValue(new Long(20000l));
		time.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		if (!SHOW_IN_WINDOW)
			time.setFont(new Font("Monospace", Font.PLAIN, 40));
		bh.add(time);
		final JLabel timeInterpretLabel = new JLabel("20 seconds");
		time.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				long val = (Long) model.getValue();
				String t = val + " milliseconds";
				if (val < 60000L) {
					t = (val / 1000) + " seconds";
				} else if (val < 3600000) {
					double min = ((double) val) / 60000.0;
					t = min + " minutes";
				} else {
					double hours = ((double) val) / (60 * 60 * 1000.0);
					t = hours + " hours";
				}
				timeInterpretLabel.setText(t);
			}
		});
		bh.setAlignmentX(JComponent.CENTER_ALIGNMENT);

		bag.gridwidth = 2;
		add(bh, bag);
		bag.gridy++;

		add(timeInterpretLabel, bag);
		bag.gridy++;

		ChannelCategory categories[] = CategoryDictionary.getCategories();

		for (ChannelCategory C : categories) {
			bag.gridwidth = 2;
			add(new JSeparator(), bag);
			bag.gridy++;

			String sep = "--------------- " + C + " ----------------";
			// if ( C.getValue().length() < 10 )
			// sep = "--" + sep + "--";
			for (int len = C.getValue().length(); len < 18; len += 2)
				sep = "-" + sep + "--";

			add(new BigLabel(sep, Font.BOLD), bag);
			bag.gridy++;

			bag.gridwidth = 1;
			Set<SignageContent> chans = ChannelCatalogFactory.getInstance().getChannelCatalog(C);
			for (final SignageContent CONTENT : chans) {
				final JButton b = new BigButton(CONTENT.getName());
				add(b, bag);
				bag.gridx++;
				final JLabel lab = new BigLabel(NOT_SELECTED, Font.ITALIC);
				add(lab, bag);
				bag.gridx--;
				bag.gridy++;
				b.addActionListener(new ActionListener() {
					boolean	selected	= false;

					@Override
					public void actionPerformed(ActionEvent arg0) {
						CONTENT.setTime((Long) time.getValue());
						if (selected) {
							selected = false;
							channelList.remove(CONTENT);
							lab.setText(NOT_SELECTED);
							labelList.remove(lab);
						} else {
							selected = true;
							channelList.add(CONTENT);
							lab.setText("" + CONTENT.getTime());
							labelList.add(lab);
						}
						fixLabels();
					}
				});
			}

		}

		bag.gridx++;
		add(Box.createRigidArea(new Dimension(10, 10)), bag);
	}

	private static List<Long> getDwellStrings() {
		ArrayList<Long> retval = new ArrayList<Long>();

		retval.add(5000l);
		retval.add(7500l);
		retval.add(8000l);
		retval.add(9000l);
		retval.add(10000l); // 10 seconds
		retval.add(12000l);
		retval.add(15000l);
		retval.add(20000l);
		retval.add(30000l);
		retval.add(45000l);
		retval.add(60000l);
		retval.add(75000l);
		retval.add(90000l);
		retval.add(100000l); // 100 seconds
		retval.add(110000l);
		retval.add(120000l); // 2 minutes
		retval.add(150000l);
		retval.add(180000l);
		retval.add(210000l);
		retval.add(240000l);
		retval.add(300000l); // 5 minutes
		retval.add(360000l);
		retval.add(420000l);
		retval.add(480000l);
		retval.add(540000l);
		retval.add(600000l); // 10 minutes
		retval.add(1200000l);
		retval.add(1800000l);
		retval.add(2400000l);
		retval.add(3600000l); // One hour
		retval.add(2 * 3600000l);
		retval.add(3 * 3600000l);
		retval.add(4 * 3600000l);
		retval.add(5 * 3600000l);
		retval.add(6 * 3600000l);
		retval.add(8 * 3600000l); // 8 hours

		return retval;
	}

	protected void fixLabels() {
		int count = 1;
		for (JLabel LAB : labelList) {
			if (!LAB.getText().equals(NOT_SELECTED)) {
				String t = LAB.getText();
				if (t.startsWith("**")) {
					String c = t.substring(t.indexOf('(') + 1, t.indexOf(')'));
					LAB.setText("** No. " + (count++) + " (" + c + ") **");
				} else
					LAB.setText("** No. " + (count++) + " (" + t + ") **");
			}
		}
	}

	// Test this GUI. Note that it exits when you click the button!
	private static Container getContainer() {
		final CreateListOfChannelsHelper helper = new CreateListOfChannelsHelper();

		helper.accept.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int count = 1;
				for (SignageContent CONTENT : helper.lister.channelList)
					if (CONTENT instanceof Channel) {
						System.out.println(count++ + " - Channel no. " + ((Channel) CONTENT).getNumber() + ": " + CONTENT.getName()
								+ ", " + CONTENT.getTime());
					} else {
						System.out.println(count++ + " - " + CONTENT.getName() + ", dwell=" + CONTENT.getTime() + " msec");
					}
				System.out.println("Default dwell time: " + helper.lister.getDwellTime() + " milliseconds");
				System.exit(0);
			}
		});
		JScrollPane scroller = new JScrollPane(helper.lister, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		if (!SHOW_IN_WINDOW)
			scroller.getVerticalScrollBar().setPreferredSize(new Dimension(40, 0));
		JPanel retval = new JPanel(new BorderLayout());
		;
		retval.add(scroller, BorderLayout.CENTER);
		retval.add(helper.accept, BorderLayout.NORTH);
		return retval;
	}

	/**
	 * @return A play list of channels
	 */
	public SignageContent getChannelList() {
		return new ChannelPlayList(channelList, getDwellTime());
	}

	/**
	 * @param channelList
	 */
	public void setChannelList(final List<SignageContent> channelList) {
		this.channelList = channelList;
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
		// ChannelCatalogFactory.useRealChannels(true);
		IS_PUBLIC_CONTROLLER = false;
		SHOW_IN_WINDOW = true;

		JFrame f = new JFrame(CreateListOfChannels.class.getCanonicalName() + " Testing;");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		f.setContentPane(getContainer());
		f.setSize(500, 900);
		f.setVisible(true);
	}
}
