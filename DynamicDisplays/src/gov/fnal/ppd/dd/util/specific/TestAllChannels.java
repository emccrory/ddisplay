package gov.fnal.ppd.dd.util.specific;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.changer.ChannelClassificationDictionary;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.nonguiUtils.CredentialsNotFoundException;
import gov.fnal.ppd.dd.util.nonguiUtils.VerifyURL;

/**
 * <p>
 * This is a stand-alone, maintenance application program for the Fermilab Dynamic Displays system.
 * </p>
 * </p>
 * Test all the channels in the system. Uses VerifyURL to test if that page can be read.
 * </p>
 * </p>
 * This should be run a couple of times per year to remove invalid channels from the system.
 * </p>
 */
public class TestAllChannels {

	public static void main(String[] args) {

		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		selectorSetup();

		int numTested = 0, numGood = 0;
		System.out.println(
				"\nThe channels that are not valid, from the categories that are relevant to this location, are listed here\n\n");
		ChannelClassification[] categories = ChannelClassificationDictionary.getCategories();
		List<Channel> badChans = new ArrayList<Channel>();
		for (ChannelClassification set : categories) {
			System.out.println("***** Testing all the channels in the category " + set);
			Set<SignageContent> list = ChannelCatalogFactory.getInstance().getChannelCatalog(set);
			for (SignageContent SC : list) {
				String u = SC.getURI().toString();
				if ("http://identify".equals(u) || "http://refresh".equals(u))
					continue;
				if ((numTested++) % 25 == 0)
					System.out.println("... testing " + numTested + " through " + (numTested + 24));
				if (!VerifyURL.isValid(u)) {
					// Falling here means that it could not open the URL
					Channel chan = (Channel) SC;
					badChans.add(chan);
					System.out.println("     " + chan.getNumber() + " is not valid, could not open the URL -- (" + chan.getName()
							+ ") [" + u + "]");
					// It looks like a web site that takes a long time to respond, like
					// https://earth.nullschool.net/#current/wind/surface/level/orthographic=-86.98,41.19,3000, can give a false
					// failure
				} else {
					numGood++;
				}
			}
		}
		System.out.println("\n----------\nTested " + numTested + " channels: " + numGood + " were good and " + badChans.size()
				+ " were bad.\n\nThe bad ones were:");
		for (Channel c : badChans)
			System.out.println("     " + c.getNumber() + " \t " + c.getName() + "\t[" + c.getURI().toASCIIString() + "]");
		System.exit(0);
	}
}
