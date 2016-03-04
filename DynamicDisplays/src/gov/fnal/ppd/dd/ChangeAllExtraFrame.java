/*
 * ChangeAllExtraFrame
 *
 * Copyright (c) 2013-16 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.PROGRAM_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationName;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageType;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Tell each "Special Announcement" frame to show the special announcement.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChangeAllExtraFrame implements ActionListener {

	/*
	 * TODO -- Maybe the right thing to do is to ask the user what to say and then send this HTML file to the Displays. Actually,
	 * asking the user what to say and then pushing that to the web server is the appropriate thing to do. Making this work in
	 * general might be hard (protections/rights). Make a PHP receiver for the information gathered from the user, which creates a
	 * new HTML file on the server that the Displays can read.
	 */
	private static float				fontSize;
	private static Insets				inset	= null;
	private static String				title;

	private List<Display>				displays;

	private Channel						announcementChannel;
	private static JComponent			buttons	= null;

	private static ChangeAllExtraFrame	me;

	/**
	 * Add all the displays to the
	 * 
	 * @param d
	 */
	public static void setListOfDisplays(List<Display> d) {
		me = new ChangeAllExtraFrame(d);
	}

	/**
	 * Setup the size of the button you want. Don't call this and you'll get a default button size
	 * 
	 * @param title
	 * @param fontSize
	 * @param inset
	 */
	public static void setup(final String title, final float fontSize, final Insets inset) {
		if (title != null)
			ChangeAllExtraFrame.title = title;
		ChangeAllExtraFrame.fontSize = fontSize;
		ChangeAllExtraFrame.inset = inset;
	}

	/**
	 * @return the button that launches the "launch special message" procedure
	 */
	public static JComponent getButtons() {
		if (me == null)
			me = new ChangeAllExtraFrame(null);

		if (buttons == null) {
			buttons = Box.createVerticalBox();

			final JTextField dwellTimeField = new JTextField();
			dwellTimeField.setFont(dwellTimeField.getFont().deriveFont(18.0f));

			// Button one: Send out the announcement command

			JButton button = new JButton(title);
			button.setToolTipText("<html><b>Send directive to show a special channel to all Displays</b> -- Cause each Display "
					+ "<br>to show a special message in the upper-right corner of the screen</html>");
			button.setActionCommand(ChangeAllExtraFrame.class.getCanonicalName());
			if (inset != null) {
				button.setFont(button.getFont().deriveFont(fontSize));
				button.setMargin(inset);
			}
			button.addActionListener(me);
			button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent ev) {
					try {
						long dwellTime = 1000L * Long.parseLong(dwellTimeField.getText());
						me.announcementChannel.setTime(dwellTime);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			button.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			buttons.add(button);

			// Button two: remove the announcement command

			button = new JButton("<html>Turn off special announcement</html>");
			button.setToolTipText("That special announcement frame you just made?  " + "This will turn it off on all the displays");
			button.setActionCommand(ChangeAllExtraFrame.class.getCanonicalName());
			if (inset != null) {
				button.setFont(button.getFont().deriveFont(fontSize));
				button.setMargin(inset);
			}
			button.addActionListener(me);
			button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					me.announcementChannel.setTime(10);
				}
			});
			button.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			buttons.add(button);

			// Entry: How long to keep this announcement up?

			Box hb = Box.createHorizontalBox();
			hb.setAlignmentX(JComponent.LEFT_ALIGNMENT);


			JLabel lab = new JLabel("How long to show the announcement (seconds): ");
			lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			lab.setFont(lab.getFont().deriveFont(14.0f));
			hb.add(lab);
			dwellTimeField.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					try {
						long dwellTime = 1000L * Long.parseLong(dwellTimeField.getText());
						me.announcementChannel.setTime(dwellTime);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			dwellTimeField.setText((me.announcementChannel.getTime()/1000L) + "");
			dwellTimeField.setAlignmentX(JComponent.LEFT_ALIGNMENT);

			hb.add(Box.createRigidArea(new Dimension(10, 10)));
			hb.add(dwellTimeField);

			buttons.add(hb);
		}
		return buttons;
	}

	/**
	 * 
	 */
	private ChangeAllExtraFrame(List<Display> d) {
		try {
			announcementChannel = new ChannelImpl("Messsage", ChannelCategory.PUBLIC, "Important message", new URI(
					getFullURLPrefix() + "/announce.html"), 0, 300000L);
			announcementChannel.setFrameNumber(1);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}

		PROGRAM_NAME = "Message";

		final SignageType sType = SignageType.XOC;

		if (d == null)
			displays = DisplayListFactory.getInstance(sType, getLocationCode());
		else {
			displays = new ArrayList<Display>();
			displays.addAll(d);
		}

		for (Display D : displays)
			D.addListener(this);
	}

	@Override
	public void actionPerformed(final ActionEvent ev) {
		if (ev.getActionCommand().equals(ChangeAllExtraFrame.class.getCanonicalName()))
			for (Display D : displays) {
				System.out.println("Set '" + D + "' to '" + announcementChannel + "'");
				D.setContent(announcementChannel);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		else if (ev.getActionCommand().equals("TurnOff")) {
			// TODO -- implement a way to turn off the extra frame
		} else {
			System.out.println("Event received: '" + ev + "'");
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameSelector();
		credentialsSetup();

		selectorSetup();

		title = "<html>Show special announcment at<br>location=" + getLocationCode() + " (<em>" + getLocationName() + "</em>)</html>";

		JFrame f = new JFrame(ChangeAllExtraFrame.class.getSimpleName());
		ChangeAllExtraFrame.setup(null, 20.0f, new Insets(20, 50, 20, 50));
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(ChangeAllExtraFrame.getButtons());
		f.pack();
		f.setVisible(true);
	}
}
