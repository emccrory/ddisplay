package test.gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.util.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.GeneralUtilities.getClassName;
import static gov.fnal.ppd.dd.util.GeneralUtilities.getNextEntropy;
import static gov.fnal.ppd.dd.util.GeneralUtilities.getOrdinalSuffix;
import static gov.fnal.ppd.dd.util.GeneralUtilities.truncate;
import static gov.fnal.ppd.dd.util.GeneralUtilities.twoDigits;
import static org.junit.Assert.assertEquals;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.junit.Test;

public class UtilTest {

	@Test
	public void testTruncateStringInt() {
		String testString = "Something";
		String truncated = "Some \u2026 ";
		assertEquals(testString, truncate(testString, testString.length()));
		assertEquals(truncated, truncate(testString, 7));
	}

	@Test
	public void testTruncateString() {
		String testString = "123456789112345678921234567893";
		assertEquals(testString, truncate(testString));
		String appendedTestString = testString + "non";
		assertEquals(("123456789112345678921234567 \u2026 "), truncate(appendedTestString));
	}

	@Test
	public void testTwoDigits() {
		assertEquals("01", twoDigits(1));
		assertEquals("09", twoDigits(9));
		assertEquals("10", twoDigits(10));
		assertEquals("99", twoDigits(99));
	}

	@Test
	public void testCatchSleep() {
		long timeThen = System.currentTimeMillis();
		catchSleep(100);
		long timeNow = System.currentTimeMillis();
		long duration = timeNow - timeThen;
		assert(duration >= 99);
		assert(duration <= 110); // Wow - in the context of running these tests in Maven, duration is 105!
	}

	@Test
	public void testGetClassName() {
		assertEquals(getClass().getSimpleName(), getClassName(getClass()));
		ActionListener o = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Does nothing
			}

		};
		assertEquals("(Object in UtilTest)", getClassName(o.getClass()));
	}

	@Test
	public void testGetOrdinalSuffix() {
		assertEquals("st", getOrdinalSuffix(1));
		assertEquals("nd", getOrdinalSuffix(2));
		assertEquals("rd", getOrdinalSuffix(3));
		assertEquals("th", getOrdinalSuffix(4));
		assertEquals("th", getOrdinalSuffix(5));
		assertEquals("th", getOrdinalSuffix(6));
		assertEquals("th", getOrdinalSuffix(7));
		assertEquals("th", getOrdinalSuffix(8));
		assertEquals("th", getOrdinalSuffix(9));
		assertEquals("th", getOrdinalSuffix(10));
		assertEquals("st", getOrdinalSuffix(21));
		assertEquals("nd", getOrdinalSuffix(22));
	}

	@Test
	public void testGetNextEntropy() {
		long r1 = getNextEntropy();
		long r2 = getNextEntropy();
		long r3 = getNextEntropy();
		long r4 = getNextEntropy();
		// A pitiful test. In reality, there is a chance that this test will fail. But given the size of longs, it is quite unlikely
		assert (r1 != r2);
		assert (r1 != r3);
		assert (r1 != r4);
		assert (r2 != r3);
		assert (r2 != r4);
		assert (r3 != r4);
	}

}
