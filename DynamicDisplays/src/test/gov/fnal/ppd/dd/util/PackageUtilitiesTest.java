package test.gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_BILLION;
import static gov.fnal.ppd.dd.util.specific.PackageUtilities.getChannelFromNumber;
import static gov.fnal.ppd.dd.util.specific.PackageUtilities.makeEmptyChannel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.signage.SignageContent;
import test.gov.fnal.ppd.dd.NeedsCredentials;

public class PackageUtilitiesTest extends NeedsCredentials {

	@Test
	public void testMakeEmptyChannel() {
		String testURL = "https://www.fnal.gov";
		SignageContent c = makeEmptyChannel(testURL);
		assertEquals(c.getClass(), ChannelImpl.class);
		assertEquals(c.getURI().toASCIIString(), testURL);
	}

	@Test
	public void testGetChannelFromSmallNumber() {
		assumeTrue(credentialsOK);

		// This test is fragile - it depends on the details of the database
		SignageContent plain = getChannelFromNumber(8); // This should be the DUNE slide show (June 2021)
		assertEquals(plain.getClass(), ChannelImpl.class);
		assertEquals(plain.getURI().toASCIIString(),
				"https://dynamicdisplays.fnal.gov/kenburns/portfolioDisplayChoice.php?exp=DUNE-LBNF&titletext=LBNF%2FDUNE");
	}

	@Test
	public void testGetChannelFromNegativeNumber() {
		assumeTrue(credentialsOK);
		SignageContent aList = getChannelFromNumber(-90); // This should be the default content for mu2e display (a list)
		assertEquals(aList.getClass(), ChannelPlayList.class);
		ChannelPlayList cpl = (ChannelPlayList) aList;
		assertEquals(cpl.getChannels().size(), 2);
	}

	@Test
	public void testGetChannelFromHugeNumber() {
		assumeTrue(credentialsOK);
		SignageContent aJpeg = getChannelFromNumber(ONE_BILLION + 65); // This should be an image from The Big Move of g-2
		assertEquals(aJpeg.getClass(), ChannelImpl.class);
		// Today (June 2021), the URL looks like this:
		// https://dynamicdisplays.fnal.gov/portfolioOneSlide.php?photo=upload%2Fitems%2F13-0211-10D.hr-r.jpg&caption=The+Big+Move");
		assertTrue(aJpeg.getURI().toASCIIString().contains("13-0211-10D.hr-r.jpg"));
		assertTrue(aJpeg.getURI().toASCIIString().contains("?photo"));
		assertTrue(aJpeg.getURI().toASCIIString().contains("&caption"));
	}
}
