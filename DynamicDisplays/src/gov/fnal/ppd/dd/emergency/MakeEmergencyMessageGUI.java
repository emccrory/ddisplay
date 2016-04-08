package gov.fnal.ppd.dd.emergency;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getFullSelectorName;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.EmergencyCommunication;
import gov.fnal.ppd.dd.signage.SignageType;
import gov.fnal.ppd.dd.util.ObjectSigning;

import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class MakeEmergencyMessageGUI implements EmergencyMessageDistributor {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		MakeEmergencyMessageGUI em = new MakeEmergencyMessageGUI();

		if (!ObjectSigning.getInstance().isEmergMessAllowed(getFullSelectorName())) {
			JOptionPane.showMessageDialog(null, "You are not allowed to create emergency messages.");
			System.exit(-1);
		}

		em.initialize();

		EmergencyLaunchGUI elg = new EmergencyLaunchGUI(em);
		elg.initialize();

		JFrame f = new JFrame("Emergency Message Distribution");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(elg);
		f.pack();
		f.setVisible(true);
	}

	private List<Display>	displays;

	/**
	 * 
	 */
	public MakeEmergencyMessageGUI() {
		credentialsSetup();

		selectorSetup();
	}

	@Override
	public boolean send(EmergencyMessage em) {
		System.out.println(em);
		System.out.println("The HTML looks like this:\n" + em.toHTML());

		EmergencyCommunication ec = new EmergCommunicationImpl();
		ec.setMessage(em);
		for (Display D : displays) {
			D.setContent(ec);
		}
		return true;
	}

	private void initialize() {
		// Establish connection(s) to the messaging server(s)
		displays = DisplayListFactory.getInstance(SignageType.XOC, getLocationCode());
	}
}
