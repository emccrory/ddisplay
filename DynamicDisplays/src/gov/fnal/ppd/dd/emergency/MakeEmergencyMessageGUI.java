package gov.fnal.ppd.dd.emergency;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getFullSelectorName;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;

import gov.fnal.ppd.dd.CredentialsNotFoundException;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.EmergencyCommunication;
import gov.fnal.ppd.dd.util.ObjectSigning;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class MakeEmergencyMessageGUI implements EmergencyMessageDistributor {

	private static EmergencyLaunchGUI elg = null;

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		MakeEmergencyMessageGUI em = new MakeEmergencyMessageGUI();

		if (!ObjectSigning.getInstance().isEmergMessAllowed(getFullSelectorName())) {
			JOptionPane.showMessageDialog(null,
					"Your PC, \"" + getFullSelectorName() + "\", is not allowed to create emergency messages.");
			System.exit(-1);
		}

		em.initialize();

		elg = new EmergencyLaunchGUI(em);
		elg.initialize();

		JFrame f = new JFrame("Fermilab Emergency Message Distribution App");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(elg);
		f.pack();
		f.setVisible(true);
	}

	/**
	 * 
	 */
	public MakeEmergencyMessageGUI() {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		selectorSetup();
	}

	@Override
	public boolean send(EmergencyMessage em) {
		System.out.println(em);
		System.out.println("The HTML looks like this:\n" + em.toHTML());

		EmergencyCommunication ec = new EmergCommunicationImpl();
		ec.setMessage(em);
		for (Display D : elg.getTargetedDisplays()) {
			D.setContent(ec);
		}
		return true;
	}

	private void initialize() {
		// Establish connection(s) to the messaging server(s)
		// displays = DisplayListFactory.getInstance(SignageType.XOC, getLocationCode());
	}
}
