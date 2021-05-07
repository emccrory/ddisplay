package test.gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.getFullURLPrefix;
import static gov.fnal.ppd.dd.util.nonguiUtils.VerifyURL.isValid;

import org.junit.Test;

public class VerifyURLTest {

	@Test
	public void testIsValid() {
		assert(isValid("https://www.fnal.gov"));
		assert(isValid("https://www.fnal.gov/?arg1=1&arg2=1"));
		assert(isValid(getFullURLPrefix()));
		assert(!isValid("Gobbledygook"));
	}
}
