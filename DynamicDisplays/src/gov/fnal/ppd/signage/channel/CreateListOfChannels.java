package gov.fnal.ppd.signage.channel;

import static gov.fnal.ppd.GlobalVariables.SHOW_IN_WINDOW;
import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.changer.ChannelCatalogFactory;
import gov.fnal.ppd.signage.changer.ChannelCategory;

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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;

/**
 * Let the user create a list of channels for a display to play. This is used in CreateListOfChannelsHelper as a GUI element that is
 * presented to ChannelSelector
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class CreateListOfChannels extends JPanel {

	private static final long		serialVersionUID	= 2157704848183269779L;
	private static final String		NOT_SELECTED		= "                  ";
	private Set<SignageContent>		publicChannels		= ChannelCatalogFactory.getInstance().getChannelCatalog(
																ChannelCategory.PUBLIC);
	private Set<SignageContent>		miscChannels		= ChannelCatalogFactory.getInstance().getChannelCatalog(
																ChannelCategory.MISCELLANEOUS);
	private Set<SignageContent>		details1Channels	= ChannelCatalogFactory.getInstance().getChannelCatalog(
																ChannelCategory.PUBLIC_DETAILS);
	private Set<SignageContent>		details2Channels	= ChannelCatalogFactory.getInstance().getChannelCatalog(
																ChannelCategory.EXPERIMENT_DETAILS);

	private Set<SignageContent>		novaChannels		= ChannelCatalogFactory.getInstance().getChannelCatalog(
																ChannelCategory.NOVA_DETAILS);
	private Set<SignageContent>		numiChannels		= ChannelCatalogFactory.getInstance().getChannelCatalog(
																ChannelCategory.NUMI_DETAILS);

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
		}
	}

	private static class BigLabel extends JLabel {
		private static final long	serialVersionUID	= 8296427549410976741L;

		public BigLabel(String title, int style) {
			super(title);
			if (!SHOW_IN_WINDOW)
				setFont(new Font("Sans Serif", style, 20));
		}
	}

	CreateListOfChannels() {
		super(new GridBagLayout());
		
		//
		// TODO Instead of a long vertical list of channels, organize them into seperate panels, with a titled border
		//
		
		GridBagConstraints bag = new GridBagConstraints();
		bag.fill = GridBagConstraints.HORIZONTAL;
		bag.gridx = bag.gridy = 1;
		bag.insets = new Insets(2, 2, 2, 2);
		bag.anchor = GridBagConstraints.CENTER;

		add(Box.createRigidArea(new Dimension(10, 10)), bag);
		bag.gridy++;

		Box bh = Box.createHorizontalBox();
		bh.add(new BigLabel("Dwell time (msec): ", Font.PLAIN));

		SpinnerModel model = new SpinnerListModel(getDwellStrings());
		time = new JSpinner(model);
		time.setValue(new Long(20000l));
		if (!SHOW_IN_WINDOW)
			time.setFont(new Font("Monospace", Font.PLAIN, 40));
		bh.add(time);

		bag.gridwidth = 2;
		add(bh, bag);
		bag.gridy++;

		add(new BigLabel("---------- Public Channels ----------", Font.BOLD), bag);
		bag.gridy++;

		bag.gridwidth = 1;
		for (final SignageContent CONTENT : publicChannels) {
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
					if (selected) {
						// TODO This does not work properly: All the labels must shift up when one is removed
						selected = false;
						channelList.remove(CONTENT);
						lab.setText(NOT_SELECTED);
						labelList.remove(lab);
					} else {
						selected = true;
						channelList.add(CONTENT);
						lab.setText("XX");
						labelList.add(lab);
					}
					fixLabels();
				}
			});
		}

		bag.gridwidth = 2;
		add(new JSeparator(), bag);
		bag.gridy++;
		add(new BigLabel("---------- Miscellaneous Channels ----------", Font.BOLD), bag);
		bag.gridwidth = 1;
		bag.gridy++;

		for (final SignageContent CONTENT : miscChannels) {
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
					if (selected) {
						// TODO This does not work properly: All the labels must shift up when one is removed
						selected = false;
						channelList.remove(CONTENT);
						lab.setText(NOT_SELECTED);
						labelList.remove(lab);
					} else {
						selected = true;
						channelList.add(CONTENT);
						lab.setText("XX");
						labelList.add(lab);
					}
					fixLabels();
				}
			});
		}

		bag.gridwidth = 2;
		add(new JSeparator(), bag);
		bag.gridy++;
		add(new BigLabel("---------- NOvA Channels ----------", Font.BOLD), bag);
		bag.gridy++;
		bag.gridwidth = 1;
		for (final SignageContent CONTENT : novaChannels) {
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
					if (selected) {
						// TODO This does not work properly: All the labels must shift up when one is removed
						selected = false;
						channelList.remove(CONTENT);
						lab.setText(NOT_SELECTED);
						labelList.remove(lab);
					} else {
						selected = true;
						channelList.add(CONTENT);
						lab.setText("XX");
						labelList.add(lab);
					}
					fixLabels();
				}
			});
		}

		bag.gridwidth = 2;
		add(new JSeparator(), bag);
		bag.gridy++;
		add(new BigLabel("---------- NuMI Channels ----------", Font.BOLD), bag);
		bag.gridy++;
		bag.gridwidth = 1;
		for (final SignageContent CONTENT : numiChannels) {
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
					if (selected) {
						// TODO This does not work properly: All the labels must shift up when one is removed
						selected = false;
						channelList.remove(CONTENT);
						lab.setText(NOT_SELECTED);
						labelList.remove(lab);
					} else {
						selected = true;
						channelList.add(CONTENT);
						lab.setText("XX");
						labelList.add(lab);
					}
					fixLabels();
				}
			});
		}

		bag.gridwidth = 2;
		add(new JSeparator(), bag);
		bag.gridy++;
		add(new BigLabel("---------- Details Channels ----------", Font.BOLD), bag);
		bag.gridy++;
		bag.gridwidth = 1;
		for (final SignageContent CONTENT : details1Channels) {
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
					if (selected) {
						// TODO This does not work properly: All the labels must shift up when one is removed
						selected = false;
						channelList.remove(CONTENT);
						lab.setText(NOT_SELECTED);
						labelList.remove(lab);
					} else {
						selected = true;
						channelList.add(CONTENT);
						lab.setText("XX");
						labelList.add(lab);
					}
					fixLabels();
				}
			});
		}
		for (final SignageContent CONTENT : details2Channels) {
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
					if (selected) {
						// TODO This does not work properly: All the labels must shift up when one is removed
						selected = false;
						channelList.remove(CONTENT);
						lab.setText(NOT_SELECTED);
						labelList.remove(lab);
					} else {
						selected = true;
						channelList.add(CONTENT);
						lab.setText("XX");
						labelList.add(lab);
					}
					fixLabels();
				}
			});
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
		retval.add(10000l);
		retval.add(12000l);
		retval.add(15000l);
		retval.add(20000l);
		retval.add(30000l);
		retval.add(45000l);
		retval.add(60000l);
		retval.add(75000l);
		retval.add(90000l);
		retval.add(100000l);
		retval.add(110000l);
		retval.add(120000l);
		retval.add(150000l);
		retval.add(180000l);
		retval.add(210000l);
		retval.add(240000l);
		retval.add(300000l);
		retval.add(360000l);
		retval.add(420000l);
		retval.add(480000l);
		retval.add(540000l);
		retval.add(600000l);
		retval.add(1200000l);
		retval.add(1800000l);
		retval.add(2400000l);
		retval.add(3600000l);

		return retval;
	}

	protected void fixLabels() {
		int count = 1;
		for (JLabel LAB : labelList) {
			if (!LAB.getText().equals(NOT_SELECTED))
				LAB.setText("** No. " + (count++) + " **");
		}
	}

	private static Container getContainer() {
		final CreateListOfChannelsHelper helper = new CreateListOfChannelsHelper();

		helper.accept.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int count = 1;
				for (SignageContent CONTENT : helper.lister.channelList)
					if (CONTENT instanceof Channel) {
						System.out.println(count++ + " - Channel no. " + ((Channel) CONTENT).getNumber() + ": " + CONTENT.getName());
					} else {
						System.out.println(count++ + " - " + CONTENT.getName());
					}
				System.out.println("Dwell time: " + helper.lister.getDwellTime() + " milliseconds");
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

		JFrame f = new JFrame(CreateListOfChannels.class.getCanonicalName() + " Testing;");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		f.setContentPane(getContainer());
		f.setSize(500, 900);
		f.setVisible(true);
	}
}
