package gov.fnal.ppd.dd.util.specific;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;

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
import gov.fnal.ppd.security.TrustAllSSL;

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
 * <p>
 * Checking of SSL certificates is BYPASSED for this main! See gov.fnal.ppd.security.TrustAllSSL
 * </p>
 */
public class TestAllChannels {

	static {
		/*
		 * There are a few URLs that the Java JVM says don't do SSL right. This seems to be a problem only with the JVM's
		 * understanding of "trust." This error does not happen in real browsers (Firefox, Chrome).
		 */

		/*
		 * This class will cause the JVM to trust ALL certificates. It is not advisable to do this routinely! The URLs that failed
		 * before this was put here were all from https://uscms.org. These work fine from a regular browser - no SSL issues
		 * reported. I bet this is because the cache of certificate things is different from a real browser when you use the JVM to
		 * fetch the URL.
		 */
		new TrustAllSSL();
	}

	private static class ChanWhy {
		public Channel	c;
		public String	why;

		public ChanWhy(Channel a, String s) {
			c = a;
			why = s;
		}
	}

	private static int numTested = 0;

	public static void main(String[] args) {

		/*
		 * With 306 channels, this takes about 31 seconds. In order to make this a viable Jenkins test, this thread will force it to
		 * stop after (an arbitrarily selected) 120 seconds.
		 */

		new Thread("DoNotLingerTooLong") {
			public void run() {
				long waitSeconds = 120L;
				try {
					sleep(waitSeconds * 1000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.err.println("This is taking too long - aborting after " + waitSeconds + " seconds and testing " + numTested
						+ " channels.");
				System.exit(-1);
			}
		}.start();

		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		boolean verbose = true;
		if (args.length > 0 && args[0].equalsIgnoreCase("silent"))
			verbose = false;

		// selectorSetup();

		int numGood = 0;
		int checkAt = 25;
		if (verbose)
			System.out.println(
					"\nThe channels that are not valid, from the categories that are relevant to this location, are listed here\n\n");
		ChannelClassification[] categories = ChannelClassificationDictionary.getCategoriesDatabaseAllChannels();
		List<ChanWhy> badChans = new ArrayList<ChanWhy>();
		for (ChannelClassification set : categories) {

			Set<SignageContent> list = ChannelCatalogFactory.getInstance().getChannelCatalog(set);
			if (verbose)
				System.out.println("***** Testing all " + list.size() + " channels in the category " + set);
			for (SignageContent SC : list) {
				String u = SC.getURI().toString();
				if ("http://identify".equals(u) || "http://refresh".equals(u))
					continue;

				if ((numTested++) % checkAt == 0)
					if (verbose)
						System.out.println("... testing " + numTested + " through " + (numTested + checkAt - 1));
				if (!VerifyURL.isValid(u)) {
					// Falling here means that it could not open the URL
					Channel chan = (Channel) SC;
					badChans.add(new ChanWhy(chan, VerifyURL.getWhyFailed()));
					if (verbose)
						System.out.println("     " + chan.getNumber() + " is not valid, could not open the URL -- ("
								+ chan.getName() + ") [" + u + "] - why=" + VerifyURL.getWhyFailed());
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
		System.out.println("   ChanNum     | ChannelName                | Why Failed?             | The URL");
		System.out.println("---------------+----------------------------+-------------------------+-----------------");
		for (ChanWhy cw : badChans)
			System.out.println("     " + cw.c.getNumber() + " \t " + getSpaces(cw.c.getName(), 28) + getSpaces(cw.why, 22) + "\t"
					+ cw.c.getURI().toASCIIString());

		// Always exit with success code, even if there are bad channels.
		System.exit(0);
	}

	private static String getSpaces(String name, int minimum) {
		if (name == null)
			name = "null";
		String spaces = " ";
		for (int i = minimum - name.length(); i > 0; i--)
			spaces += " ";
		return name + spaces;
	}
}
