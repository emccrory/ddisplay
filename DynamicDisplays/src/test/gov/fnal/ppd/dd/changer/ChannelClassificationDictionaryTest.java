package test.gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.changer.ChannelClassificationDictionary.getCategories;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChannelClassificationDictionaryTest {

	static {
		credentialsSetup();
	}

	@Test
	public void testGetCategories() {
		assertTrue(getCategories().length > 1);
	}

}
