package test.gov.fnal.ppd.dd.emergency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Test;

import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.emergency.EmergCommunicationImpl;
import gov.fnal.ppd.dd.emergency.EmergencyMessage;

/**
 * Unit tests for the class gov.fnal.ppd.emergency.EmergCommunicationImpl
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class EmergCommunicationImplTest {

	private EmergCommunicationImpl	testObject	= new EmergCommunicationImpl();
	private EmergencyMessage		message;
	private String					name		= "Energency Communication " + new Date();
	private String					desc		= "Emergency Communication message " + new Date();
	private ChannelClassification	category	= ChannelClassification.MISCELLANEOUS;
	private long					expire		= 0L;
	private int						code		= ((int) System.currentTimeMillis()) + 31415926;
	private long					time		= System.currentTimeMillis();

	public EmergCommunicationImplTest() {
		message = new EmergencyMessage();

		testObject.setChannelClassification(category);
		testObject.setCode(code);
		testObject.setDescription(desc);
		testObject.setExpiration(expire);
		testObject.setName(name);
		testObject.setMessage(message);
		testObject.setTime(time);
	}

	@Test
	public void testGetName() {
		assertEquals(name, testObject.getName());
	}

	@Test
	public void testGetDescription() {
		assertEquals(desc, testObject.getDescription());
	}

	@Test
	public void testGetChannelClassification() {
		assertEquals(category, testObject.getChannelClassification());
	}

	@Test
	public void testGetURI() {
		assertNotNull(testObject.getURI());
	}

	@Test
	public void testGetTime() {
		assertEquals(time, testObject.getTime());
	}

	@Test
	public void testGetExpiration() {
		assertEquals(expire, testObject.getExpiration());
	}

	@Test
	public void testGetCode() {
		assertEquals(code, testObject.getCode());
	}

	@Test
	public void testSetMessage() {
		assertEquals(code, testObject.getCode());
	}

	@Test
	public void testGetMessage() {
		assertEquals(message, testObject.getMessage());
	}
}
