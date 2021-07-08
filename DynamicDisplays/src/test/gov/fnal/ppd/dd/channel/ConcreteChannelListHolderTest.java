package test.gov.fnal.ppd.dd.channel;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelInListImpl;
import gov.fnal.ppd.dd.channel.ConcreteChannelListHolder;

public class ConcreteChannelListHolderTest {

	@Test
	public void testConcreteChannelListHolder() {
		ConcreteChannelListHolder testCase = new ConcreteChannelListHolder();
		assertTrue(testCase.getName().startsWith("Content created at "));
	}

	@Test
	public void testConcreteChannelListHolderStringListOfChannelInList() {
		try {
			List<ChannelInList> cil = new ArrayList<ChannelInList>();

			ChannelImpl chan = new ChannelImpl("Name", "Description", new URI("https://google.com:"), 0, 5 * ONE_SECOND);
			ChannelInListImpl listImpl = new ChannelInListImpl(chan, 1, 5 * ONE_SECOND);

			ConcreteChannelListHolder testCase = new ConcreteChannelListHolder("contentName", cil);

			testCase.channelAdd(listImpl);
			chan = new ChannelImpl("Name", "Description", new URI("https://google.com:"), 0, 5 * ONE_SECOND);
			listImpl = new ChannelInListImpl(chan, 2, 6 * ONE_SECOND);
			testCase.channelAdd(listImpl);

			assertEquals(2, testCase.getList().size());

		} catch (Exception e) {
			fail("Exception " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testClear() {
		try {
			List<ChannelInList> cil = new ArrayList<ChannelInList>();

			ChannelImpl chan = new ChannelImpl("Name", "Description", new URI("https://google.com:"), 0, 5 * ONE_SECOND);
			ChannelInListImpl listImpl = new ChannelInListImpl(chan, 1, 5 * ONE_SECOND);

			ConcreteChannelListHolder testCase = new ConcreteChannelListHolder("contentName", cil);

			testCase.channelAdd(listImpl);
			chan = new ChannelImpl("Name", "Description", new URI("https://google.com:"), 0, 5 * ONE_SECOND);
			listImpl = new ChannelInListImpl(chan, 2, 6 * ONE_SECOND);
			testCase.channelAdd(listImpl);
			testCase.clear();
			assertEquals(0, testCase.getList().size());

		} catch (Exception e) {
			fail("Exception " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testChannelAdd() {
		try {
			List<ChannelInList> cil = new ArrayList<ChannelInList>();

			ChannelImpl chan = new ChannelImpl("Name", "Description", new URI("https://google.com:"), 0, 5 * ONE_SECOND);
			ChannelInListImpl listImpl = new ChannelInListImpl(chan, 1, 5 * ONE_SECOND);

			ConcreteChannelListHolder testCase = new ConcreteChannelListHolder("contentName", cil);
			assertEquals(0, testCase.getList().size());

			testCase.channelAdd(listImpl);
			assertEquals(1, testCase.getList().size());

			chan = new ChannelImpl("Name", "Description", new URI("https://google.com:"), 0, 5 * ONE_SECOND);
			listImpl = new ChannelInListImpl(chan, 2, 6 * ONE_SECOND);
			testCase.channelAdd(listImpl);
			assertEquals(2, testCase.getList().size());

		} catch (Exception e) {
			fail("Exception " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testGetName() {
		final String name = "ContenName" + Math.random();
		try {
			List<ChannelInList> cil = new ArrayList<ChannelInList>();

			ChannelImpl chan = new ChannelImpl("ChanName", "Description", new URI("https://google.com:"), 0, 5 * ONE_SECOND);
			ChannelInListImpl listImpl = new ChannelInListImpl(chan, 1, 5 * ONE_SECOND);

			ConcreteChannelListHolder testCase = new ConcreteChannelListHolder(name, cil);

			testCase.channelAdd(listImpl);
			
			assertEquals(name, testCase.getName());

		} catch (Exception e) {
			fail("Exception " + e.getLocalizedMessage());
		}
	}

}
