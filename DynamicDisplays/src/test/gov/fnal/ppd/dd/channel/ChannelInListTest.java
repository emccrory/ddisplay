package test.gov.fnal.ppd.dd.channel;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;

import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.ChannelInListImpl;
import gov.fnal.ppd.dd.signage.Channel;

public class ChannelInListTest {

	@Test
	public void testChannelInListImplSignageContent() {
		try {
			ChannelImpl chan = new ChannelImpl("Name", "Description", new URI("https://google.com:"), 0, 5 * ONE_SECOND);
			ChannelInListImpl testCase = new ChannelInListImpl(chan);
			assertEquals(-1, testCase.getSequenceNumber());
		} catch (Exception e) {
			fail("Exception " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testChannelInListImplStringChannelClassificationStringURIIntLong() {
		try {
			ChannelInListImpl testCase = new ChannelInListImpl("Name", ChannelClassification.MISCELLANEOUS, "Description",
					new URI("https://google.com:"), 4, 5 * ONE_SECOND);
			assertEquals(-1, testCase.getSequenceNumber());
		} catch (Exception e) {
			fail("Exception " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testChannelInListImplChannelIntLong() {
		try {
			ChannelImpl chan = new ChannelImpl("Name", "Description", new URI("https://google.com:"), 0, 5 * ONE_SECOND);
			ChannelInListImpl testCase = new ChannelInListImpl(chan, 4, 5 * ONE_SECOND);
			assertEquals(4, testCase.getSequenceNumber());
		} catch (Exception e) {
			fail("Exception " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testSetSequenceNumber() {
		try {
			ChannelImpl chan = new ChannelImpl("Name", "Description", new URI("https://google.com:"), 0, 5 * ONE_SECOND);
			ChannelInListImpl testCase = new ChannelInListImpl(chan, 4, 5 * ONE_SECOND);
			assertEquals(4, testCase.getSequenceNumber());
			testCase.setSequenceNumber(6);
			assertEquals(6, testCase.getSequenceNumber());
		} catch (Exception e) {
			fail("Exception " + e.getLocalizedMessage());
		}
	}

}
