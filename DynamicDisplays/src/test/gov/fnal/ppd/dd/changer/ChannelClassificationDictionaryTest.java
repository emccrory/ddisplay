package test.gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.changer.ChannelClassificationDictionary.getCategories;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import test.gov.fnal.ppd.dd.NeedsCredentials;

public class ChannelClassificationDictionaryTest extends NeedsCredentials {

	@Test
	public void testGetCategories() {
		if (!credentialsOK)
			return;
		assertTrue(getCategories().length > 1);
	}

}
