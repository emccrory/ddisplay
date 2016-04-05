package gov.fnal.ppd.dd.emergency;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.GlobalVariables.getLocationCode;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import static gov.fnal.ppd.dd.emergency.EmergencyLaunchGUI.SHOW_MORE_STUFF;
import gov.fnal.ppd.dd.changer.DisplayListFactory;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.EmergencyCommunication;
import gov.fnal.ppd.dd.signage.SignageType;

import java.util.List;

import javax.swing.JFrame;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class MakeEmergencyMessageGUI implements EmergencyMessageDistributor {

	public static void main(String[] args) {

		MakeEmergencyMessageGUI em = new MakeEmergencyMessageGUI();
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
