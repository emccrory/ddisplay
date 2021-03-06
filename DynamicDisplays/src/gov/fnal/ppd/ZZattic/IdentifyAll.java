/*
 * IdentifyAll
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.ZZattic;

import static gov.fnal.ppd.dd.GlobalVariables.SELF_IDENTIFY;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.nonguiUtils.CredentialsNotFoundException;

/**
 * Let the user ask each Display in the Dynamic Display system to go to the "Identify" screen. Since this Channel is designed to
 * only be on the screen for a fraction of a minute, the Display will go back to its "regularly scheduled program" after a while.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class IdentifyAll implements ActionListener {

	private static float		fontSize;
	private static Insets		inset			= null;
	private static String		title;

	private List<Display>		displays;

	private Channel				identifyChannel	= null;

	private static IdentifyAll	me;

	private static class PlainURLChannel extends ChannelImpl {
		private static final long serialVersionUID = 1713902164277236377L;

		/**
		 * @param theURL
		 *            The URL that is this Channel
		 * @throws Exception
		 *             In the case that the URL passed here is not a valid URI
		 */
		public PlainURLChannel(final URL theURL) throws Exception {
			super(theURL.toString(), "SimpleURL of '" + theURL + "'", theURL.toURI(), 0, 0);
		}

	}

	/**
	 * Add all the displays to the
	 * 
	 * @param d
	 */
	public static void setListOfDisplays(List<Display> d) {
		me = new IdentifyAll(d);
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
			IdentifyAll.title = title;
		IdentifyAll.fontSize = fontSize;
		IdentifyAll.inset = inset;
	}

	/**
	 * @return the button that launches the "identify all" procedure
	 */
	public static JButton getButton() {
		if (me == null)
			me = new IdentifyAll(null);

		JButton button = new JButton(title);
		button.setToolTipText("<html><b>Identify All Displays</b> -- Cause each Display "
				+ "<br>to show a special, self-identify splash screen</html>");
		button.setActionCommand(IdentifyAll.class.getCanonicalName());
		if (inset != null) {
			button.setFont(button.getFont().deriveFont(fontSize));
			button.setMargin(inset);
		}
		button.addActionListener(me);
		return button;
	}

	/**
	 * 
	 */
	private IdentifyAll(List<Display> d) {
		// Set up a different connection to each display within this class, doubling the number of connections to the messaging
		// server
		if (d == null)
			displays = DisplayListFactory.getInstance(getLocationCode());
		else {
			displays = new ArrayList<Display>();
			displays.addAll(d);
		}

		try {
			identifyChannel = new PlainURLChannel(new URL(SELF_IDENTIFY));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		for (Display D : displays)
			D.addListener(this);
	}

	@Override
	public void actionPerformed(final ActionEvent ev) {
		if (ev.getActionCommand().equals(IdentifyAll.class.getCanonicalName()))
			for (Display D : displays) {
				System.out.println("Set '" + D + "' to '" + identifyChannel + "'");
				D.setContent(identifyChannel);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		// else {
		// System.out.println("Event received: '" + ev + "'");
		// }
	}

	/**
	 * Create the "Identify All" GUI to allow the "Identify All" directive to all the local displays
	 * 
	 * @param args
	 *            No args are expected
	 */
	public static void main(final String[] args) {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		gov.fnal.ppd.dd.GetMessagingServer.getMessagingServerNameSelector();

		title = "Identify all at location=" + getLocationCode();

		JFrame f = new JFrame(IdentifyAll.class.getSimpleName());
		IdentifyAll.setup(null, 30.0f, new Insets(20, 50, 20, 50));
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Box b = Box.createVerticalBox();
		b.add(new JLabel("Pressing this button will send the 'Identify All' directive to all the displays"));
		b.add(Box.createRigidArea(new Dimension(10, 10)));
		b.add(IdentifyAll.getButton());
		b.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		f.setContentPane(b);
		f.pack();
		f.setVisible(true);
	}
}
