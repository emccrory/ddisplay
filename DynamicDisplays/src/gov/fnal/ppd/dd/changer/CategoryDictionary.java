package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.*;

/**
 * Retrieve the categories that are to be used by the ChannelSelector GUI
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class CategoryDictionary {

	private CategoryDictionary() {
	}

	/**
	 * @return The array of Channel categories that are appropriate for this instance of the ChannelSelector GUI
	 */
	public static ChannelCategory[] getCategories() {
		ChannelCategory[] categories;

		// TODO Push this into the database (rather than hard-coding it here)

		switch (locationCode) {
		case 0: // ROC-West

			categories = new ChannelCategory[] { ChannelCategory.PUBLIC, ChannelCategory.PUBLIC_DETAILS,
					ChannelCategory.NOVA_DETAILS, ChannelCategory.NUMI_DETAILS, ChannelCategory.EXPERIMENT_DETAILS,
					ChannelCategory.ACCELERATOR, ChannelCategory.VIDEOS, ChannelCategory.MISCELLANEOUS };

			if (IS_PUBLIC_CONTROLLER) {
				categories = new ChannelCategory[] { ChannelCategory.PUBLIC, ChannelCategory.PUBLIC_DETAILS,
						ChannelCategory.VIDEOS, ChannelCategory.MISCELLANEOUS };
			}
			break;

		case 1:// ROC-East
			categories = new ChannelCategory[] { new ChannelCategory("CMS"), new ChannelCategory("LHC"),
					ChannelCategory.MISCELLANEOUS };
			break;

		case 2:
			// Test regime in Elliott's office
		case 3:
			// WH2E
		default:
			categories = new ChannelCategory[] { ChannelCategory.PUBLIC, ChannelCategory.PUBLIC_DETAILS,
					ChannelCategory.NOVA_DETAILS, ChannelCategory.NUMI_DETAILS, ChannelCategory.EXPERIMENT_DETAILS,
					ChannelCategory.ACCELERATOR, ChannelCategory.VIDEOS, ChannelCategory.MISCELLANEOUS, new ChannelCategory("CMS"),
					new ChannelCategory("LHC") };
		}

		return categories;
	}

	private static final String[]	FermilabExperiments	= { "gMinus2", "LBNF", "MicroBooNE", "MiniBooNE", "MINERvA", "MINOS",
			"Mu2E", "NOvA", "SeaQuest", "NUmI"			};

	private static final String[]	CERNExperiments		= { "CMS", "ATLAS", "LHC", "AEGIS", "ALICE", "ALPHA", "AMS", "ASACUSA",
			"ATRAP", "AWAKE", "BASE", "CAST", "CLOUD", "CMS", "ACE", "AEGIS", "ALICE", "ALPHA", "AMS", "ASACUSA", "ATRAP", "AWAKE",
			"BASE", "CAST", "CLOUD", "COMPASS", "DIRAC", "ISOLDE", "LHCb", "LHCf", "MOEDAL", "NA61/SHINE", "NA62", "nTOF", "OSQAR",
			"TOTEM", "UA9"								};

	/**
	 * @param exp
	 *            The experiment to check
	 * @return Is this experiment relevant to the GUI at the designated locationCode?
	 */
	public static boolean isExperiment(final String exp) {
		if (locationCode == 0) {
			// ROC-West
			for (String EXP : FermilabExperiments)
				if (exp.equalsIgnoreCase(EXP))
					return true;

		} else if (locationCode == 1) {
			// ROC-East
			for (String EXP : CERNExperiments)
				if (exp.equalsIgnoreCase(EXP))
					return true;

		} else if (locationCode == 2) {
			// Test regime in Elliott's office
			return true;
		} else if (locationCode == 3) {
			// WH2
			return true;
		} else if (locationCode == -1) {
			// Everything
			return true;
		} else
			throw new RuntimeException("Unknown locationCode, " + locationCode);
		return false;
	}
}
