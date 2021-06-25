package test.gov.fnal.ppd.dd.version;

import static org.junit.Assert.*;

import org.junit.Test;

import gov.fnal.ppd.dd.util.version.VersionInformation;
import gov.fnal.ppd.dd.util.version.VersionInformation.FLAVOR;

/**
 * Unit tests for the VersionInformation class.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class VersionInformationTest {

	private static final long	THE_TIMESTAMP	= System.currentTimeMillis();
	private static final FLAVOR	THE_FLAVOR		= FLAVOR.DEVELOPMENT;
	private static final String	THE_DESCRIPTION	= "The description of this version";
	private static final int	A				= 1;
	private static final int	B				= 2;
	private static final int	C				= 3;
	private VersionInformation	testingVersion	= new VersionInformation();

	public VersionInformationTest() {
		testingVersion.setTimeStamp(THE_TIMESTAMP);
		testingVersion.setDisposition(THE_FLAVOR);
		testingVersion.setVersionDescription(THE_DESCRIPTION);
		testingVersion.setVersionVal(0, A);
		testingVersion.setVersionVal(1, B);
		testingVersion.setVersionVal(2, C);
	}

	@Test
	public void testGetTimeStamp() {
		assertEquals(THE_TIMESTAMP, testingVersion.getTimeStamp());
	}

	@Test
	public void testGetDisposition() {
		assertEquals(THE_FLAVOR, testingVersion.getDisposition());
	}

	@Test
	public void testGetVersionDescription() {
		assertEquals(THE_DESCRIPTION, testingVersion.getVersionDescription());
	}

	@Test
	public void testGetVersionString() {
		String versionString = A + "." + B + "." + C;
		assertEquals(versionString, testingVersion.getVersionString());
	}

	@Test
	public void testGetVersionVal() {
		assertEquals(A, testingVersion.getVersionVal(0));
		assertEquals(B, testingVersion.getVersionVal(1));
		assertEquals(C, testingVersion.getVersionVal(2));
	}

	@Test
	public void testEqualsObject() {
		VersionInformation secondVI = new VersionInformation();
		secondVI.setTimeStamp(THE_TIMESTAMP);
		secondVI.setDisposition(THE_FLAVOR);
		secondVI.setVersionDescription(THE_DESCRIPTION);
		secondVI.setVersionVal(0, A);
		secondVI.setVersionVal(1, B);
		secondVI.setVersionVal(2, C);
		assertEquals(secondVI, testingVersion);
	}

}
