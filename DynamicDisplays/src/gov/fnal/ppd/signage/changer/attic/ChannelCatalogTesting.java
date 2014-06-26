package gov.fnal.ppd.signage.changer.attic;

import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.changer.ChannelCatalog;
import gov.fnal.ppd.signage.changer.ChannelCategory;
import gov.fnal.ppd.signage.channel.ChannelImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Holds the definitions of some Channels in the XOC signage system so that a channel can be selected in the touch panel. This was a
 * prototype that was used before the database was established.
 * 
 * @author Elliott McCrory, Fermilab AD, 2012
 * @deprecated
 */
public class ChannelCatalogTesting extends HashMap<String, SignageContent> implements ChannelCatalog {

	private static final long	serialVersionUID	= -787877522875087421L;

	private static final char	NU					= 0x3bd;

	/**
	 * A convenience structure for holding the names and descriptions of the channels here.
	 * 
	 * @author Elliott McCrory, Fermilab AD, 2012
	 */
	private static class ChannelDesc {
		public String	n, d;

		public ChannelDesc(String n, String d) {
			this.n = n;
			this.d = d;
		}
	}

	/**
	 * 
	 */
	public static final ChannelDesc[]						Public			= {
			new ChannelDesc("MINOS", "Public info on the Main Injector Neutrino Oscillation Something"),
			new ChannelDesc("MINER" + NU + "A", "a neutrino scattering experiment which uses the NuMI beamline"),
			new ChannelDesc("MiniBooNE", "test for neutrino mass by searching for neutrino oscillations"),
			new ChannelDesc(
					"MicroBooNE",
					"The experiment will build and operate a large 170 ton Liquid Argon Time Projection Chamber (LArTPC) located along the Booster neutrino beam line"),
			new ChannelDesc("Mu2E", "Search for muon-to-electron conversion"),
			new ChannelDesc("Muon g-2", "To measure the muon anomalous magnetic moment to 0.14 ppm"),
			new ChannelDesc("SeaQuest",
					"Use Drell-Yan process to measure the contributions of antiquarks to the structure of the proton or neutron"),
			new ChannelDesc("LNBE", "The Long Baseline Neutrino Experiment at Fermilab"),
			new ChannelDesc("NO" + NU + "A",
					"Will study the strange properties of neutrinos, especially the elusive transition of muon neutrinos into electron neutrinos"),
			new ChannelDesc("Accelerator", "Useful public information on the Fermilab Accelerator Chain"), };

	/**
	 * 
	 */
	public static final String[]							DetailedOne		= { "MINOS a", "MINOS b", "MINOS c", "MINOS d",
			"MINER" + NU + "A a", "MINER" + NU + "A b", "MINER" + NU + "A c", "MINER" + NU + "A d", "MiniBooNE a", "MiniBooNE b",
			"MiniBooNE c", "MiniBooNE d", "MicroBooNE a", "MicroBooNE b", "MicroBooNE c", "MicroBooNE d", "Mu2E a", "Mu2E b",
			"Mu2E c", "Mu2E d", "Muon g-2 a", "Muon g-2 b", "Muon g-2 c", "Muon g-2 d", "SeaQuest a", "SeaQuest b", "SeaQuest c",
			"SeaQuest d", "LNBE a", "LNBE b", "LNBE c", "LNBE d", "NO" + NU + "A a", "NO" + NU + "A b", "NO" + NU + "A c",
			"NO" + NU + "A d", "Accelerator a", "Accelerator b", "Accelerator c", "Accelerator d", };

	/**
	 * 
	 */
	public static final String[]							DetailedTwo		= { "MINOS XA", "Muon g-2 XB", "Mu2E XC", "MINOS XD",
			"Muon g-2 XE", "Mu2E XF", "MINOS XG", "Muon g-2 XH", "Mu2E XX", "MINOS XY", "Muon g-2 XZ", "Mu2E XA1", "MINOS XB1",
			"Muon g-2 XC1", "Mu2E XD1", "MINOS XE1", "Muon g-2 XF1", "Mu2E XG1", "MINOS XH1", "Muon g-2 XX1", "Mu2E XY1",
			"MINOS XZ1", "A NO" + NU + "A", "B NO" + NU + "A", "C NO" + NU + "A", "D NO" + NU + "A", "E NO" + NU + "A",
			"A MINER" + NU + "A", "B MINER" + NU + "A", "C MINER" + NU + "A", "D MINER" + NU + "A", "E MINER" + NU + "A",
			"A SeaQuest", "B SeaQuest", "C SeaQuest", "D SeaQuest", "E SeaQuest", "A MiniBooNE",
			"B MiniBooNE a d f another onethingshshshshshshhshsgsgsgs", "C MiniBooNE", "D MiniBooNE", "E MiniBooNE",
			"A Accelerator", "B Accelerator", "C Accelerator", "D Accelerator", "E Accelerator", "A LBNE", "B LBNE", "C LBNE",
			"D LBNE", "E LBNE",											};

