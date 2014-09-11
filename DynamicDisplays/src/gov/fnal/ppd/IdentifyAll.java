package gov.fnal.ppd;

import static gov.fnal.ppd.GlobalVariables.IS_PUBLIC_CONTROLLER;
import static gov.fnal.ppd.GlobalVariables.PROGRAM_NAME;
import static gov.fnal.ppd.GlobalVariables.SELF_IDENTIFY;
import static gov.fnal.ppd.GlobalVariables.locationCode;
import static gov.fnal.ppd.GlobalVariables.locationName;
import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.DisplayListFactory;
import gov.fnal.ppd.signage.channel.PlainURLChannel;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
/**
 * Let the user ask each Display in the Dynamic Display system to go to the "Identify" screen. Since this Channel is designed to
 * only be on the screen for a fraction of a minute, the Display will go back to its "regularly scheduled program" after a while.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class IdentifyAll extends JButton implements ActionListener {

	private static final long	serialVersionUID	= 389156534115421551L;

	private List<Display>		displays;

	private Channel				identifyChannel		= null;

	/**
	 * 
	 */
	public IdentifyAll() {
		super("Identify All Displays: " + locationName[locationCode]);
		PROGRAM_NAME = "Identify";
		
		final SignageType sType = (IS_PUBLIC_CONTROLLER ? SignageType.Public : SignageType.XOC);

		displays = DisplayListFactory.getInstance(sType, locationCode);

		try {
			identifyChannel = new PlainURLChannel(new URL(SELF_IDENTIFY));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		setFont(getFont().deriveFont(30.0f));
		setMargin(new Insets(20, 50, 20, 50));

		addActionListener(this);

		for (Display D : displays)
			D.addListener(this);
	}

	@Override
	public void actionPerformed(final ActionEvent ev) {
		if (ev.getSource() == this)
			for (Display D : displays) {
				System.out.println("Set '" + D + "' to '" + identifyChannel + "'");
				D.setContent(identifyChannel);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		else {
			System.out.println("Event received: '" + ev + "'");
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		JFrame f = new JFrame(IdentifyAll.class.getSimpleName());
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(new IdentifyAll());
		f.pack();
		f.setVisible(true);
	}
}
