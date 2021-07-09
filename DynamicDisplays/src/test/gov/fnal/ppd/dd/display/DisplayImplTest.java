package test.gov.fnal.ppd.dd.display;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Stack;

import org.junit.Test;

import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.display.DisplayImpl;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

/**
 * Unit tests for the Display class.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class DisplayImplTest {

	private class LocalDisplayImpl extends DisplayImpl {

		boolean				fullyValid	= false;
		protected String	errorMessage;

		public LocalDisplayImpl(String ipName, int vDisplay, int dbDisplay, int screenNumber, String location, Color color) {
			super(ipName, vDisplay, dbDisplay, screenNumber, location, color);

			fullyValid = true;
			try {
				@SuppressWarnings("unused")
				InetAddress t = InetAddress.getByName(ipName);
			} catch (UnknownHostException e) {
				System.err.println("Host not found: " + ipName);
				fullyValid = false;
			}

		}

		public Stack<SignageContent> getStack() {
			return this.previousChannelStack;
		}

		@Override
		public String getMessagingName() {
			return "Testing";
		}

		@Override
		protected boolean localSetContent() {
			return true;
		}

		public int getListenersSize() {
			return listeners.size();
		}
	}

	private final int			dbDisplay		= 10;
	private final int			vDisplay		= 1;
	private final int			screenNo		= 0;
	private final String		myName			= "localhost";
	private final String		myLocation		= "a location";
	private final Color			myColor			= Color.BLACK;
	 Channel				testingChan		= null;
	private LocalDisplayImpl	testingDisplay	= new LocalDisplayImpl(myName, vDisplay, dbDisplay, screenNo, myLocation, myColor);

	public DisplayImplTest() {
		try {
			testingChan = new ChannelImpl("Testing", "Testing desc", new URI("https://www.fnal.gov"), 1, 200000L);
			testingDisplay.setContent(testingChan);
		} catch (Exception e) {
			e.printStackTrace();
			testingDisplay.fullyValid = false;
		}
	}
	
	@Test
	public void testSetContent() {
		assertTrue(testingDisplay.fullyValid);
		try {
			Channel c = new ChannelImpl("TestingAgain", "Testing Again Desc", new URI("https://dynamicdisplays.fnal.gov"), 314,
					23456789L);
			testingDisplay.setContent(c);
			assertEquals(testingDisplay.getContent(), c);
		} catch (Exception e) {
			fail("Exception caught: " + e.getClass().getSimpleName() + " -- " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testChannelStack() {
		// Test that the process of showing the self-identify screen brings us back to the original content
		assertTrue(testingDisplay.fullyValid);
		try {

			Channel c1 = new ChannelImpl("Testing_01", "Testing One Desc", new URI("https://dynamicdisplays.fnal.gov/one"), 314,
					23456789L);
			Channel c2 = new ChannelImpl("Testing_02", "Testing Two Desc", new URI("https://dynamicdisplays.fnal.gov/two"), 314,
					23456789L);

			Stack<SignageContent> stack = testingDisplay.getStack();
			assertNotNull(stack);
			assertEquals(2, stack.size());
			SignageContent s1 = testingDisplay.setContent(c1);
			assertEquals(testingChan, s1);
			SignageContent s2 = testingDisplay.setContent(c2);
			assertEquals(c1, s2);
			stack = testingDisplay.getStack();
			assertNotNull(stack);
			assertEquals(4, stack.size());

		} catch (Exception e) {
			fail("Exception caught: " + e.getClass().getSimpleName() + " -- " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testGetDBDisplayNumber() {
		assertTrue(testingDisplay.fullyValid);
		assertEquals(dbDisplay, testingDisplay.getDBDisplayNumber());
	}

	@Test
	public void testGetVirtualDisplayNumber() {
		assertTrue(testingDisplay.fullyValid);
		assertEquals(vDisplay, testingDisplay.getVirtualDisplayNumber());
	}

	@Test
	public void testSetDBDisplayNumber() {
		assertTrue(testingDisplay.fullyValid);
		int nextDBNum = dbDisplay + 1;
		testingDisplay.setDBDisplayNumber(nextDBNum);
		assertEquals(nextDBNum, dbDisplay + 1);
		testingDisplay.setDBDisplayNumber(dbDisplay);
	}

	@Test
	public void testSetVirtualDisplayNumber() {
		assertTrue(testingDisplay.fullyValid);
		int nextDBNum = vDisplay + 1;
		testingDisplay.setVirtualDisplayNumber(nextDBNum);
		assertEquals(nextDBNum, vDisplay + 1);
		testingDisplay.setVirtualDisplayNumber(vDisplay);
	}

	@Test
	public void testGetPreferredHighlightColor() {
		assertTrue(testingDisplay.fullyValid);
		assertEquals(myColor, testingDisplay.getPreferredHighlightColor());
	}

	@Test
	public void testAddListener() {
		assertTrue(testingDisplay.fullyValid);

		try {
			testingDisplay.addListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
				}
			});
			assertTrue(testingDisplay.getListenersSize() > 0);
		} catch (Exception e) {
			fail("Exception caught: " + e.getClass().getSimpleName() + " -- " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testErrorHandler() {
		assertTrue(testingDisplay.fullyValid);

		try {
			testingDisplay.addListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("Listener called with " + e.getSource());
					testingDisplay.errorMessage = e.getSource().toString();
				}
			});
			assertTrue(testingDisplay.getListenersSize() > 0);
			String message = "Error message " + Math.random();
			testingDisplay.errorHandler(message);
			catchSleep(100);
			assertEquals("Display 10", testingDisplay.errorMessage);
		} catch (Exception e) {
			fail("Exception caught: " + e.getClass().getSimpleName() + " -- " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testGetContent() {
		assertTrue(testingDisplay.fullyValid);
		assertEquals(testingChan, testingDisplay.getContent());
	}

	@Test
	public void testGetScreenNumber() {
		assertTrue(testingDisplay.fullyValid);
		assertEquals(screenNo, testingDisplay.getScreenNumber());
	}

	@Test
	public void testGetIPAddress() {
		assertTrue(testingDisplay.fullyValid);
		try {
			@SuppressWarnings("unused")
			InetAddress inet = testingDisplay.getIPAddress();
			assert (true);
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
		assert (false);
	}

	@Test
	public void testGetLocation() {
		assertTrue(testingDisplay.fullyValid);
		assertEquals(myLocation, testingDisplay.getLocation());
	}

	@Test
	public void testGetStatus() {
		assertTrue(testingDisplay.fullyValid);
		assertEquals(testingChan.toString(), testingDisplay.getStatus());
	}

}
