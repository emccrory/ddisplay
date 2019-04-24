package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;

import java.util.Set;

import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.changer.ChannelClassificationDictionary;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

/**
 * Test all the channels in the system so we can remove invalid channels from the system
 */
public class TestAllChannels {

	public static void main(String[] args) {

		credentialsSetup();

		selectorSetup();

		int numTested = 0, numGood = 0, numBad = 0;
		System.out.println("The channels that are not valid are:");
		ChannelClassification[] categories = ChannelClassificationDictionary.getCategories();
		for (ChannelClassification set : categories) {
			System.out.println("***** Category " + set);
			Set<SignageContent> list = ChannelCatalogFactory.getInstance().getChannelCatalog(set);
			for (SignageContent SC : list) {
				String u = SC.getURI().toString();
				if ("http://identify".equals(u) || "http://refresh".equals(u))
					continue;
				if ((numTested++) % 25 == 0)
					System.out.println("... testing " + numTested);
				if (!URLTest.isValid(u)) {
					Channel chan = (Channel) SC;
					numBad++;
					System.out.println("     " + chan.getNumber() + " (" + chan.getName() + ") [" + u + "]");
				} else {
					numGood++;
				}
			}
		}
		System.out.println("Tested " + numTested + " channels: " + numGood + " were good and " + numBad + " were bad.");
		System.exit(0);
	}
}
