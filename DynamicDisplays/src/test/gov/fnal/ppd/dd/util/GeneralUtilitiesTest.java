package test.gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.bytesToString;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.getClassName;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.getNextEntropy;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.getOrdinalSuffix;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.truncate;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.twoDigits;
import static org.junit.Assert.assertEquals;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.junit.Test;

/**
 * Tests of the class GeneralUtilities
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class GeneralUtilitiesTest {

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
	public void testBytesToString() {
		String s = "Very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_string";
		byte[] b = s.getBytes();
		String r = bytesToString(b, true);
		String expected = "56657279 5f766572 795f7665 72795f76 6572795f 76657279 5f766572 795f7665 72795f76 6572795f 76657279 5f766572 795f7665 72795f76 6572795f 76657279 5f766572 795f7665 72795f6c 6f6e675f 73747269 6e67";
		System.out.println(expected.equals(r));
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
		assert (duration >= 99);
		assert (duration <= 110); // Wow - in the context of running these tests in Maven, duration is 105!
	}

	@Test
	public void testGetClassName() {
		assertEquals(getClass().getSimpleName(), getClassName(getClass()));
		// Create an anonymous class name here as this is used elsewhere. So if Java changes the way anonymous classes are named,
		// this will fail.
		ActionListener o = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Does nothing
			}

		};
		assertEquals("(Object in GeneralUtilitiesTest)", getClassName(o.getClass()));
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
		long r5 = getNextEntropy();
		
		// A pitiful test. There really is not a good way to test for randomness on a small set of numbers, though.
		// Furthermore, there is a chance that this test will fail. But given the size of longs, (-9,223,372,036,854,775,808 to
		// +9,223,372,036,854,775,807 = 18 quintrillion possible numbers) it is quite unlikely
		
		// According to https://en.wikipedia.org/wiki/Prime_number_theorem, the number of primes in 2^63 values is about 1 per ln(2^63),
		// which is about every 44'th number.  So that's still about 4 trillion primes.  (A prime is used as a seed to start this).
		
		assert (r1 != r2);
		assert (r1 != r3);
		assert (r1 != r4);
		assert (r2 != r3);
		assert (r2 != r4);
		assert (r3 != r4);
		
		assert (r5 != r1);
		assert (r5 != r2);
		assert (r5 != r3);
		assert (r5 != r4);
	
	}

}
