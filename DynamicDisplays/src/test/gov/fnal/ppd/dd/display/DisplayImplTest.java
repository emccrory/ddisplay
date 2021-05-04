package test.gov.fnal.ppd.dd.display;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

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
 * Simple unit tests for the Display class.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class DisplayImplTest {

	private final int		dbDisplay	= 10;
	private final int		vDisplay	= 1;
	private final int		screenNo	= 0;
	private final String	myName		= "localhost";
	private final String	myLocation	= "a location";
	private final Color		myColor		= Color.BLACK;
	private static Channel	testingChan	= null;

	private class D extends DisplayImpl {

		boolean fullyValid = false;

		public D(String ipName, int vDisplay, int dbDisplay, int screenNumber, String location, Color color) {
			super(ipName, vDisplay, dbDisplay, screenNumber, location, color);

			fullyValid = true;
			try {
				@SuppressWarnings("unused")
				InetAddress t = InetAddress.getByName(ipName);
				fullyValid = false;
			} catch (UnknownHostException e) {
				System.err.println("Host not found: " + ipName);
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
	}

	public DisplayImplTest() {
		if (testingDisplay == null) {
			testingDisplay = new D(myName, vDisplay, dbDisplay, screenNo, myLocation, myColor);
			try {
				testingChan = new ChannelImpl("Testing", "Testing desc", new URI("https://www.fnal.gov"), 1, 200000L);
				testingDisplay.setContent(testingChan);
			} catch (Exception e) {
				e.printStackTrace();
				testingDisplay.fullyValid = false;
			}
		}
	}

	private static D testingDisplay = null;

	@Test
	public void testSetContent() {
		assumeTrue(testingDisplay.fullyValid);
		try {
			Channel c = new ChannelImpl("TestingAgain", "Testing Again Desc", new URI("https://dynamicdisplays.fnal.gov"), 314,
					23456789L);
			testingDisplay.setContent(c);
			assertEquals(testingDisplay.getContent(), c);
			return;
		} catch (Exception e) {
			e.printStackTrace();
			assert (false);
		}
	}

	@Test
	public void testChannelStack() {
		// Test that the process of showing the self-identify screen brings us back to the original content
		assumeTrue(testingDisplay.fullyValid);
		try {

			Channel c1 = new ChannelImpl("Testing_01", "Testing One Desc", new URI("https://dynamicdisplays.fnal.gov/one"), 314,
					23456789L);
			Channel c2 = new ChannelImpl("Testing_02", "Testing Two Desc", new URI("https://dynamicdisplays.fnal.gov/two"), 314,
					23456789L);
			
			Stack<SignageContent> stack = testingDisplay.getStack();
			assertNotNull(stack);
			assert(stack.size() == 0);
			SignageContent s1 = testingDisplay.setContent(c1);
			assert(s1.equals(c1));
			SignageContent s2 = testingDisplay.setContent(c2);
			assert(s2.equals(c2));
			stack = testingDisplay.getStack();
			assertNotNull(stack);
			assert(stack.size() == 2);
			
		} catch (Exception e) {
			e.printStackTrace();
			assert (false);
		}
	}

	@Test
	public void testGetDBDisplayNumber() {
		assumeTrue(testingDisplay.fullyValid);
		assertEquals(dbDisplay, testingDisplay.getDBDisplayNumber());
	}

	@Test
	public void testGetVirtualDisplayNumber() {
		assumeTrue(testingDisplay.fullyValid);
		assertEquals(vDisplay, testingDisplay.getVirtualDisplayNumber());
	}

	@Test
	public void testSetDBDisplayNumber() {
		assumeTrue(testingDisplay.fullyValid);
		int nextDBNum = dbDisplay + 1;
		testingDisplay.setDBDisplayNumber(nextDBNum);
		assertEquals(nextDBNum, dbDisplay + 1);
		testingDisplay.setDBDisplayNumber(dbDisplay);
	}

	@Test
	public void testSetVirtualDisplayNumber() {
		assumeTrue(testingDisplay.fullyValid);
		int nextDBNum = vDisplay + 1;
		testingDisplay.setVirtualDisplayNumber(nextDBNum);
		assertEquals(nextDBNum, vDisplay + 1);
		testingDisplay.setVirtualDisplayNumber(vDisplay);
	}

	@Test
	public void testGetPreferredHighlightColor() {
		assumeTrue(testingDisplay.fullyValid);
		assertEquals(myColor, testingDisplay.getPreferredHighlightColor());
	}

	@Test
	public void testAddListener() {
		assumeTrue(testingDisplay.fullyValid);

		try {
			testingDisplay.addListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
				}
			});
			assert (true);
			return;
		} catch (Exception e) {
			e.printStackTrace();
			assert (false);
		}
	}

	@Test
	public void testErrorHandler() {
		assumeTrue(testingDisplay.fullyValid);
	}

	@Test
	public void testGetContent() {
		assumeTrue(testingDisplay.fullyValid);
		assertEquals(testingChan, testingDisplay.getContent());
	}

	@Test
	public void testGetScreenNumber() {
		assumeTrue(testingDisplay.fullyValid);
		assertEquals(screenNo, testingDisplay.getScreenNumber());
	}

	@Test
	public void testGetIPAddress() {
		assumeTrue(testingDisplay.fullyValid);
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
		assumeTrue(testingDisplay.fullyValid);
		assertEquals(myLocation, testingDisplay.getLocation());
	}

	@Test
	public void testGetStatus() {
		assumeTrue(testingDisplay.fullyValid);
		assertEquals(testingChan.toString(), testingDisplay.getStatus());
	}

}
