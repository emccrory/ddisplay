package gov.fnal.ppd;

import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.DisplayListFactory;
import gov.fnal.ppd.signage.channel.PlainURLChannel;

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
 * @copy 2014
 * 
 */
public class IdentifyAll extends JButton implements ActionListener {

	private static final long	serialVersionUID	= 389156534115421551L;

	private List<Display>		displays			= DisplayListFactory
															.getInstance((Boolean.getBoolean("signage.selector.public") ? SignageType.Public
																	: SignageType.XOC));

	private Channel				identifyChannel		= null;

	/**
	 * 
	 */
	public IdentifyAll() {
		super("Identify All Displays");
		try {
			identifyChannel = new PlainURLChannel(new URL("http://identify"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		addActionListener(this);

		for (Display D : displays)
			D.addListener(this);
	}

	@Override
	public void actionPerformed(final ActionEvent ev) {
		if (ev.getSource() == this)
			for (Display D : displays)
				D.setContent(identifyChannel);
		else {
			System.out.println("Event received: '" + ev + "'");
		}
	}

	public static void main(final String[] args) {
		JFrame f = new JFrame(IdentifyAll.class.getSimpleName());
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(new IdentifyAll());
		f.pack();
		f.setVisible(true);
	}
}
