/*
 * IdentifyAll
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd;

import static gov.fnal.ppd.dd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.dd.GlobalVariables.PROGRAM_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.SELF_IDENTIFY;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationName;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.channel.PlainURLChannel;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageType;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;

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
	private static String		title			= "Identify All Displays: " + getLocationName();

	private List<Display>		displays;

	private Channel				identifyChannel	= null;

	private static IdentifyAll	me;

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
		PROGRAM_NAME = "Identify";

		final SignageType sType = (IS_PUBLIC_CONTROLLER ? SignageType.Public : SignageType.XOC);

		if (d == null)
			displays = DisplayListFactory.getInstance(sType, getLocationCode());
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
	 * @param args
	 */
	public static void main(final String[] args) {
		JFrame f = new JFrame(IdentifyAll.class.getSimpleName());
		IdentifyAll.setup(null, 30.0f, new Insets(20, 50, 20, 50));
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(IdentifyAll.getButton());
		f.pack();
		f.setVisible(true);
	}
}
