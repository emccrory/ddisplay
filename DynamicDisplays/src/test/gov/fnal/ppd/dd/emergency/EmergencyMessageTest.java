package test.gov.fnal.ppd.dd.emergency;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

import gov.fnal.ppd.dd.emergency.EmergencyMessage;
import gov.fnal.ppd.dd.emergency.Severity;

/**
 * Unit tests for the class, EmergencyMessage, that holds the emergency messaging text and related stuff.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class EmergencyMessageTest {

	private static final String		THE_HEADLINE	= "The Heading";
	private static final Severity	THE_SEVERITY	= Severity.TESTING;
	private static final long		THE_DWELL		= 12345678L;
	private static final String		THE_IP			= "My IP Address";
	private static final long		THE_TIMESTAMP	= System.currentTimeMillis();
	private static final String		THE_FOOTNOTE	= "The Footnote";
	private static final String		THE_MESSAGE		= "The long form message being conveyed";

	public EmergencyMessage			testingMessage	= new EmergencyMessage();

	public EmergencyMessageTest() {
		testingMessage.setHeadline(THE_HEADLINE);
		testingMessage.setSeverity(THE_SEVERITY);
		testingMessage.setDwellTime(THE_DWELL);
		testingMessage.setIpAddress(THE_IP);
		testingMessage.setTimestamp(THE_TIMESTAMP);
		testingMessage.setFootnote(THE_FOOTNOTE);
		testingMessage.setMessage(THE_MESSAGE);
	}

	// ---------------------------------------------------------------------------------------------------
	// Getters
	// ---------------------------------------------------------------------------------------------------

	@Test
	public void testGetFootnote() {
		assertEquals(THE_FOOTNOTE, testingMessage.getFootnote());
	}

	@Test
	public void testGetHeadline() {
		assertEquals(THE_HEADLINE, testingMessage.getHeadline());
	}

	@Test
	public void testGetMessage() {
		assertEquals(THE_MESSAGE, testingMessage.getMessage());
	}

	@Test
	public void testGetSeverity() {
		assertEquals(THE_SEVERITY, testingMessage.getSeverity());
	}

	@Test
	public void testGetIpAddress() {
		assertEquals(THE_IP, testingMessage.getIpAddress());
	}

	@Test
	public void testGetDwellTime() {
		assertEquals(THE_DWELL, testingMessage.getDwellTime());
	}

	@Test
	public void testGetTimestamp() {
		assertEquals(THE_TIMESTAMP, testingMessage.getTimestamp());
	}

	// ---------------------------------------------------------------------------------------------------
	// Setters - These setters are redundant to the way the EmergencyMessage object is created, above.
	// ---------------------------------------------------------------------------------------------------

	@Test
	public void testSetFootnote() {
		String newFootnote = THE_FOOTNOTE + new Date();
		testingMessage.setFootnote(newFootnote);
		assertEquals(newFootnote, testingMessage.getFootnote());
	}

	@Test
	public void testSetHeadline() {
		String newHeadline = THE_HEADLINE + new Date();
		testingMessage.setHeadline(newHeadline);
		assertEquals(newHeadline, testingMessage.getHeadline());
	}

	@Test
	public void testSetMessage() {
		String newMessage = THE_MESSAGE + new Date();
		testingMessage.setMessage(newMessage);
		assertEquals(newMessage, testingMessage.getMessage());
	}

	@Test
	public void testSetSeverity() {
		Severity sev = Severity.INFORMATION;
		testingMessage.setSeverity(sev);
		assertEquals(sev, testingMessage.getSeverity());
	}

	@Test
	public void testSetDwellTime() {
		long dt = 2L * THE_DWELL;
		testingMessage.setDwellTime(dt);
		assertEquals(dt, testingMessage.getDwellTime());
	}

	@Test
	public void testSetIpAddress() {
		String newIP = THE_IP + new Date();
		testingMessage.setIpAddress(newIP);
		assertEquals(newIP, testingMessage.getIpAddress());
	}

	@Test
	public void testSetTimestamp() {
		long newTimeStamp = System.currentTimeMillis();
		testingMessage.setTimestamp(newTimeStamp);
		assertEquals(newTimeStamp, testingMessage.getTimestamp());
	}

}