	/**
	 * 
	 */
	public static final ChannelDesc[]						MetaChannels	= {
			new ChannelDesc("List of Channels", "List of all the channels defined in the XOC Signage Display System"),
			new ChannelDesc("List of Displays", "List of all the Displays that are controlled by the XOC Signage Display Sisytem"),
			new ChannelDesc("Shift Schedule", "The present shift schedule in the XOC"),
			new ChannelDesc("Flights", "Flight information from O'hare and Midway airports"),
			new ChannelDesc("Weather", "Current weather conditions at Fermilab and all the far sites"),
			new ChannelDesc("Football", "World Cup coverage, or Sunday NFL"), };

	private SignageContent									defaultChannel;

	private static final Comparator<? super SignageContent>	comparator		= new Comparator<SignageContent>() {

																				public int compare(SignageContent o1,
																						SignageContent o2) {
																					return o1.getName().compareTo(o2.getName());
																				}

																			};

	ChannelCatalogTesting() {
		try {
			int chanNum = 1;

			for (ChannelDesc c : Public) {
				URI uri = new URI("http://www.fnal.gov/signage/" + chanNum++ + ".html");
				put(c.n, new ChannelImpl(c.n, ChannelCategory.PUBLIC, c.d, uri, chanNum++));
			}
			for (String c : DetailedOne) {
				URI uri = new URI("http://www.fnal.gov/signage/" + chanNum++ + ".html");
				put(c, new ChannelImpl(c, ChannelCategory.PUBLIC_DETAILS, c, uri, chanNum++));
			}
			for (String c : DetailedTwo) {
				URI uri = new URI("http://www.fnal.gov/signage/" + chanNum++ + ".html");
				put(c, new ChannelImpl(c, ChannelCategory.EXPERIMENT_DETAILS, c, uri, chanNum++));
			}
			for (ChannelDesc c : MetaChannels) {
				URI uri = new URI("http://www.fnal.gov/signage/" + chanNum++ + ".html");
				put(c.n, new ChannelImpl(c.n, ChannelCategory.MISCELLANEOUS, c.d, uri, chanNum++));
			}
			defaultChannel = get(MetaChannels[0].n);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.fnal.ppd.xoc.signage.changer.ChannelCatalog#getPublicChannels()
	 */
	@Override
	public Map<String, SignageContent> getPublicChannels() {
		HashMap<String, SignageContent> retval = new HashMap<String, SignageContent>();

		for (String key : this.keySet()) {
			if (this.get(key).getCategory() == ChannelCategory.PUBLIC)
				retval.put(key, this.get(key));
		}
		return retval;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.fnal.ppd.xoc.signage.changer.ChannelCatalog#getDetailsChannels()
	 */
	@Override
	public Map<String, SignageContent> getDetailsChannels() {
		HashMap<String, SignageContent> retval = new HashMap<String, SignageContent>();

		for (String key : this.keySet()) {
			if (this.get(key).getCategory() == ChannelCategory.PUBLIC_DETAILS)
				retval.put(key, this.get(key));
		}
		return retval;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.fnal.ppd.xoc.signage.changer.ChannelCatalog#getChannelCatalog(gov.fnal.ppd.xoc.signage.changer .ChannelCategory)
	 */
	@Override
	public Set<SignageContent> getChannelCatalog(ChannelCategory cat) {
		TreeSet<SignageContent> retval = new TreeSet<SignageContent>(comparator);

		for (String key : this.keySet()) {
			if (this.get(key).getCategory() == cat)
				retval.add(this.get(key));
		}
		return retval;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.fnal.ppd.xoc.signage.changer.ChannelCatalog#getAllDetailsChannels()
	 */
	@Override
	public Map<String, SignageContent> getAllDetailsChannels() {
		HashMap<String, SignageContent> retval = new HashMap<String, SignageContent>();

		for (String key : this.keySet()) {
			if (this.get(key).getCategory() == ChannelCategory.PUBLIC_DETAILS
					|| this.get(key).getCategory() == ChannelCategory.EXPERIMENT_DETAILS)
				retval.put(key, this.get(key));
		}
		return retval;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.fnal.ppd.xoc.signage.changer.ChannelList#getDefaultChannel()
	 */
	@Override
	public SignageContent getDefaultChannel() {
		return defaultChannel;
	}
}
