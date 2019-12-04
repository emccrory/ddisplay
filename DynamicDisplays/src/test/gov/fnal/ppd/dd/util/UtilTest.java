package test.gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.getClassName;
import static gov.fnal.ppd.dd.util.Util.getNextEntropy;
import static gov.fnal.ppd.dd.util.Util.getOrdinalSuffix;
import static gov.fnal.ppd.dd.util.Util.truncate;
import static gov.fnal.ppd.dd.util.Util.twoDigits;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.junit.Test;

public class UtilTest {

	@Test
	public void testTruncateStringInt() {
		String testString = "Something";
		String truncated = "Some  \u2026 ";
		assert (testString.equals(truncate(testString, testString.length())));
		assert (truncated.equals(truncate(testString, 7)));
	}

	@Test
	public void testTruncateString() {
		String testString = "123456789112345678921234567893";
		assert (testString.equals(truncate(testString)));
		String appendedTestString = testString + "non";
		assert ((testString + " \u2026 ").equals(truncate(appendedTestString)));
	}

	@Test
	public void testTwoDigits() {
		assert ("01".equals(twoDigits(1)));
		assert ("09".equals(twoDigits(9)));
		assert ("10".equals(twoDigits(10)));
		assert ("99".equals(twoDigits(99)));
	}

	@Test
	public void testCatchSleep() {
		long timeThen = System.currentTimeMillis();
		catchSleep(100);
		long timeNow = System.currentTimeMillis();
		long duration = timeNow - timeThen;
		assert (duration >= 99 && duration <= 101);
	}

	@Test
	public void testGetClassName() {
		assert (getClass().getSimpleName().equals(getClassName(getClass())));
		Object o = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Does nothing
			}

		};
		assert (ActionListener.class.getSimpleName().equals(getClassName(o.getClass())));
	}

	@Test
	public void testGetOrdinalSuffix() {
		assert ("st".equals(getOrdinalSuffix(1)));
		assert ("nd".equals(getOrdinalSuffix(2)));
		assert ("rd".equals(getOrdinalSuffix(3)));
		assert ("th".equals(getOrdinalSuffix(4)));
		assert ("th".equals(getOrdinalSuffix(5)));
		assert ("th".equals(getOrdinalSuffix(6)));
		assert ("th".equals(getOrdinalSuffix(7)));
		assert ("th".equals(getOrdinalSuffix(8)));
		assert ("th".equals(getOrdinalSuffix(9)));
		assert ("th".equals(getOrdinalSuffix(10)));
		assert ("st".equals(getOrdinalSuffix(21)));
		assert ("nd".equals(getOrdinalSuffix(22)));
	}

	@Test
	public void testGetNextEntropy() {
		long r1 = getNextEntropy();
		long r2 = getNextEntropy();
		long r3 = getNextEntropy();
		long r4 = getNextEntropy();
		// A pitiful test.  In reality, there is a chance that this test will fail.  But given the size of longs, it is quite unlikely
		assert (r1 != r2);
		assert (r1 != r3);
		assert (r1 != r4);
		assert (r2 != r3);
		assert (r2 != r4);
		assert (r3 != r4);
	}

}
